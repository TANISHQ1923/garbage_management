from typing import Optional
from pydantic import BaseModel, Field, field_validator
from datetime import datetime, timezone
import re

class PredictionRequest(BaseModel):
    imageUrl: str = Field(..., description="Public HTTP/HTTPS URL of the garbage image")

    @field_validator("imageUrl")
    @classmethod
    def validate_url(cls, v: str) -> str:
        trimmed = v.strip()
        if not re.match(r"^https?://[^\s/$.?#].[^\s]*$", trimmed, re.IGNORECASE):
            raise ValueError("Invalid URL: Must be a valid http or https URL.")
        return trimmed

class ClassificationResult(BaseModel):
    category: str = Field(..., description="Garbage waste category")
    confidence: Optional[float] = Field(None, description="Prediction confidence score (null for development baseline)")

class SeverityResult(BaseModel):
    level: str = Field(..., description="Estimated waste severity level: LOW, MEDIUM, HIGH, CRITICAL")
    confidence: Optional[float] = Field(None, description="Estimation confidence score (null for development baseline)")

class PredictionData(BaseModel):
    classification: ClassificationResult
    severity: SeverityResult
    modelStatus: str = Field("DEVELOPMENT_BASELINE", description="Status of the AI model performing the analysis")
    processedAt: str = Field(
        default_factory=lambda: datetime.now(timezone.utc).isoformat(),
        description="ISO 8601 timestamp of analysis execution"
    )

class PredictionResponse(BaseModel):
    success: bool = True
    data: PredictionData

class HealthResponse(BaseModel):
    success: bool = True
    status: str = "HEALTHY"
    service: str = "Smart Garbage AI Analysis Service"
    modelStatus: str = "DEVELOPMENT_BASELINE"
    version: str = "1.0.0"
