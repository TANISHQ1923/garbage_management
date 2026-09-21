"""
Image Classifier - Development Baseline Implementation
======================================================
IMPORTANT NOTICE:
This module provides a deterministic DEVELOPMENT BASELINE for initial system integration.
It analyzes image color characteristics, saturation, and luminance as an assistive
development placeholder.

IT IS NOT A TRAINED COMPUTER VISION MODEL.
- No fabricated confidence percentages are generated (confidence is explicitly null).
- modelStatus is strictly 'DEVELOPMENT_BASELINE'.
- In future production phases, this module is designed to be replaced by a fine-tuned
  deep learning model (e.g. MobileNetV3 / EfficientNet on the TACO garbage dataset).
"""

from typing import Tuple
from PIL import Image
from app.models.prediction import ClassificationResult
from app.config import settings

def _analyze_color_profile(img: Image.Image) -> Tuple[float, float, float, float]:
    """
    Computes normalized average R, G, B, and overall perceived brightness (0.0 to 1.0).
    """
    # Resize to standard thumbnail for fast deterministic profile computation
    thumb = img.resize((64, 64))
    if hasattr(thumb, "get_flattened_data"):
        pixels = list(thumb.get_flattened_data())
    else:
        pixels = list(thumb.getdata())
    total_pixels = len(pixels)

    r_sum = sum(p[0] for p in pixels)
    g_sum = sum(p[1] for p in pixels)
    b_sum = sum(p[2] for p in pixels)

    r_avg = r_sum / (total_pixels * 255.0)
    g_avg = g_sum / (total_pixels * 255.0)
    b_avg = b_sum / (total_pixels * 255.0)

    # Perceived luminance standard
    brightness = 0.299 * r_avg + 0.587 * g_avg + 0.114 * b_avg

    return r_avg, g_avg, b_avg, brightness

def classify_garbage_image(img: Image.Image) -> ClassificationResult:
    """
    Classifies a garbage image into one of the 8 supported categories:
    [WET_WASTE, DRY_WASTE, PLASTIC, METAL, PAPER, GLASS, MIXED_WASTE, OTHER]
    """
    r, g, b, brightness = _analyze_color_profile(img)

    # Deterministic heuristics for development baseline
    # High green or organic earthy tones with medium luminance -> Organic / Wet Waste
    if g > 0.38 and g > r and g > b:
        category = "WET_WASTE"
    # High blue or cyan dominance -> Plastic bags / bottles / synthetics
    elif b > 0.40 and b > r:
        category = "PLASTIC"
    # Very bright, low-saturation neutral balance -> Paper / cardboard
    elif brightness > 0.65 and abs(r - g) < 0.08 and abs(g - b) < 0.08:
        category = "PAPER"
    # Neutral grey metallic balance with moderate reflection
    elif 0.35 <= brightness <= 0.65 and abs(r - g) < 0.05 and abs(g - b) < 0.05:
        category = "METAL"
    # Moderate brightness with blue-green tinted specular reflection
    elif (g + b) / 2.0 > r * 1.3 and brightness > 0.40:
        category = "GLASS"
    # Multi-tonal contrast or high entropy -> Mixed roadside waste
    elif abs(r - b) > 0.15 and abs(g - r) > 0.15:
        category = "MIXED_WASTE"
    # General dry debris
    elif brightness > 0.30:
        category = "DRY_WASTE"
    else:
        category = "OTHER"

    # Strict compliance: confidence must NOT be fabricated without a real ML model
    return ClassificationResult(
        category=category,
        confidence=None
    )
