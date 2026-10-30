"""Uno NDJSON -> authenticated REST. Only pyserial is third party.

No disk spool: bounded in-memory queue, drop-oldest on overflow. A reconnect
starts a new server session; readings already queued retain their old identity.
"""
import argparse
import json
import math
import os
import queue
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from dataclasses import dataclass


class ProtocolError(ValueError):
    pass


class PermanentHTTPError(RuntimeError):
    def __init__(self, status):
        self.status = status
        super().__init__(f"Backend rejected request (HTTP {status}).")


def parse_line(raw):
    if len(raw) > 512:
        raise ProtocolError("Serial line too long")
    try:
        data = json.loads(raw.decode("ascii") if isinstance(raw, bytes) else raw)
    except (ValueError, UnicodeError):
        raise ProtocolError("Malformed serial JSON") from None
    if not isinstance(data, dict) or set(data) != {"sequence", "uptimeMs", "distanceCm", "valid", "reason"}:
        raise ProtocolError("Unexpected serial fields")
    for key in ("sequence", "uptimeMs"):
        if type(data[key]) is not int or not 0 <= data[key] <= 4294967295:
            raise ProtocolError("Invalid sequence or uptime")
    if type(data["valid"]) is not bool or data["reason"] not in ("OK", "NO_ECHO", "OUT_OF_RANGE"):
        raise ProtocolError("Invalid validity flag")
    distance = data["distanceCm"]
    if data["valid"]:
        if type(distance) not in (float, int) or not math.isfinite(distance) or not 2 <= distance <= 400 or data["reason"] != "OK":
            raise ProtocolError("Invalid measured distance")
    elif distance is not None or data["reason"] == "OK":
        raise ProtocolError("Invalid readings must have null distance and an error reason")
    return data


@dataclass(frozen=True)
class Config:
    port: str
    baud: int
    backend_url: str
    token: str
    timeout: float = 4.0
    attempts: int = 3
    queue_size: int = 128

    @classmethod
    def from_env(cls):
        port = os.environ.get("BIN_COM_PORT", "")
        token = os.environ.get("BIN_DEVICE_TOKEN", "")
        url = os.environ.get("BIN_BACKEND_URL", "http://127.0.0.1:5000").rstrip("/")
        parsed = urllib.parse.urlparse(url)
        if not port or not token:
            raise ValueError("Set BIN_COM_PORT and BIN_DEVICE_TOKEN.")
        if parsed.username or parsed.password or parsed.query or parsed.fragment or parsed.path not in ("", "/"):
            raise ValueError("BIN_BACKEND_URL must be an origin without credentials or an API path.")
        if parsed.scheme != "https" and not (parsed.scheme == "http" and parsed.hostname in ("127.0.0.1", "localhost", "::1")):
            raise ValueError("Use HTTPS, or HTTP only on loopback for the laptop prototype.")
        baud = int(os.environ.get("BIN_BAUD", "9600"))
        if baud <= 0:
            raise ValueError("BIN_BAUD must be positive.")
        return cls(port, baud, url, token)


class Backend:
    def __init__(self, config):
        self.config = config

    def post(self, endpoint, payload):
        # Serialize once: every HTTP retry has precisely the same reading identity/content.
        wire = json.dumps(payload, separators=(",", ":"), allow_nan=False).encode("utf-8")
        request = urllib.request.Request(self.config.backend_url + "/api/device/" + endpoint,
            data=wire, headers={"Content-Type": "application/json", "Authorization": "Bearer " + self.config.token}, method="POST")
        for attempt in range(self.config.attempts):
            try:
                # No redirects: never forward a device credential to an unexpected origin.
                opener = urllib.request.build_opener(NoRedirect())
                with opener.open(request, timeout=self.config.timeout) as response:
                    body = json.loads(response.read(65536))
                    if not isinstance(body, dict) or not body.get("success") or not isinstance(body.get("data"), dict):
                        raise ValueError("Backend protocol error")
                    if endpoint == 'session':
                        uuid.UUID(body['data'].get('sessionId', ''))
                        if type(body['data'].get('serverTimeMs')) is not int or body['data']['serverTimeMs'] <= 0:
                            raise ValueError('Backend session clock missing')
                    return body["data"]
            except urllib.error.HTTPError as error:
                if error.code not in (408, 429) and error.code < 500:
                    raise PermanentHTTPError(error.code) from None
            except (urllib.error.URLError, TimeoutError, OSError, ValueError):
                pass
            if attempt + 1 < self.config.attempts:
                time.sleep(min(0.5 * 2 ** attempt, 2))
        raise ConnectionError("Backend unavailable after bounded retries")


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def put_latest(outbox, item):
    try:
        outbox.put_nowait(item)
    except queue.Full:
        try:
            outbox.get_nowait()
        except queue.Empty:
            pass
        outbox.put_nowait(item)


