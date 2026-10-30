import json
import queue
import unittest
import urllib.error
from types import SimpleNamespace
from unittest.mock import Mock, MagicMock
from unittest.mock import patch
from smart_bin_bridge import Backend, Config, PermanentHTTPError, ProtocolError, parse_line, put_latest, serial_reader


class BridgeTest(unittest.TestCase):
    def frame(self, **changes):
        return {"sequence": 1, "uptimeMs": 2000, "distanceCm": 15.9, "valid": True, "reason": "OK", **changes}

    def test_serial_contract(self):
        self.assertEqual(parse_line(json.dumps(self.frame()).encode())["distanceCm"], 15.9)
        self.assertFalse(parse_line(json.dumps(self.frame(distanceCm=None, valid=False, reason="NO_ECHO")))["valid"])

    def test_bad_lines(self):
        for raw in ("No echo", "{}", "x" * 513, json.dumps(self.frame(distanceCm=0)),
                    json.dumps(self.frame(distanceCm=0, valid=False)), json.dumps(self.frame(sequence=True)),
                    json.dumps(self.frame(distanceCm=float("nan")))):
            with self.subTest(raw=raw[:40]), self.assertRaises(ProtocolError):
                parse_line(raw)

    def test_queue_is_bounded_and_discards_oldest(self):
        outbox = queue.Queue(maxsize=2)
        put_latest(outbox, 1); put_latest(outbox, 2); put_latest(outbox, 3)
        self.assertEqual([outbox.get(), outbox.get()], [2, 3])

    @patch("smart_bin_bridge.time.sleep")
    @patch("smart_bin_bridge.urllib.request.build_opener")
    def test_retries_preserve_exact_wire_payload(self, opener, sleep):
        opener.return_value.open.side_effect = urllib.error.URLError("offline")
        client = Backend(Config("COM_TEST", 9600, "http://127.0.0.1:5000", "test-only-token"))
        payload = {**self.frame(), "sessionId": "stable-session", "capturedAtMs": 1000, "buffered": False}
        with self.assertRaises(ConnectionError):
            client.post("readings", payload)
        self.assertEqual(opener.return_value.open.call_count, 3)
        wires = [call.args[0].data for call in opener.return_value.open.call_args_list]
        self.assertEqual(wires[0], wires[1]); self.assertEqual(wires[1], wires[2])
        self.assertEqual(json.loads(wires[0])["sessionId"], "stable-session")

    @patch("smart_bin_bridge.urllib.request.build_opener")
    def test_unauthorized_is_not_retried(self, opener):
        opener.return_value.open.side_effect = urllib.error.HTTPError("unused", 401, "rejected", {}, None)
        with self.assertRaises(PermanentHTTPError):
            Backend(Config("COM_TEST", 9600, "http://127.0.0.1", "test-only-token")).post("readings", {})
        self.assertEqual(opener.return_value.open.call_count, 1)

    @patch.dict("os.environ", {"BIN_COM_PORT": "COM_TEST", "BIN_DEVICE_TOKEN": "test-only-token", "BIN_BACKEND_URL": "http://remote.example.invalid"}, clear=True)
    def test_nonlocal_plaintext_transport_rejected(self):
        with self.assertRaises(ValueError):
            Config.from_env()

    def test_serial_open_failure_reconnects_with_bounded_wait(self):
        serial_factory = Mock(side_effect=[OSError('disconnected'), MagicMock()])
        stop = Mock(); stop.is_set.return_value = False
        stop.wait.side_effect = [False, True]
        with patch.dict('sys.modules', {'serial': SimpleNamespace(Serial=serial_factory)}), patch('builtins.print'):
            serial_reader(Config('COM_TEST', 9600, 'http://127.0.0.1', 'test-only-token'), Mock(), queue.Queue(), stop)
        self.assertEqual(serial_factory.call_count, 2)
        self.assertEqual(stop.wait.call_args_list[0].args, (2,))

    def test_partial_serial_frame_survives_read_timeout(self):
        raw = (json.dumps(self.frame()) + '\n').encode('ascii')
        port = MagicMock()
        port.read_until.side_effect = [raw[:30], b'', raw[30:]]
        context = MagicMock(); context.__enter__.return_value = port
        done = [False]
        stop = Mock(); stop.wait.return_value = False; stop.is_set.side_effect = lambda: done[0]
        outbox = Mock(); outbox.put_nowait.side_effect = lambda item: done.__setitem__(0, True)
        backend = Mock(); backend.post.return_value = {'sessionId': '11111111-1111-4111-8111-111111111111', 'serverTimeMs': 1000000}
        with patch.dict('sys.modules', {'serial': SimpleNamespace(Serial=Mock(return_value=context))}), patch('builtins.print'):
            serial_reader(Config('COM_TEST', 9600, 'http://127.0.0.1', 'test-only-token'), backend, outbox, stop)
        self.assertEqual(outbox.put_nowait.call_count, 1)
        self.assertEqual(outbox.put_nowait.call_args.args[0][0]['sequence'], 1)
        port.reset_input_buffer.assert_called_once()


if __name__ == "__main__":
    unittest.main()
