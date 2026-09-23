"""
Unit test suite for Phase 3 CatVTON integration.
Covers:
1. VTON_PROVIDER=demo selects DemoVtonProvider
2. VTON_PROVIDER=catvton selects CatVTONProvider
3. CatVTON worker returns success -> Job completed
4. CatVTON worker unavailable -> Auto-fallback to demo succeeds
5. CatVTON worker timeout -> Graceful fallback
6. Invalid image -> Clean 4xx error
7. Worker returns malformed response -> Backend handles gracefully without crash
"""
import asyncio
import io
import pytest
from unittest.mock import AsyncMock, patch
from PIL import Image
from httpx import AsyncClient, ASGITransport

from backend.main import app
from backend.config import Settings
from backend.services.virtual_tryon import VirtualTryOnService, TryOnJob
from backend.services.demo_provider import DemoVtonProvider
from backend.services.catvton_provider import CatVTONProvider
from backend.services.catvton_worker_client import (
    CatVTONWorkerClient,
    WorkerConnectionError,
    WorkerTimeoutError,
    WorkerMalformedResponseError,
)
from backend.models.schemas import JobStatus


def create_sample_jpeg_bytes(width: int = 300, height: int = 400, color: tuple = (100, 150, 200)) -> bytes:
    buf = io.BytesIO()
    img = Image.new("RGB", (width, height), color=color)
    img.save(buf, format="JPEG")
    return buf.getvalue()


def test_01_provider_selection_demo():
    """Test 1: When VTON_PROVIDER=demo, DemoVtonProvider is selected."""
    demo_settings = Settings(demo_mode=True, vton_provider="demo")
    with patch("backend.services.virtual_tryon.get_settings", return_value=demo_settings):
        service = VirtualTryOnService()
        provider = service.get_provider()
        assert isinstance(provider, DemoVtonProvider)
        assert provider.provider_id == "DEMO_SIMULATOR"


def test_02_provider_selection_catvton():
    """Test 2: When VTON_PROVIDER=catvton and DEMO_MODE=False, CatVTONProvider is selected."""
    catvton_settings = Settings(demo_mode=False, vton_provider="catvton", catvton_worker_url="https://gpu-worker.example.com")
    with patch("backend.services.virtual_tryon.get_settings", return_value=catvton_settings):
        service = VirtualTryOnService()
        provider = service.get_provider()
        assert isinstance(provider, CatVTONProvider)
        assert provider.provider_id == "CATVTON_GPU"


@pytest.mark.asyncio
async def test_03_catvton_worker_success():
    """Test 3: External CatVTON worker returns success -> Job becomes completed."""
    dummy_jpeg = create_sample_jpeg_bytes(width=200, height=300)

    mock_client = AsyncMock(spec=CatVTONWorkerClient)
    mock_client.is_configured.return_value = True
    mock_client.submit_job.return_value = "worker_job_999"
    mock_client.poll_until_complete.return_value = dummy_jpeg

    catvton_provider = CatVTONProvider(worker_client=mock_client)

    service = VirtualTryOnService()
    service._catvton_provider = catvton_provider

    catvton_settings = Settings(
        demo_mode=False,
        vton_provider="catvton",
        catvton_worker_url="https://gpu-worker.example.com",
        auto_fallback_to_demo=False
    )

    with patch("backend.services.virtual_tryon.get_settings", return_value=catvton_settings):
        # Save temp input files
        from backend.services.storage import storage_service
        img = Image.new("RGB", (100, 100))
        _, p_path = storage_service.save_image(img, "uploads", "p")
        _, g_path = storage_service.save_image(img, "uploads", "g")

        job_id = service.submit_job(p_path, g_path, "blazers", "upper")
        # Wait for async background task
        await asyncio.sleep(0.3)

        job = service.get_job(job_id)
        assert job is not None
        assert job.status == JobStatus.COMPLETED
        assert job.result_filename is not None
        assert "catvton" in job.result_filename


@pytest.mark.asyncio
async def test_04_catvton_worker_unavailable_auto_fallback():
    """Test 4: When CatVTON worker is unavailable and AUTO_FALLBACK_TO_DEMO=true, fallback to Demo succeeds."""
    mock_client = AsyncMock(spec=CatVTONWorkerClient)
    mock_client.is_configured.return_value = True
    mock_client.submit_job.side_effect = WorkerConnectionError("Connection refused by GPU worker")

    catvton_provider = CatVTONProvider(worker_client=mock_client)

    service = VirtualTryOnService()
    service._catvton_provider = catvton_provider

    fallback_settings = Settings(
        demo_mode=False,
        vton_provider="catvton",
        catvton_worker_url="https://offline-worker.example.com",
        auto_fallback_to_demo=True
    )

    with patch("backend.services.virtual_tryon.get_settings", return_value=fallback_settings):
        from backend.services.storage import storage_service
        img = Image.new("RGB", (100, 100))
        _, p_path = storage_service.save_image(img, "uploads", "p")
        _, g_path = storage_service.save_image(img, "uploads", "g")

        job_id = service.submit_job(p_path, g_path, "blazers", "upper")
        # Allow fallback execution
        await asyncio.sleep(3.5)

        job = service.get_job(job_id)
        assert job is not None
        assert job.status == JobStatus.COMPLETED
        assert job.result_filename is not None
        assert "demo" in job.result_filename


