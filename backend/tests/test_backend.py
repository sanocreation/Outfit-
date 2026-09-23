"""
Comprehensive backend test suite for TRYON AI.
Tests the 10 required scenarios:
1. GET /health
2. Invalid upload (non-image / corrupted)
3. Oversized upload
4. Valid person image validation
5. Valid garment image validation
6. POST /api/try-on
7. Job status check
8. Completed result retrieval
9. Failed generation handling
10. Invalid job ID (404)
"""
import io
import asyncio
import pytest
from PIL import Image
from httpx import AsyncClient, ASGITransport

from backend.main import app
from backend.services.virtual_tryon import tryon_service, TryOnJob
from backend.models.schemas import JobStatus
from backend.utils.image_utils import validate_image_bytes, ImageValidationError


def create_sample_jpeg_bytes(width: int = 400, height: int = 600, color: tuple = (120, 140, 180)) -> bytes:
    """Helper creating valid in-memory JPEG bytes."""
    buf = io.BytesIO()
    img = Image.new("RGB", (width, height), color=color)
    img.save(buf, format="JPEG", quality=85)
    return buf.getvalue()


@pytest.mark.asyncio
async def test_01_health_check():
    """1. Test GET /health endpoint."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"
        assert data["service"] == "TRYON AI Backend"
        assert data["demo_mode"] is True


@pytest.mark.asyncio
async def test_02_invalid_upload_corrupt_file():
    """2. Test upload with corrupted non-image content."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        files = {
            "person_image": ("person.jpg", b"This is not a real JPEG image file.", "image/jpeg"),
            "garment_image": ("garment.jpg", create_sample_jpeg_bytes(), "image/jpeg"),
        }
        data = {"category": "blazers"}
        response = await client.post("/api/try-on", files=files, data=data)
        assert response.status_code == 400
        result = response.json()
        assert result["success"] is False
        assert "error" in result
        assert "CORRUPTED_IMAGE" in result["error"]["code"]


@pytest.mark.asyncio
async def test_03_oversized_upload():
    """3. Test upload exceeding configured maximum size."""
    # Test validator with 1KB limit
    large_bytes = b"x" * 2048
    with pytest.raises(ImageValidationError) as exc:
        validate_image_bytes(
            file_bytes=large_bytes,
            filename="too_big.jpg",
            content_type="image/jpeg",
            max_size_bytes=1024
        )
    assert exc.value.code == "FILE_TOO_LARGE"


def test_04_valid_person_image():
    """4. Test validation of valid person image."""
    img_bytes = create_sample_jpeg_bytes(width=500, height=800)
    image, w, h = validate_image_bytes(
        file_bytes=img_bytes,
        filename="person.jpg",
        content_type="image/jpeg",
        max_size_bytes=10 * 1024 * 1024
    )
    assert image is not None
    assert w == 500
    assert h == 800


def test_05_valid_garment_image():
    """5. Test validation of valid garment image."""
    img_bytes = create_sample_jpeg_bytes(width=400, height=500, color=(50, 50, 60))
    image, w, h = validate_image_bytes(
        file_bytes=img_bytes,
        filename="garment.png",
        content_type="image/png",
        max_size_bytes=10 * 1024 * 1024
    )
    assert image is not None
    assert w == 400
    assert h == 500


@pytest.mark.asyncio
async def test_06_post_try_on_success():
    """6. Test successful POST /api/try-on job creation."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        files = {
            "person_image": ("person.jpg", create_sample_jpeg_bytes(width=300, height=450), "image/jpeg"),
            "garment_image": ("garment.jpg", create_sample_jpeg_bytes(width=300, height=400), "image/jpeg"),
        }
        data = {"category": "blazers", "garment_type": "upper"}
        response = await client.post("/api/try-on", files=files, data=data)
        assert response.status_code == 200
        result = response.json()
        assert result["success"] is True
        assert "job_id" in result
        assert result["status"] == "queued"


@pytest.mark.asyncio
async def test_07_job_status():
    """7. Test checking status of queued/processing job."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        files = {
            "person_image": ("person.jpg", create_sample_jpeg_bytes(width=200, height=300), "image/jpeg"),
            "garment_image": ("garment.jpg", create_sample_jpeg_bytes(width=200, height=250), "image/jpeg"),
        }
        create_resp = await client.post("/api/try-on", files=files)
        job_id = create_resp.json()["job_id"]

        status_resp = await client.get(f"/api/try-on/status/{job_id}")
        assert status_resp.status_code == 200
        data = status_resp.json()
        assert data["success"] is True
        assert data["job_id"] == job_id
        assert data["status"] in ["queued", "processing", "completed"]


@pytest.mark.asyncio
async def test_08_completed_result():
    """8. Test polling until completion and downloading the result image."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        files = {
            "person_image": ("person.jpg", create_sample_jpeg_bytes(width=200, height=300), "image/jpeg"),
            "garment_image": ("garment.jpg", create_sample_jpeg_bytes(width=200, height=250), "image/jpeg"),
        }
        create_resp = await client.post("/api/try-on", files=files)
        job_id = create_resp.json()["job_id"]

        # Poll until complete (demo mode takes ~3 seconds)
        for _ in range(15):
            await asyncio.sleep(0.5)
            status_resp = await client.get(f"/api/try-on/status/{job_id}")
            data = status_resp.json()
            if data["status"] == "completed":
                break

        assert data["status"] == "completed"
        assert data["result_url"] is not None

        # Fetch result image
        result_resp = await client.get(data["result_url"])
        assert result_resp.status_code == 200
        assert result_resp.headers["content-type"] == "image/jpeg"
        # Verify valid JPEG
        downloaded_img = Image.open(io.BytesIO(result_resp.content))
        assert downloaded_img.format == "JPEG"


@pytest.mark.asyncio
async def test_09_failed_generation_handling():
    """9. Test handling of job failure gracefully."""
    fake_job_id = "job_failed_test"
    failed_job = TryOnJob(
        job_id=fake_job_id,
        status=JobStatus.FAILED,
        error="Generation failed during virtual try-on inference.",
        message="Processing failed"
    )
    tryon_service._jobs[fake_job_id] = failed_job

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.get(f"/api/try-on/status/{fake_job_id}")
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "failed"
        assert data["error"] is not None


@pytest.mark.asyncio
async def test_10_invalid_job_id_404():
    """10. Test requesting status for non-existent job ID returns clean 404."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.get("/api/try-on/status/non_existent_job_12345")
        assert resp.status_code == 404
        data = resp.json()
        assert data["success"] is False
        assert data["error"]["code"] == "JOB_NOT_FOUND"
