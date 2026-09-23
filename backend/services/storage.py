"""
Storage service handling local temporary file storage for uploads and results.
Designed with a modular interface to support future migration to S3/GCS/Supabase.
Enforces strict filename sanitization and path traversal prevention.
"""
import os
import uuid
from pathlib import Path
from typing import Optional, Tuple
from PIL import Image

BASE_DIR = Path(__file__).resolve().parent.parent
STORAGE_ROOT = BASE_DIR / "storage"
UPLOADS_DIR = STORAGE_ROOT / "uploads"
RESULTS_DIR = STORAGE_ROOT / "results"


class StorageService:
    def __init__(self, storage_root: Optional[Path] = None):
        self.storage_root = storage_root or STORAGE_ROOT
        self.uploads_dir = self.storage_root / "uploads"
        self.results_dir = self.storage_root / "results"
        self._ensure_directories()

    def _ensure_directories(self):
        """Ensures storage directories exist."""
        self.uploads_dir.mkdir(parents=True, exist_ok=True)
        self.results_dir.mkdir(parents=True, exist_ok=True)

    def save_image(self, image: Image.Image, target_folder: str = "uploads", prefix: str = "img") -> Tuple[str, str]:
        """
        Saves a PIL Image safely as JPEG with a UUID-based filename.
        Returns a tuple of (relative_filename, absolute_path_str).
        """
        folder = self.results_dir if target_folder == "results" else self.uploads_dir
        unique_name = f"{prefix}_{uuid.uuid4().hex[:16]}.jpg"
        target_path = folder / unique_name

        # Save with high quality JPEG
        image.save(target_path, format="JPEG", quality=92, optimize=True)
        return unique_name, str(target_path.resolve())

    def get_result_path(self, filename: str) -> Optional[Path]:
        """
        Safely retrieves the Path for a result file.
        Strictly prevents directory traversal (e.g. '../').
        """
        # Remove any leading path separators
        clean_name = os.path.basename(filename)
        resolved_path = (self.results_dir / clean_name).resolve()

        # Security check: must reside inside results_dir
        try:
            resolved_path.relative_to(self.results_dir.resolve())
        except ValueError:
            return None

        if resolved_path.is_file():
            return resolved_path
        return None

    def build_result_url(self, filename: str, base_url: str = "") -> str:
        """Constructs safe URL for downloading the result."""
        clean_name = os.path.basename(filename)
        endpoint = f"/api/try-on/results/{clean_name}"
        if base_url:
            return f"{base_url.rstrip('/')}{endpoint}"
        return endpoint


# Singleton instance
storage_service = StorageService()
