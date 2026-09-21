import os

class Settings:
    PORT: int = int(os.getenv("AI_SERVICE_PORT", "8000"))
    HOST: str = os.getenv("AI_SERVICE_HOST", "0.0.0.0")
    MAX_IMAGE_SIZE_MB: int = int(os.getenv("MAX_IMAGE_SIZE_MB", "10"))
    MAX_IMAGE_SIZE_BYTES: int = MAX_IMAGE_SIZE_MB * 1024 * 1024
    DOWNLOAD_TIMEOUT_SECONDS: float = float(os.getenv("DOWNLOAD_TIMEOUT_SECONDS", "5.0"))
    MODEL_STATUS: str = os.getenv("MODEL_STATUS", "DEVELOPMENT_BASELINE")

    ALLOWED_CONTENT_TYPES = {
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    }

    CATEGORIES = [
        "WET_WASTE",
        "DRY_WASTE",
        "PLASTIC",
        "METAL",
        "PAPER",
        "GLASS",
        "MIXED_WASTE",
        "OTHER"
    ]

    SEVERITIES = [
        "LOW",
        "MEDIUM",
        "HIGH",
        "CRITICAL"
    ]

settings = Settings()
