"""
Client for communicating with an external GPU worker running CatVTON inference.
Handles job submission, status polling, result retrieval, error translation, and network timeouts.
"""
import asyncio
import logging
import os
import time
from typing import Optional
import httpx

from backend.config import get_settings
from backend.services.base_provider import ProgressCallback

logger = logging.getLogger("tryon_backend.catvton_client")


class CatVTONWorkerException(Exception):
    """Base exception for external GPU worker communication."""
    pass


class WorkerNotConfiguredError(CatVTONWorkerException):
    pass


class WorkerConnectionError(CatVTONWorkerException):
    pass


class WorkerTimeoutError(CatVTONWorkerException):
    pass


class WorkerInferenceError(CatVTONWorkerException):
    pass


class WorkerMalformedResponseError(CatVTONWorkerException):
    pass


class CatVTONWorkerClient:
    def __init__(
        self,
        worker_url: Optional[str] = None,
        api_key: Optional[str] = None,
        timeout_seconds: Optional[int] = None,
        poll_interval_seconds: Optional[float] = None
    ):
        settings = get_settings()
        self.worker_url = (worker_url or settings.catvton_worker_url or "").rstrip("/")
        self.api_key = api_key if api_key is not None else settings.catvton_worker_api_key
        self.timeout_seconds = timeout_seconds or settings.catvton_worker_timeout_seconds
        self.poll_interval = poll_interval_seconds or settings.catvton_worker_poll_interval_seconds

    def _get_headers(self) -> dict:
        headers = {}
        if self.api_key and self.api_key.strip():
            headers["Authorization"] = f"Bearer {self.api_key.strip()}"
            headers["X-API-Key"] = self.api_key.strip()
        return headers

    def is_configured(self) -> bool:
        return bool(self.worker_url and self.worker_url.strip())

    async def submit_job(
        self,
        person_image_path: str,
        garment_image_path: str,
        category: str = "blazers",
        garment_type: str = "upper"
    ) -> str:
        """
        Submits try-on images to the external GPU worker via multipart/form-data.
        Returns the worker's unique job_id.
        """
        if not self.is_configured():
            raise WorkerNotConfiguredError("CatVTON worker URL is not configured.")

        if not os.path.exists(person_image_path):
            raise WorkerConnectionError(f"Person image not found at {person_image_path}")
        if not os.path.exists(garment_image_path):
            raise WorkerConnectionError(f"Garment image not found at {garment_image_path}")

        submit_url = f"{self.worker_url}/v1/try-on"
        logger.info(f"Submitting job to GPU worker: {submit_url}")

        try:
            with open(person_image_path, "rb") as pf, open(garment_image_path, "rb") as gf:
                files = {
                    "person_image": ("person.jpg", pf.read(), "image/jpeg"),
                    "garment_image": ("garment.jpg", gf.read(), "image/jpeg"),
                }
                data = {
                    "category": category,
                    "garment_type": garment_type
                }

                async with httpx.AsyncClient(timeout=30.0) as client:
                    resp = await client.post(
                        submit_url,
                        files=files,
                        data=data,
                        headers=self._get_headers()
                    )

            if resp.status_code >= 400:
                logger.error(f"Worker rejected submission with HTTP {resp.status_code}: {resp.text[:200]}")
                raise WorkerConnectionError(f"Worker returned HTTP {resp.status_code}: {resp.text[:120]}")

            try:
                json_data = resp.json()
            except Exception as e:
                raise WorkerMalformedResponseError(f"Invalid JSON received from GPU worker: {str(e)}")

            worker_job_id = json_data.get("job_id")
            if not worker_job_id:
                raise WorkerMalformedResponseError("Worker response did not contain 'job_id'")

            logger.info(f"Job successfully queued on GPU worker with ID: {worker_job_id}")
            return worker_job_id

        except httpx.RequestError as exc:
            logger.error(f"Failed to connect to GPU worker at {submit_url}: {exc}")
            raise WorkerConnectionError(f"Unable to reach GPU worker: {str(exc)}")

    async def get_status(self, worker_job_id: str) -> dict:
        """Queries worker status for the given job_id."""
        status_url = f"{self.worker_url}/v1/try-on/status/{worker_job_id}"
        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                resp = await client.get(status_url, headers=self._get_headers())

            if resp.status_code >= 400:
                raise WorkerConnectionError(f"Worker returned HTTP {resp.status_code} during status poll")

            try:
                return resp.json()
            except Exception as e:
                raise WorkerMalformedResponseError(f"Worker returned malformed status response: {str(e)}")

        except httpx.RequestError as exc:
            raise WorkerConnectionError(f"Error querying worker status: {str(exc)}")

    async def poll_until_complete(
        self,
        worker_job_id: str,
        on_progress: Optional[ProgressCallback] = None
    ) -> bytes:
        """
        Polls the worker until inference finishes, reporting progress callbacks.
        Downloads and returns the completed result image bytes.
        """
        start_time = time.time()
        poll_count = 0

        while True:
            elapsed = time.time() - start_time
            if elapsed > self.timeout_seconds:
                logger.error(f"Worker inference timed out after {elapsed:.1f}s for job {worker_job_id}")
                raise WorkerTimeoutError(f"CatVTON GPU worker inference timed out ({self.timeout_seconds}s limit).")

            await asyncio.sleep(self.poll_interval)
            poll_count += 1

            status_payload = await self.get_status(worker_job_id)
            status = str(status_payload.get("status", "")).lower()
            msg = status_payload.get("message")

            # Map worker states into progress increments
            if status in ["queued", "starting"]:
                progress_val = 0.25
                default_msg = "GPU worker queuing CatVTON pipeline..."
            elif status in ["loading_model", "preprocessing"]:
                progress_val = 0.45
                default_msg = "GPU worker preprocessing garment & silhouette..."
            elif status in ["processing", "inferencing"]:
                progress_val = min(0.85, 0.50 + (poll_count * 0.05))
                default_msg = "CatVTON neural diffusion in progress on GPU..."
            elif status == "completed":
                progress_val = 1.0
                default_msg = "GPU inference complete. Downloading result..."
            elif status == "failed":
                err_detail = status_payload.get("error", "Worker indicated inference failure")
                raise WorkerInferenceError(f"GPU worker error: {err_detail}")
            else:
                progress_val = 0.50
                default_msg = "Processing with CatVTON..."

            if on_progress:
                on_progress(poll_count, msg or default_msg, progress_val)

            if status == "completed":
                raw_url = status_payload.get("result_url")
                if not raw_url:
                    raise WorkerMalformedResponseError("Worker marked completed but provided no 'result_url'")

                # Resolve download URL
                full_download_url = raw_url if raw_url.startswith("http") else f"{self.worker_url.rstrip('/')}/{raw_url.lstrip('/')}"
                logger.info(f"Downloading finished result from GPU worker: {full_download_url}")

                try:
                    async with httpx.AsyncClient(timeout=30.0) as client:
                        dl_resp = await client.get(full_download_url, headers=self._get_headers())
                    if dl_resp.status_code != 200:
                        raise WorkerConnectionError(f"Failed to download result image: HTTP {dl_resp.status_code}")
                    return dl_resp.content
                except httpx.RequestError as exc:
                    raise WorkerConnectionError(f"Network error downloading result image: {str(exc)}")
