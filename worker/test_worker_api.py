"""
Automated unit tests for worker_server.py.
Verifies API contract compatibility with FastAPI backend CatVTONWorkerClient.
"""
import asyncio
import io
import pytest
from httpx import AsyncClient, ASGITransport
from PIL import Image

from worker.worker_server import app, jobs_db, JobStatus


def generate_test_jpeg(width: int = 200, height: int = 300, color=(120, 160, 200)) -> bytes:
    buf = io.BytesIO()
    img = Image.new("RGB", (width, height), color=color)
    img.save(buf, format="JPEG")
    return buf.getvalue()


@pytest.mark.asyncio
async def test_worker_health_check():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.get("/health")
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "ok"
        assert data["model"] == "catvton"
        assert "device" in data


@pytest.mark.asyncio
async def test_worker_tryon_lifecycle():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        files = {
            "person_image": ("person.jpg", generate_test_jpeg(250, 350), "image/jpeg"),
            "garment_image": ("garment.jpg", generate_test_jpeg(250, 350, (220, 80, 80)), "image/jpeg"),
        }
        data = {
            "category": "blazers",
            "garment_type": "upper"
        }

        # 1. Submit job
        post_resp = await client.post("/v1/try-on", files=files, data=data)
        assert post_resp.status_code == 200
        post_data = post_resp.json()
        assert "job_id" in post_data
        assert post_data["status"] == "queued"
        job_id = post_data["job_id"]

        # 2. Poll until completed (simulation mode finishes quickly)
        completed = False
        for _ in range(25):
            await asyncio.sleep(0.1)
            status_resp = await client.get(f"/v1/try-on/status/{job_id}")
            assert status_resp.status_code == 200
            status_data = status_resp.json()
            if status_data["status"] == "completed":
                completed = True
                assert "result_url" in status_data
                break

        assert completed is True

        # 3. Download result
        result_resp = await client.get(f"/v1/try-on/result/{job_id}")
        assert result_resp.status_code == 200
        assert result_resp.headers["content-type"] == "image/jpeg"
        assert len(result_resp.content) > 0


@pytest.mark.asyncio
async def test_worker_invalid_mime_type():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        files = {
            "person_image": ("bad.txt", b"plain text", "text/plain"),
            "garment_image": ("garment.jpg", generate_test_jpeg(), "image/jpeg"),
        }
        resp = await client.post("/v1/try-on", files=files)
        assert resp.status_code == 400


@pytest.mark.asyncio
async def test_worker_path_traversal_protection():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        # Invalid pattern with traversal characters
        resp = await client.get("/v1/try-on/result/..%2F..%2Fetc%2Fpasswd")
        assert resp.status_code in [400, 404]
