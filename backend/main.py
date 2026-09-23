"""
TRYON AI — FastAPI Backend Service.
Entry point for the AI Virtual Try-On API server.
"""
import logging
from fastapi import FastAPI, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from backend.api.tryon import router as tryon_router
from backend.config import get_settings
from backend.models.schemas import HealthResponse

# Configure structured logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("tryon_backend")

settings = get_settings()

app = FastAPI(
    title="TRYON AI Backend",
    description="High-performance AI Virtual Try-On backend service powering the TRYON AI mobile client.",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json"
)

# Configure Cross-Origin Resource Sharing (CORS)
allowed_origins = settings.get_allowed_origins_list()
logger.info(f"Configuring CORS with allowed origins: {allowed_origins}")

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Global Error Handling: Ensures consistent JSON responses with no exposed stack traces
@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    if isinstance(exc.detail, dict) and "code" in exc.detail:
        error_body = exc.detail
    else:
        error_body = {
            "code": f"HTTP_{exc.status_code}",
            "message": str(exc.detail)
        }

    return JSONResponse(
        status_code=exc.status_code,
        content={"success": False, "error": error_body}
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    errors = exc.errors()
    first_msg = errors[0]["msg"] if errors else "Invalid request payload"
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "success": False,
            "error": {
                "code": "VALIDATION_ERROR",
                "message": first_msg
            }
        }
    )


@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled internal server error: {exc}", exc_info=True)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "success": False,
            "error": {
                "code": "INTERNAL_SERVER_ERROR",
                "message": "An internal server error occurred while processing the request."
            }
        }
    )


# Health Check Endpoint
@app.get(
    "/health",
    response_model=HealthResponse,
    tags=["System"],
    summary="Backend Health Check",
    description="Returns service status, active provider, and worker configuration status."
)
async def health_check():
    current_settings = get_settings()
    return HealthResponse(
        status="ok",
        service="TRYON AI Backend",
        demo_mode=current_settings.demo_mode,
        vton_provider=current_settings.vton_provider,
        catvton_worker_configured=current_settings.is_catvton_worker_configured
    )


# Mount Try-On API Routes
app.include_router(tryon_router)


@app.get(
    "/api/download/catvton_colab_worker.ipynb",
    tags=["Worker"],
    summary="Download CatVTON Colab Worker Notebook",
    description="Directly downloads the Phase 4 Google Colab notebook for the CatVTON GPU worker."
)
async def download_worker_notebook():
    import os
    from fastapi.responses import FileResponse
    notebook_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "worker", "catvton_colab_worker.ipynb")
    if not os.path.exists(notebook_path):
        raise HTTPException(status_code=404, detail="Notebook file not found.")
    return FileResponse(
        notebook_path,
        media_type="application/x-ipynb+json",
        filename="catvton_colab_worker.ipynb"
    )


if __name__ == "__main__":
    import uvicorn
    logger.info(f"Starting TRYON AI backend on {settings.host}:{settings.port}")
    uvicorn.run("backend.main:app", host=settings.host, port=settings.port, reload=True)
