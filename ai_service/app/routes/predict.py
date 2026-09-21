from datetime import datetime, timezone
from fastapi import APIRouter, HTTPException, Request
from app.models.prediction import (
    PredictionRequest,
    PredictionResponse,
    PredictionData,
    HealthResponse
)
from app.utils.image_utils import fetch_and_validate_image
from app.services.image_classifier import classify_garbage_image
from app.services.severity_estimator import estimate_waste_severity
from app.config import settings

router = APIRouter(prefix="/api", tags=["Prediction"])

@router.get("/health", response_model=HealthResponse)
async def health_check():
    """
    Health check endpoint reporting service vitality and model status.
    """
    return HealthResponse(
        success=True,
        status="HEALTHY",
        service="Smart Garbage AI Analysis Service",
        modelStatus=settings.MODEL_STATUS,
        version="1.0.0"
    )

@router.post("/predict/image", response_model=PredictionResponse)
async def predict_image(request: PredictionRequest):
    """
    Analyzes a public image URL, returning:
    - Garbage category classification
    - Severity estimation level
    - Model status ('DEVELOPMENT_BASELINE')
    - Timestamp of execution
    """
    # 1. Download and securely validate image
    image, size_bytes, mime_type = await fetch_and_validate_image(request.imageUrl)

    # 2. Run classification
    classification = classify_garbage_image(image)

    # 3. Run severity estimation
    severity = estimate_waste_severity(image)

    # 4. Construct unified prediction data
    data = PredictionData(
        classification=classification,
        severity=severity,
        modelStatus=settings.MODEL_STATUS,
        processedAt=datetime.now(timezone.utc).isoformat()
    )

    return PredictionResponse(
        success=True,
        data=data
    )
