import io
import ipaddress
import socket
from urllib.parse import urlparse
from typing import Tuple
import httpx
from PIL import Image
from fastapi import HTTPException
from app.config import settings

def is_private_or_local_host(hostname: str) -> bool:
    """
    Check if a hostname resolves to a loopback, link-local, or private IP address.
    Provides server-side request forgery (SSRF) defense.
    """
    lowered = hostname.lower()
    if lowered in {"localhost", "127.0.0.1", "0.0.0.0", "::1"}:
        return True

    try:
        ip = ipaddress.ip_address(hostname)
        return ip.is_private or ip.is_loopback or ip.is_link_local
    except ValueError:
        # Not an IP string, attempt DNS resolution
        try:
            resolved_ip_str = socket.gethostbyname(hostname)
            resolved_ip = ipaddress.ip_address(resolved_ip_str)
            return resolved_ip.is_private or resolved_ip.is_loopback or resolved_ip.is_link_local
        except Exception:
            # If DNS fails or unresolved, allow httpx to fail downstream or block
            return False

async def fetch_and_validate_image(url: str, client: httpx.AsyncClient = None) -> Tuple[Image.Image, int, str]:
    """
    Downloads an image securely over HTTP/HTTPS, enforcing:
    1. Scheme check (http / https only)
    2. SSRF check (rejects private/local IPs)
    3. Max file size check (10MB)
    4. Content-Type inspection
    5. Pillow image validation (verifies corrupted or non-image payloads)
    """
    parsed = urlparse(url)
    if parsed.scheme not in {"http", "https"}:
        raise HTTPException(
            status_code=400,
            detail="Unsupported URL scheme. Only HTTP and HTTPS are permitted."
        )

    if not parsed.netloc:
        raise HTTPException(status_code=400, detail="Invalid URL format.")

    hostname = parsed.hostname or ""
    if is_private_or_local_host(hostname):
        raise HTTPException(
            status_code=400,
            detail="Security restriction: Access to local, internal, or loopback network addresses is prohibited."
        )

    close_client = False
    if client is None:
        client = httpx.AsyncClient(
            timeout=settings.DOWNLOAD_TIMEOUT_SECONDS,
            follow_redirects=True
        )
        close_client = True

    try:
        response = await client.get(url)
        if response.status_code != 200:
            raise HTTPException(
                status_code=400,
                detail=f"Failed to fetch image from remote host. Status: {response.status_code}"
            )

        content_type = response.headers.get("content-type", "").split(";")[0].strip().lower()
        if content_type and content_type not in settings.ALLOWED_CONTENT_TYPES:
            raise HTTPException(
                status_code=400,
                detail=f"Unsupported image format: {content_type}. Allowed: JPEG, PNG, WEBP."
            )

        content = response.content
        size = len(content)

        if size > settings.MAX_IMAGE_SIZE_BYTES:
            raise HTTPException(
                status_code=400,
                detail=f"Image size ({size} bytes) exceeds the maximum limit of {settings.MAX_IMAGE_SIZE_MB}MB."
            )

        if size == 0:
            raise HTTPException(status_code=400, detail="Empty response received from image URL.")

        # Pillow verification
        try:
            # 1. Verify byte stream integrity
            verify_img = Image.open(io.BytesIO(content))
            verify_img.verify()

            # 2. Re-open for actual processing and convert to RGB
            img = Image.open(io.BytesIO(content)).convert("RGB")
            return img, size, content_type or "image/jpeg"
        except Exception as e:
            raise HTTPException(
                status_code=400,
                detail=f"Invalid or corrupted image content: {str(e)}"
            )

    except httpx.TimeoutException:
        raise HTTPException(
            status_code=504,
            detail="Timeout fetching image from remote host."
        )
    except httpx.RequestError as e:
        raise HTTPException(
            status_code=400,
            detail=f"Network error downloading image: {str(e)}"
        )
    finally:
        if close_client:
            await client.aclose()
