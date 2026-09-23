"""
CatVTON Virtual Try-On Provider.
Implements VirtualTryOnProvider interface and delegates neural inference
to an external GPU worker (e.g. Google Colab T4 / RunPod / Replicate).
"""
import io
import logging
from typing import Optional, Tuple
from PIL import Image

from backend.config import get_settings
from backend.services.base_provider import VirtualTryOnProvider, ProgressCallback
from backend.services.catvton_worker_client import (
    CatVTONWorkerClient,
    WorkerNotConfiguredError,
    WorkerMalformedResponseError,
)
from backend.services.storage import storage_service

logger = logging.getLogger("tryon_backend.catvton_provider")


class CatVTONProvider(VirtualTryOnProvider):
    def __init__(self, worker_client: Optional[CatVTONWorkerClient] = None):
        self._worker_client = worker_client or CatVTONWorkerClient()

    @property
    def provider_id(self) -> str:
        return "CATVTON_GPU"

    @property
    def display_name(self) -> str:
        return "CatVTON Neural Diffusion Engine (GPU)"

    @property
    def worker_client(self) -> CatVTONWorkerClient:
        return self._worker_client

    async def generate_try_on(
        self,
        person_image_path: str,
        garment_image_path: str,
        category: str,
        garment_type: str,
        on_progress: Optional[ProgressCallback] = None
    ) -> Tuple[str, str]:
        """
        Executes CatVTON try-on on the remote GPU worker.
        """
        logger.info(f"Starting CatVTON inference pipeline for category '{category}'")

        if not self._worker_client.is_configured():
            raise WorkerNotConfiguredError(
                "CatVTON external GPU worker URL is not configured. "
                "Set CATVTON_WORKER_URL or switch to demo mode."
            )

        # 1. Submit images to worker
        if on_progress:
            on_progress(1, "Sending images to CatVTON GPU worker...", 0.15)

        worker_job_id = await self._worker_client.submit_job(
            person_image_path=person_image_path,
            garment_image_path=garment_image_path,
            category=category,
            garment_type=garment_type
        )

        # 2. Poll worker until complete
        result_bytes = await self._worker_client.poll_until_complete(
            worker_job_id=worker_job_id,
            on_progress=on_progress
        )

        # 3. Verify, preserve original face & figure identity, and save output
        try:
            result_img = Image.open(io.BytesIO(result_bytes))
            result_img.verify()
            # Reopen for processing and saving
            result_img = Image.open(io.BytesIO(result_bytes))
            if result_img.mode != "RGB":
                result_img = result_img.convert("RGB")

            # Double-layer face preservation: Ensure face and neck area
            # strictly matches the original person photo with zero alteration
            orig_person = Image.open(person_image_path).convert("RGB")
            orig_person_resized = orig_person.resize(result_img.size, Image.Resampling.LANCZOS)
            rw, rh = result_img.size
            face_safe_h = int(rh * 0.22)
            face_patch = orig_person_resized.crop((0, 0, rw, face_safe_h))
            result_img.paste(face_patch, (0, 0))

        except Exception as exc:
            raise WorkerMalformedResponseError(f"Worker returned unreadable image stream: {exc}")

        filename, full_path = storage_service.save_image(
            result_img,
            target_folder="results",
            prefix="tryon_catvton"
        )

        logger.info(f"CatVTON result saved to storage: {filename}")
        if on_progress:
            on_progress(10, "Virtual Try-On finalized.", 1.0)

        return filename, full_path
