import io
import pytest
from unittest.mock import patch, AsyncMock
from fastapi.testclient import TestClient
from PIL import Image
import httpx

from app.main import app
from app.config import settings

client = TestClient(app)

def create_test_image_bytes(color=(100, 200, 100), size=(64, 64), format="JPEG") -> bytes:
    """Helper to generate in-memory image bytes without disk I/O."""
    buf = io.BytesIO()
    img = Image.new("RGB", size, color)
    img.save(buf, format=format)
    return buf.getvalue()

def test_health_endpoint():
    """1. Test health endpoint returns 200 and baseline model info."""
    response = client.get("/api/health")
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["status"] == "HEALTHY"
    assert data["modelStatus"] == "DEVELOPMENT_BASELINE"
    assert "version" in data

def test_valid_image_prediction():
    """2. Test valid image prediction returns structured prediction with baseline status."""
    valid_bytes = create_test_image_bytes(color=(40, 60, 220)) # Blue dominant

    mock_resp = httpx.Response(
        status_code=200,
        content=valid_bytes,
        headers={"content-type": "image/jpeg"},
        request=httpx.Request("GET", "https://res.cloudinary.com/demo/image/upload/sample.jpg")
    )

    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_resp
        response = client.post(
            "/api/predict/image",
            json={"imageUrl": "https://res.cloudinary.com/demo/image/upload/sample.jpg"}
        )

    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    data = body["data"]

    # 6. Response schema validation
    assert "classification" in data
    assert "severity" in data
    assert "modelStatus" in data
    assert "processedAt" in data

    # 7. Baseline model status validation
    assert data["modelStatus"] == "DEVELOPMENT_BASELINE"

    # 8. Category validation
    classification = data["classification"]
    assert classification["category"] in settings.CATEGORIES
    assert classification["confidence"] is None # Honest baseline

    # 9. Severity validation
    severity = data["severity"]
    assert severity["level"] in settings.SEVERITIES
    assert severity["confidence"] is None # Honest baseline

def test_invalid_url_scheme():
    """3. Test invalid URL schemes are rejected."""
    # FTP scheme
    response = client.post("/api/predict/image", json={"imageUrl": "ftp://example.com/image.jpg"})
    assert response.status_code == 422 # Pydantic URL validator or 400

    # Local file path
    response = client.post("/api/predict/image", json={"imageUrl": "file:///etc/passwd"})
    assert response.status_code == 422

def test_ssrf_private_ip_rejection():
    """4. Test SSRF protection blocks loopback/private IPs."""
    response = client.post("/api/predict/image", json={"imageUrl": "http://127.0.0.1:5000/secret.jpg"})
    assert response.status_code == 400
    assert "Security restriction" in response.json()["detail"]

    response2 = client.post("/api/predict/image", json={"imageUrl": "http://localhost:8080/image.jpg"})
    assert response2.status_code == 400
    assert "Security restriction" in response2.json()["detail"]

def test_unsupported_content_type():
    """5. Test rejection of non-image MIME types."""
    mock_resp = httpx.Response(
        status_code=200,
        content=b"<html>Not an image</html>",
        headers={"content-type": "text/html"},
        request=httpx.Request("GET", "https://example.com/file.html")
    )

    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_resp
        response = client.post(
            "/api/predict/image",
            json={"imageUrl": "https://example.com/file.html"}
        )

    assert response.status_code == 400
    assert "Unsupported image format" in response.json()["detail"]

def test_oversized_image_rejection():
    """6. Test rejection of oversized image payload exceeding limit."""
    oversized_bytes = b"X" * (settings.MAX_IMAGE_SIZE_BYTES + 1024)

    mock_resp = httpx.Response(
        status_code=200,
        content=oversized_bytes,
        headers={"content-type": "image/jpeg"},
        request=httpx.Request("GET", "https://example.com/huge.jpg")
    )

    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_resp
        response = client.post(
            "/api/predict/image",
            json={"imageUrl": "https://example.com/huge.jpg"}
        )

    assert response.status_code == 400
    assert "exceeds the maximum limit" in response.json()["detail"]

def test_corrupt_image_content():
    """7. Test rejection of corrupted image bytes."""
    corrupt_bytes = b"\xFF\xD8\xFF\xE0FakeCorruptContentNotAnImage"

    mock_resp = httpx.Response(
        status_code=200,
        content=corrupt_bytes,
        headers={"content-type": "image/jpeg"},
        request=httpx.Request("GET", "https://example.com/corrupted.jpg")
    )

    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_resp
        response = client.post(
            "/api/predict/image",
            json={"imageUrl": "https://example.com/corrupted.jpg"}
        )

    assert response.status_code == 400
    assert "Invalid or corrupted image content" in response.json()["detail"]

def test_network_timeout_handling():
    """8. Test handling of remote HTTP timeouts."""
    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.side_effect = httpx.TimeoutException("Connection timed out")
        response = client.post(
            "/api/predict/image",
            json={"imageUrl": "https://example.com/timeout.jpg"}
        )

    assert response.status_code == 504
    assert "Timeout fetching image" in response.json()["detail"]

def test_network_connection_error_handling():
    """9. Test handling of remote connection failures."""
    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.side_effect = httpx.ConnectError("Connection refused")
        response = client.post(
            "/api/predict/image",
            json={"imageUrl": "https://example.com/broken.jpg"}
        )

    assert response.status_code == 400
    assert "Network error downloading image" in response.json()["detail"]

def test_classification_and_severity_categories():
    """10. Test classification categories and severity levels across test image profiles."""
    # Test organic green image -> WET_WASTE
    green_bytes = create_test_image_bytes(color=(30, 180, 40))
    mock_resp = httpx.Response(
        status_code=200,
        content=green_bytes,
        headers={"content-type": "image/jpeg"},
        request=httpx.Request("GET", "https://example.com/green.jpg")
    )

    with patch("httpx.AsyncClient.get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_resp
        res = client.post(
            "/api/predict/image",
            json={"imageUrl": "https://example.com/green.jpg"}
        )
    assert res.status_code == 200
    body = res.json()
    assert body["data"]["classification"]["category"] == "WET_WASTE"
    assert body["data"]["severity"]["level"] in ["LOW", "MEDIUM", "HIGH", "CRITICAL"]
