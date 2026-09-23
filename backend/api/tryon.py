"""
FastAPI endpoints for virtual try-on workflow:
- POST /api/try-on: Uploads person & garment images and queues inference
- GET /api/try-on/status/{job_id}: Polls async processing status
- GET /api/try-on/results/{filename}: Serves completed result images safely
"""
from typing import Optional
from fastapi import APIRouter, File, Form, HTTPException, Request, UploadFile, status
from fastapi.responses import FileResponse

from backend.config import get_settings
from backend.models.schemas import (
    ErrorResponse,
    ErrorDetail,
    JobStatus,
    TryOnJobCreateResponse,
    TryOnStatusResponse,
)
from backend.services.storage import storage_service
from backend.services.virtual_tryon import tryon_service
from backend.utils.image_utils import (
    ImageValidationError,
    validate_image_bytes,
    validate_image_extension,
)

router = APIRouter(prefix="/api/try-on", tags=["Virtual Try-On"])


async def _read_file_safely(upload_file: UploadFile, max_bytes: int) -> bytes:
    """Reads uploaded file with strict size cutoff to prevent memory exhaustion."""
    chunks = []
    total_bytes = 0
    while True:
        chunk = await upload_file.read(64 * 1024)  # 64KB chunks
        if not chunk:
            break
        total_bytes += len(chunk)
        if total_bytes > max_bytes:
            raise ImageValidationError(
                code="FILE_TOO_LARGE",
                message=f"File exceeds maximum allowed size of {max_bytes // (1024 * 1024)}MB."
            )
        chunks.append(chunk)
    return b"".join(chunks)


@router.post(
    "",
    response_model=TryOnJobCreateResponse,
    responses={
        400: {"model": ErrorResponse, "description": "Invalid image or parameters"},
        413: {"model": ErrorResponse, "description": "Payload too large"},
    },
    summary="Submit Virtual Try-On Request",
    description="Uploads a person portrait and garment image to queue an AI try-on inference job."
)
async def submit_try_on(
    request: Request,
    person_image: UploadFile = File(..., description="Front-facing person photo"),
    garment_image: UploadFile = File(..., description="Clean garment or outfit photo"),
    category: Optional[str] = Form("blazers", description="Clothing category"),
    garment_type: Optional[str] = Form("upper", description="Upper / lower / overall classification")
):
    settings = get_settings()

    # Validate filenames exist
    if not person_image.filename or not garment_image.filename:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"code": "MISSING_FILENAME", "message": "Both uploaded files must have valid filenames."}
        )

    # Read and validate person image
    try:
        person_bytes = await _read_file_safely(person_image, settings.max_image_bytes)
        person_pil, _, _ = validate_image_bytes(
            file_bytes=person_bytes,
            filename=person_image.filename,
            content_type=person_image.content_type,
            max_size_bytes=settings.max_image_bytes
        )
    except ImageValidationError as err:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"code": f"PERSON_{err.code}", "message": f"Person photo error: {err.message}"}
        )

    # Read and validate garment image
    try:
        garment_bytes = await _read_file_safely(garment_image, settings.max_image_bytes)
        garment_pil, _, _ = validate_image_bytes(
            file_bytes=garment_bytes,
            filename=garment_image.filename,
            content_type=garment_image.content_type,
            max_size_bytes=settings.max_image_bytes
        )
    except ImageValidationError as err:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"code": f"GARMENT_{err.code}", "message": f"Garment photo error: {err.message}"}
        )

    # Save images to local temporary upload storage
    _, person_path = storage_service.save_image(person_pil, target_folder="uploads", prefix="person")
    _, garment_path = storage_service.save_image(garment_pil, target_folder="uploads", prefix="garment")

    # Submit job for asynchronous processing
    job_id = tryon_service.submit_job(
        person_image_path=person_path,
        garment_image_path=garment_path,
        category=category or "blazers",
        garment_type=garment_type or "upper"
    )

    return TryOnJobCreateResponse(
        success=True,
        job_id=job_id,
        status=JobStatus.QUEUED.value
    )


@router.get(
    "/status/{job_id}",
    response_model=TryOnStatusResponse,
    responses={404: {"model": ErrorResponse, "description": "Job not found"}},
    summary="Check Try-On Job Status",
    description="Polls the current processing status of an asynchronous Virtual Try-On job."
)
async def get_job_status(job_id: str, request: Request):
    job = tryon_service.get_job(job_id)
    if not job:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"code": "JOB_NOT_FOUND", "message": f"No job found with ID '{job_id}'."}
        )

    settings = get_settings()
    base_url = settings.base_url or str(request.base_url).rstrip('/')

    result_url = None
    if job.status == JobStatus.COMPLETED and job.result_filename:
        result_url = storage_service.build_result_url(job.result_filename, base_url=base_url)

    return TryOnStatusResponse(
        success=True,
        job_id=job.job_id,
        status=job.status.value,
        result_url=result_url,
        message=job.message,
        progress=job.progress,
        error=job.error
    )


@router.get(
    "/results/{filename}",
    summary="Download Generated Result Image",
    description="Serves the generated try-on output image with secure path sanitization."
)
async def get_result_image(filename: str):
    path = storage_service.get_result_path(filename)
    if not path or not path.exists():
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"code": "IMAGE_NOT_FOUND", "message": "The requested result image does not exist or has expired."}
        )

    return FileResponse(
        path=path,
        media_type="image/jpeg",
        filename="tryon-ai-result.jpg"
    )
