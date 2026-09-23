# TRYON AI — CatVTON GPU Worker for Google Colab

This directory provides the complete GPU worker implementation for running **CatVTON (Concatenation Is All You Need for Virtual Try-On)** on an NVIDIA Tesla T4 GPU in Google Colab, exposing a secure HTTPS API via Cloudflare Tunnel for the TRYON AI FastAPI backend.

---

## Architecture

```
Android Application (Jetpack Compose)
       │
       ▼ POST /api/try-on
FastAPI Backend (VirtualTryOnService)
       │
       ▼ CatVTONProvider ➔ CatVTONWorkerClient
HTTPS Cloudflare Tunnel (https://*.trycloudflare.com)
       │
       ▼ FastAPI Worker Server (Port 8000)
Google Colab NVIDIA Tesla T4 (15–16 GB VRAM)
       │
       ▼ CatVTONPipeline (FP16 / PyTorch / Diffusers)
Generated Virtual Try-On Image (768x1024)
       │
       ▼ Downloaded by FastAPI Backend
Android ResultScreen (Displayed to User)
```

---

## ⚠️ Important Google Colab Environment Notice

> **Google Colab Free is a development/testing environment.**
> GPU availability, session duration (typically 4–12 hours), disconnects, and idle timeouts are controlled by Google.
> This worker is designed for **development, testing, and feasibility validation**. It is **not** a 24/7 production server.
> For commercial scale, transition to RunPod, Modal, Replicate, or a dedicated GCP A10G/T4 instance.

---

## Quick Start: Running on Google Colab

### Step 1: Open Notebook in Google Colab
1. Navigate to [Google Colab](https://colab.research.google.com).
2. Click **Upload** and upload `worker/catvton_colab_worker.ipynb`.

### Step 2: Select T4 GPU Runtime
1. In Colab, click **Runtime** in the top menu bar.
2. Select **Change runtime type**.
3. Under **Hardware accelerator**, select **T4 GPU**.
4. Click **Save**.

### Step 3: Run the Cells in Sequence
The notebook is organized into 10 structured cells:

| Cell | Purpose | Expected Output |
|------|---------|-----------------|
| **Cell 1** | Environment & GPU Check | Detects NVIDIA Tesla T4, 15+ GB VRAM, CUDA >= 12.0 |
| **Cell 2** | Install Dependencies & Cloudflared | Installs diffusers, transformers, accelerate, fastapi, uvicorn, cloudflared |
| **Cell 3** | Clone Official CatVTON Repo | Clones `Zheng-Chong/CatVTON` into `/content/CatVTON` |
| **Cell 4** | Download Model Weights | Downloads `zhengchong/CatVTON` from Hugging Face Hub |
| **Cell 5** | Verify Model Files | Confirms model checkpoints exist on disk |
| **Cell 6** | Load CatVTON Pipeline | Loads `CatVTONPipeline` in FP16 precision on GPU |
| **Cell 7** | Local Test Inference | Runs 1 trial inference and displays output in Colab |
| **Cell 8** | Start FastAPI Worker Server | Launches background worker server on `http://localhost:8000` |
| **Cell 9** | Start Cloudflare Tunnel | Exposes server via public `https://*.trycloudflare.com` URL |
| **Cell 10**| Display URL & Backend Config | Displays `.env` copy-paste block and verifies live HTTPS connectivity |

---

## Step 4: Connecting the Worker to the FastAPI Backend

Once Cell 10 runs, it prints your unique public tunnel URL and API Key:

```bash
# Example Output from Cell 10:
Worker Public URL : https://fashion-tryon-demo.trycloudflare.com
Worker API Key    : tryon_secret_9a4f21b7c8
```

Update your backend configuration file (`/backend/.env`):

```dotenv
# Provider selection: switch from demo to real neural model
VTON_PROVIDER=catvton
DEMO_MODE=false

# Worker Connection Details
CATVTON_WORKER_URL=https://fashion-tryon-demo.trycloudflare.com
CATVTON_WORKER_API_KEY=tryon_secret_9a4f21b7c8

# Timeout and Polling Configuration
CATVTON_WORKER_TIMEOUT_SECONDS=120
CATVTON_WORKER_POLL_INTERVAL_SECONDS=2.0

# Safe Auto-Fallback: if Colab sleeps or disconnects, gracefully return demo result
AUTO_FALLBACK_TO_DEMO=true
```

Restart your FastAPI backend:
```bash
python run_backend.py
```

---

## API Contract Reference

The GPU worker implements the following endpoints:

### 1. Health Check
- **Endpoint**: `GET /health`
- **Response**:
```json
{
  "status": "ok",
  "model": "catvton",
  "device": "cuda",
  "gpu": "Tesla T4",
  "vram_total_gb": 15.0,
  "pipeline_loaded": true
}
```

### 2. Submit Try-On Job
- **Endpoint**: `POST /v1/try-on`
- **Headers**: `Authorization: Bearer <API_KEY>` or `X-API-Key: <API_KEY>`
- **Body**: `multipart/form-data`
  - `person_image`: Target person portrait (JPEG/PNG/WebP)
  - `garment_image`: Garment image (JPEG/PNG/WebP)
  - `category`: e.g. `"blazers"`, `"t-shirts"`, `"dresses"`
  - `garment_type`: `"upper"`, `"lower"`, or `"overall"`
- **Response**:
```json
{
  "job_id": "worker_8a3f9e2b10cd",
  "status": "queued"
}
```

### 3. Check Job Status
- **Endpoint**: `GET /v1/try-on/status/{job_id}`
- **Headers**: `Authorization: Bearer <API_KEY>`
- **Response**:
```json
{
  "job_id": "worker_8a3f9e2b10cd",
  "status": "completed",
  "result_url": "/v1/try-on/result/worker_8a3f9e2b10cd",
  "message": "Generation complete."
}
```

### 4. Retrieve Try-On Image
- **Endpoint**: `GET /v1/try-on/result/{job_id}`
- **Headers**: `Authorization: Bearer <API_KEY>`
- **Response**: Binary image stream (`image/jpeg`)

---

## Testing Worker Contract Locally

Run the automated contract test suite to verify endpoint behavior:

```bash
PYTHONPATH=. pytest worker/test_worker_api.py -v
```
