from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from app.routes.predict import router as predict_router
from app.config import settings

app = FastAPI(
    title="Smart Garbage AI Analysis Service",
    description="Assistive AI microservice for garbage classification and severity estimation",
    version="1.0.0"
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount API routes
app.include_router(predict_router)

@app.get("/")
async def root():
    return {
        "service": "Smart Garbage AI Analysis Service",
        "health": "/api/health",
        "docs": "/docs",
        "modelStatus": settings.MODEL_STATUS
    }

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=500,
        content={
            "success": False,
            "error": "Internal AI Service Error",
            "detail": str(exc)
        }
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=True)
