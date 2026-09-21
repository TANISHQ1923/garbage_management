# Smart Garbage Management — AI Analysis Microservice

Independent Python microservice providing garbage classification and waste severity estimation for the Smart Garbage Management ecosystem.

---

## 1. Overview & Model Status

- **Service Role**: Assistive computer vision analysis for incoming citizen garbage complaints.
- **Current Model Status**: `DEVELOPMENT_BASELINE`
- **Model Transparency & Honesty Policy**:
  - The service strictly returns `modelStatus: "DEVELOPMENT_BASELINE"`.
  - Confidence is intentionally reported as `null` (no fabricated metrics or false claims like "95% accuracy").
  - The analysis is purely an assistive heuristic. Officer verification is always required.
  - The AI service has zero authority to alter official municipal complaint status or automatically reject issues.

---

## 2. Supported Classes

### Waste Categories
- `WET_WASTE`: Biodegradable / organic / food waste
- `DRY_WASTE`: General non-biodegradable debris
- `PLASTIC`: Bottles, bags, polythene, packaging
- `METAL`: Cans, foils, scrap pieces
- `PAPER`: Cardboard, newspapers, packaging
- `GLASS`: Bottles, broken shards, containers
- `MIXED_WASTE`: Complex roadside piles
- `OTHER`: Unclassified solid waste

### Severity Levels
- `LOW`: Minor surface litter
- `MEDIUM`: Moderate roadside accumulation
- `HIGH`: Heavy accumulation impeding footpaths
- `CRITICAL`: Severe overflowing heap posing health hazards

---

## 3. Setup and Installation

### Prerequisites
- Python 3.11+
- Virtual environment (`venv`)

### Setup Virtual Environment
```bash
cd ai_service

# Create virtual environment
python -m venv venv

# Activate on Windows (PowerShell)
.\venv\Scripts\Activate.ps1

# Activate on Linux/macOS
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

---

## 4. Running the Service

```bash
# Start FastAPI service with auto-reload
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The service runs at `http://localhost:8000`. Interactive API documentation is available at:
- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`

---

## 5. API Endpoints

### 1. Health Check
```http
GET /api/health
```
**Response:**
```json
{
  "success": true,
  "status": "HEALTHY",
  "service": "Smart Garbage AI Analysis Service",
  "modelStatus": "DEVELOPMENT_BASELINE",
  "version": "1.0.0"
}
```

### 2. Predict Waste Classification & Severity
```http
POST /api/predict/image
Content-Type: application/json

{
  "imageUrl": "https://res.cloudinary.com/demo/image/upload/sample.jpg"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "classification": {
      "category": "PLASTIC",
      "confidence": null
    },
    "severity": {
      "level": "HIGH",
      "confidence": null
    },
    "modelStatus": "DEVELOPMENT_BASELINE",
    "processedAt": "2026-09-17T09:30:00.000000+00:00"
  }
}
```

---

## 6. Security Features
- **SSRF Defense**: Automatically detects and rejects private, loopback (`127.0.0.1`, `localhost`), link-local, and internal subnet targets.
- **Payload Limits**: Rejects images exceeding 10MB (`MAX_IMAGE_SIZE_MB`).
- **Content Inspection**: Verifies image header MIME types and confirms image integrity with Pillow.
- **Zero Local Persistence**: Downloaded media is processed strictly in-memory and discarded.

---

## 7. Replacing the Baseline with a Trained Model

The architecture is built with dependency injection and modular boundaries to facilitate easy model upgrades:

1. **Classification Upgrade**:
   - Train a PyTorch/TensorFlow CNN (e.g. `MobileNetV3`, `EfficientNet-B0`) or Vision Transformer on the TACO garbage dataset.
   - Export the model weights to `ai_service/app/models/weights/garbage_classifier.onnx` or `.pt`.
   - Update `ai_service/app/services/image_classifier.py` to load the weights and return real softmax confidence values:
     ```python
     return ClassificationResult(
         category=predicted_category,
         confidence=round(float(softmax_score), 3)
     )
     ```
   - Update `modelStatus` from `"DEVELOPMENT_BASELINE"` to `"PRODUCTION_MOBILENET_V3"`.

2. **Severity Estimation Upgrade**:
   - Train a regression or 4-class density model to estimate waste pile volume.
   - Update `ai_service/app/services/severity_estimator.py` accordingly.

---

## 8. Running Automated Tests

```bash
pytest
```
All 10 unit and security test cases execute in < 2 seconds.
