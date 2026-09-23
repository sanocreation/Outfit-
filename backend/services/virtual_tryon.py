"""
Virtual Try-On Orchestration and In-Memory Job Management Service.
Coordinates image jobs between the API layer, storage, and the active VTON provider.
Supports seamless switching between DemoVtonProvider and CatVTONProvider with auto-fallback.
"""
import asyncio
import logging
import time
import uuid
from dataclasses import dataclass, field
from typing import Dict, Optional

from backend.config import get_settings
from backend.models.schemas import JobStatus
from backend.services.base_provider import VirtualTryOnProvider
from backend.services.catvton_provider import CatVTONProvider
from backend.services.demo_provider import DemoVtonProvider
from backend.services.storage import storage_service

logger = logging.getLogger("tryon_backend.service")


@dataclass
class TryOnJob:
    job_id: str
    status: JobStatus = JobStatus.QUEUED
    created_at: float = field(default_factory=time.time)
    updated_at: float = field(default_factory=time.time)
    result_filename: Optional[str] = None
    result_path: Optional[str] = None
    message: Optional[str] = "Job queued for AI processing"
    progress: float = 0.0
    error: Optional[str] = None


class VirtualTryOnService:
    def __init__(self):
        self._jobs: Dict[str, TryOnJob] = {}
        self._demo_provider = DemoVtonProvider()
        self._catvton_provider = CatVTONProvider()

    def get_provider(self) -> VirtualTryOnProvider:
        """
        Resolves active provider based on configuration.
        Defaults to DemoVtonProvider if demo_mode=True or vton_provider="demo".
        Switches to CatVTONProvider if demo_mode=False and vton_provider="catvton".
        """
        settings = get_settings()
        if not settings.demo_mode and settings.vton_provider.lower().strip() == "catvton":
            return self._catvton_provider
        return self._demo_provider

    def get_job(self, job_id: str) -> Optional[TryOnJob]:
        return self._jobs.get(job_id)

    def submit_job(
        self,
        person_image_path: str,
        garment_image_path: str,
        category: str = "blazers",
        garment_type: str = "upper"
    ) -> str:
        """
        Creates a new asynchronous job and queues background inference.
        Returns unique job_id.
        """
        job_id = f"job_{uuid.uuid4().hex[:12]}"
        job = TryOnJob(job_id=job_id, status=JobStatus.QUEUED)
        self._jobs[job_id] = job

        logger.info(f"Job created: {job_id}")

        # Launch background task
        asyncio.create_task(
            self._execute_job(
                job_id=job_id,
                person_image_path=person_image_path,
                garment_image_path=garment_image_path,
                category=category,
                garment_type=garment_type
            )
        )

        return job_id

    async def _execute_job(
        self,
        job_id: str,
        person_image_path: str,
        garment_image_path: str,
        category: str,
        garment_type: str
    ):
        job = self._jobs.get(job_id)
        if not job:
            return

        job.status = JobStatus.PROCESSING
        job.updated_at = time.time()
        job.message = "Initializing AI synthesis pipeline..."

        def on_progress(step: int, msg: str, prog: float):
            job.message = msg
            job.progress = prog
            job.updated_at = time.time()

        settings = get_settings()
        provider = self.get_provider()
        logger.info(f"Provider selected for job {job_id}: {provider.display_name} ({provider.provider_id})")

        filename = None
        full_path = None

        try:
            logger.info(f"Inference request started for job {job_id}")
            filename, full_path = await provider.generate_try_on(
                person_image_path=person_image_path,
                garment_image_path=garment_image_path,
                category=category,
                garment_type=garment_type,
                on_progress=on_progress
            )
        except Exception as exc:
            logger.warning(f"Inference failed with provider '{provider.provider_id}' for job {job_id}: {exc}")

            # Check if auto-fallback to demo is enabled
            if provider.provider_id != "DEMO_SIMULATOR" and settings.auto_fallback_to_demo:
                logger.info(f"Fallback activated: Auto-fallback to DemoVtonProvider for job {job_id}")
                on_progress(1, "GPU worker offline. Generating preview...", 0.3)
                try:
                    filename, full_path = await self._demo_provider.generate_try_on(
                        person_image_path=person_image_path,
                        garment_image_path=garment_image_path,
                        category=category,
                        garment_type=garment_type,
                        on_progress=on_progress
                    )
                except Exception as fallback_exc:
                    logger.error(f"Fallback Demo provider also failed for job {job_id}: {fallback_exc}")
                    job.status = JobStatus.FAILED
                    job.error = "Generation failed during virtual try-on inference. Please verify uploaded photos and retry."
                    job.message = "Processing failed"
                    job.updated_at = time.time()
                    return
            else:
                job.status = JobStatus.FAILED
                job.error = "Generation failed during virtual try-on inference. Please verify uploaded photos and retry."
                job.message = "Processing failed"
                job.updated_at = time.time()
                return

        # Mark job as completed
        job.status = JobStatus.COMPLETED
        job.result_filename = filename
        job.result_path = full_path
        job.message = "AI Try-On completed successfully"
        job.progress = 1.0
        job.updated_at = time.time()
        logger.info(f"Inference completed for job {job_id}: saved as {filename}")


# Global singleton
tryon_service = VirtualTryOnService()