@pytest.mark.asyncio
async def test_05_catvton_worker_timeout_fallback():
    """Test 5: When CatVTON worker times out, auto-fallback executes gracefully."""
    mock_client = AsyncMock(spec=CatVTONWorkerClient)
    mock_client.is_configured.return_value = True
    mock_client.submit_job.return_value = "worker_job_slow"
    mock_client.poll_until_complete.side_effect = WorkerTimeoutError("Inference timed out after 120s")

    catvton_provider = CatVTONProvider(worker_client=mock_client)

    service = VirtualTryOnService()
    service._catvton_provider = catvton_provider

    timeout_settings = Settings(
        demo_mode=False,
        vton_provider="catvton",
        catvton_worker_url="https://slow-worker.example.com",
        auto_fallback_to_demo=True
    )

    with patch("backend.services.virtual_tryon.get_settings", return_value=timeout_settings):
        from backend.services.storage import storage_service
        img = Image.new("RGB", (100, 100))
        _, p_path = storage_service.save_image(img, "uploads", "p")
        _, g_path = storage_service.save_image(img, "uploads", "g")

        job_id = service.submit_job(p_path, g_path, "blazers", "upper")
        await asyncio.sleep(3.5)

        job = service.get_job(job_id)
        assert job is not None
        assert job.status == JobStatus.COMPLETED
        assert "demo" in job.result_filename


@pytest.mark.asyncio
async def test_06_invalid_image_upload_400():
    """Test 6: Invalid non-image upload returns clean 400 without crashing."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        files = {
            "person_image": ("bad.txt", b"plain text payload", "text/plain"),
            "garment_image": ("garment.jpg", create_sample_jpeg_bytes(), "image/jpeg"),
        }
        resp = await client.post("/api/try-on", files=files)
        assert resp.status_code == 400
        data = resp.json()
        assert data["success"] is False
        assert "error" in data
        assert "INVALID_MIME_TYPE" in data["error"]["code"]


@pytest.mark.asyncio
async def test_07_worker_malformed_response_handling():
    """Test 7: When worker returns malformed response, server handles it safely without crashing."""
    mock_client = AsyncMock(spec=CatVTONWorkerClient)
    mock_client.is_configured.return_value = True
    mock_client.submit_job.side_effect = WorkerMalformedResponseError("Invalid HTML returned instead of JSON")

    catvton_provider = CatVTONProvider(worker_client=mock_client)

    service = VirtualTryOnService()
    service._catvton_provider = catvton_provider

    no_fallback_settings = Settings(
        demo_mode=False,
        vton_provider="catvton",
        catvton_worker_url="https://corrupt-worker.example.com",
        auto_fallback_to_demo=False
    )

    with patch("backend.services.virtual_tryon.get_settings", return_value=no_fallback_settings):
        from backend.services.storage import storage_service
        img = Image.new("RGB", (100, 100))
        _, p_path = storage_service.save_image(img, "uploads", "p")
        _, g_path = storage_service.save_image(img, "uploads", "g")

        job_id = service.submit_job(p_path, g_path, "blazers", "upper")
        await asyncio.sleep(0.3)

        job = service.get_job(job_id)
        assert job is not None
        assert job.status == JobStatus.FAILED
        assert job.error is not None
        # Verify stack traces are not leaked in error message
        assert "Traceback" not in job.error


@pytest.mark.asyncio
async def test_08_e2e_backend_to_worker_contract():
    """Test 8: Backend CatVTONWorkerClient end-to-end contract with worker_server.app."""
    from worker.worker_server import app as worker_app
    from backend.services.storage import storage_service

    worker_transport = ASGITransport(app=worker_app)
    
    # Subclass client to route through worker ASGI transport
    class TestCatVTONWorkerClient(CatVTONWorkerClient):
        async def submit_job(self, person_image_path: str, garment_image_path: str, category: str = "blazers", garment_type: str = "upper") -> str:
            async with AsyncClient(transport=worker_transport, base_url="http://worker") as client:
                with open(person_image_path, "rb") as pf, open(garment_image_path, "rb") as gf:
                    files = {
                        "person_image": ("person.jpg", pf.read(), "image/jpeg"),
                        "garment_image": ("garment.jpg", gf.read(), "image/jpeg"),
                    }
                    data = {"category": category, "garment_type": garment_type}
                    resp = await client.post("/v1/try-on", files=files, data=data)
                    return resp.json()["job_id"]

        async def get_status(self, worker_job_id: str) -> dict:
            async with AsyncClient(transport=worker_transport, base_url="http://worker") as client:
                resp = await client.get(f"/v1/try-on/status/{worker_job_id}")
                return resp.json()

        async def poll_until_complete(self, worker_job_id: str, on_progress=None) -> bytes:
            for _ in range(25):
                await asyncio.sleep(0.1)
                st = await self.get_status(worker_job_id)
                if st.get("status") == "completed":
                    raw_url = st.get("result_url")
                    async with AsyncClient(transport=worker_transport, base_url="http://worker") as client:
                        dl_resp = await client.get(raw_url)
                        return dl_resp.content
            raise TimeoutError("Test timeout")

    worker_client = TestCatVTONWorkerClient(worker_url="http://worker")
    catvton_provider = CatVTONProvider(worker_client=worker_client)

    service = VirtualTryOnService()
    service._catvton_provider = catvton_provider

    settings = Settings(
        demo_mode=False,
        vton_provider="catvton",
        catvton_worker_url="http://worker",
        auto_fallback_to_demo=False
    )

    with patch("backend.services.virtual_tryon.get_settings", return_value=settings):
        img = Image.new("RGB", (100, 100), color=(100, 200, 100))
        _, p_path = storage_service.save_image(img, "uploads", "p")
        _, g_path = storage_service.save_image(img, "uploads", "g")

        job_id = service.submit_job(p_path, g_path, "blazers", "upper")
        await asyncio.sleep(0.5)

        job = service.get_job(job_id)
        assert job is not None
        assert job.status == JobStatus.COMPLETED
        assert job.result_filename is not None
        assert "catvton" in job.result_filename