def serial_reader(config, backend, outbox, stop):
    import serial
    while not stop.is_set():
        try:
            with serial.Serial(config.port, config.baud, timeout=1) as port:
                # Opening the Uno typically resets it. Close Arduino Serial Monitor first.
                if stop.wait(2):
                    return
                session = backend.post("session", {})
                epoch_now = session["serverTimeMs"]
                session_mono = time.monotonic()
                port.reset_input_buffer()
                anchor_uptime = None
                anchor_mono = None
                last_sequence = last_uptime = -1
                print("Serial connected; new authenticated session active.", flush=True)
                last_line_at = time.monotonic()
                pending = b""
                while not stop.is_set():
                    chunk = port.read_until(b"\n", size=513 - len(pending))
                    if not chunk:
                        if time.monotonic() - last_line_at > 10:
                            raise ProtocolError("No serial measurements; reconnecting")
                        continue
                    last_line_at = time.monotonic()
                    pending += chunk
                    if len(pending) > 512:
                        pending = b""
                        port.reset_input_buffer()
                        print("Discarded oversized serial line.", flush=True)
                        continue
                    if not pending.endswith(b"\n"):
                        # A serial timeout may split a perfectly valid line. Keep it until newline.
                        continue
                    raw, pending = pending, b""
                    try:
                        frame = parse_line(raw)
                    except ProtocolError:
                        print("Discarded malformed serial line.", flush=True)
                        continue
                    if frame["sequence"] <= last_sequence or frame["uptimeMs"] <= last_uptime:
                        # Reboot, wrap, or duplicate serial frame: establish a new server session.
                        raise ProtocolError("Serial sequence reset; reconnecting")
                    received_mono = time.monotonic()
                    if anchor_uptime is None:
                        anchor_uptime, anchor_mono = frame["uptimeMs"], received_mono
                    captured_mono = anchor_mono + (frame["uptimeMs"] - anchor_uptime) / 1000
                    if captured_mono > received_mono + 1:
                        raise ProtocolError("Serial clock drift; reconnecting")
                    captured_at = int(epoch_now + 1000 * (captured_mono - session_mono))
                    last_sequence, last_uptime = frame["sequence"], frame["uptimeMs"]
                    payload = {**frame, "sessionId": session["sessionId"], "capturedAtMs": captured_at}
                    put_latest(outbox, (payload, captured_mono))
        except PermanentHTTPError as error:
            print(str(error) + " Check device provisioning/token; bridge stopped.", flush=True)
            stop.set()
        except (OSError, ProtocolError, ConnectionError):
            print("Serial/backend disconnected; retrying connection in 2 seconds.", flush=True)
            stop.wait(2)


def run(config):
    import serial  # Fail before starting threads if the dependency is missing.
    backend, stop = Backend(config), threading.Event()
    outbox = queue.Queue(maxsize=config.queue_size)
    reader = threading.Thread(target=serial_reader, args=(config, backend, outbox, stop), daemon=True)
    reader.start()
    try:
        while not stop.is_set():
            try:
                payload, captured_mono = outbox.get(timeout=1)
            except queue.Empty:
                continue
            payload["buffered"] = time.monotonic() - captured_mono > 3
            try:
                result = backend.post("readings", payload)
                print(f"Reading {payload['sequence']}: {result.get('reason', 'accepted')}", flush=True)
            except PermanentHTTPError as error:
                print(str(error) + " Reading dropped.", flush=True)
                if error.status in (401, 403):
                    stop.set()
            except ConnectionError:
                print("Reading delivery exhausted retries; reading dropped.", flush=True)
    except KeyboardInterrupt:
        print("Bridge stopped.", flush=True)
    finally:
        stop.set()
        reader.join(timeout=config.timeout * config.attempts + 5)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--list-ports", action="store_true")
    args = parser.parse_args()
    if args.list_ports:
        from serial.tools import list_ports
        for item in list_ports.comports():
            print(f"{item.device}: {item.description}")
        return
    try:
        run(Config.from_env())
    except (ValueError, ImportError) as error:
        # Error messages never include the config object, token, or HTTP response body.
        print("Bridge configuration/dependency error. Check environment and install requirements.")
        raise SystemExit(1) from None


if __name__ == "__main__":
    main()
