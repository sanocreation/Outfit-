"""
Image utilities for validation, decoding, dimension checks, and safe resizing.
Protects memory by enforcing strict file size cutoffs and sanitizing inputs.
"""
import io
import os
from typing import Optional, Tuple
from PIL import Image, ImageOps, UnidentifiedImageError

ALLOWED_MIME_TYPES = {
    "image/jpeg",
    "image/jpg",
    "image/png",
    "image/webp"
}

ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}

# Bounds for reasonable dimensions
MIN_DIMENSION = 64
MAX_DIMENSION = 4096
MAX_PROCESSING_DIMENSION = 2048


class ImageValidationError(Exception):
    def __init__(self, code: str, message: str):
        self.code = code
        self.message = message
        super().__init__(message)


def validate_image_extension(filename: str) -> str:
    """Validates file extension against allowed formats."""
    _, ext = os.path.splitext(filename.lower())
    if ext not in ALLOWED_EXTENSIONS:
        raise ImageValidationError(
            code="INVALID_EXTENSION",
            message=f"Unsupported file extension '{ext}'. Allowed extensions: {', '.join(ALLOWED_EXTENSIONS)}"
        )
    return ext


def validate_image_bytes(
    file_bytes: bytes,
    filename: str,
    content_type: Optional[str],
    max_size_bytes: int
) -> Tuple[Image.Image, int, int]:
    """
    Validates uploaded file bytes:
    1. Ensures non-empty
    2. Enforces maximum size
    3. Validates MIME type
    4. Decodes image with PIL and verifies integrity
    5. Checks dimensions are within reasonable boundaries
    6. Safely auto-orients using EXIF and resizes if oversized

    Returns:
        Tuple of (PIL.Image in RGB, original_width, original_height)
    """
    if not file_bytes:
        raise ImageValidationError(
            code="EMPTY_FILE",
            message=f"Uploaded file '{filename}' is empty."
        )

    if len(file_bytes) > max_size_bytes:
        max_mb = max_size_bytes / (1024 * 1024)
        raise ImageValidationError(
            code="FILE_TOO_LARGE",
            message=f"File exceeds maximum allowed size of {max_mb:.0f}MB."
        )

    # Validate MIME type if supplied
    if content_type and content_type.lower() not in ALLOWED_MIME_TYPES:
        raise ImageValidationError(
            code="INVALID_MIME_TYPE",
            message=f"Invalid content type '{content_type}'. Must be one of: {', '.join(ALLOWED_MIME_TYPES)}"
        )

    validate_image_extension(filename)

    # Verify image integrity
    try:
        raw_stream = io.BytesIO(file_bytes)
        img = Image.open(raw_stream)
        img.verify()
    except (UnidentifiedImageError, ValueError, Exception) as exc:
        raise ImageValidationError(
            code="CORRUPTED_IMAGE",
            message=f"The uploaded file is not a valid or readable image: {str(exc)}"
        )

    # Reopen to load actual pixel data after verify()
    raw_stream.seek(0)
    image = Image.open(raw_stream)
    image = ImageOps.exif_transpose(image)

    width, height = image.size

    if width < MIN_DIMENSION or height < MIN_DIMENSION:
        raise ImageValidationError(
            code="IMAGE_TOO_SMALL",
            message=f"Image resolution ({width}x{height}) is too small. Minimum required is {MIN_DIMENSION}x{MIN_DIMENSION}px."
        )

    if width > MAX_DIMENSION or height > MAX_DIMENSION:
        raise ImageValidationError(
            code="IMAGE_TOO_LARGE",
            message=f"Image resolution ({width}x{height}) exceeds maximum limit of {MAX_DIMENSION}x{MAX_DIMENSION}px."
        )

    # Convert to standard RGB
    if image.mode != "RGB":
        image = image.convert("RGB")

    # Downscale oversized images to save memory while preserving aspect ratio
    if width > MAX_PROCESSING_DIMENSION or height > MAX_PROCESSING_DIMENSION:
        image.thumbnail((MAX_PROCESSING_DIMENSION, MAX_PROCESSING_DIMENSION), Image.Resampling.LANCZOS)

    return image, width, height
