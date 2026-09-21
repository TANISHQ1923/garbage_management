"""
Severity Estimator - Development Baseline Implementation
========================================================
IMPORTANT NOTICE:
This module provides an assistive DEVELOPMENT BASELINE estimation of garbage pile severity.
It evaluates spatial variance and pixel intensity spread to gauge physical clutter.

IT IS NOT AN OFFICIAL MUNICIPAL ASSESSMENT.
- Officer verification is strictly required.
- Confidence is intentionally null for development baseline.
- Categories supported: LOW, MEDIUM, HIGH, CRITICAL.
"""

from PIL import Image
from app.models.prediction import SeverityResult
from app.config import settings

def estimate_waste_severity(img: Image.Image) -> SeverityResult:
    """
    Estimates waste accumulation severity based on spatial luminance variance.
    Higher pixel variance indicates high visual clutter, multiple overlapping objects,
    or overflowing waste heaps.
    """
    # Convert to grayscale thumbnail
    gray = img.convert("L").resize((64, 64))
    if hasattr(gray, "get_flattened_data"):
        pixels = list(gray.get_flattened_data())
    else:
        pixels = list(gray.getdata())
    n = len(pixels)

    if n == 0:
        return SeverityResult(level="LOW", confidence=None)

    mean = sum(pixels) / n
    variance = sum((p - mean) ** 2 for p in pixels) / n
    std_dev = (variance ** 0.5) / 255.0  # Normalized to 0.0 - 1.0

    # Classify severity based on texture variance
    if std_dev > 0.28:
        level = "CRITICAL"
    elif std_dev > 0.18:
        level = "HIGH"
    elif std_dev > 0.10:
        level = "MEDIUM"
    else:
        level = "LOW"

    return SeverityResult(
        level=level,
        confidence=None
    )
