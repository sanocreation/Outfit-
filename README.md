# TRYON AI — Virtual Try-On Mobile App & Backend

A modern, full-stack Virtual Try-On (VTON) Android application and FastAPI backend powered by CatVTON (Diffusion-based Virtual Try-On, ICLR 2025).

---

## 📱 Features

- **Jetpack Compose Android App**: Modern Material 3 UI with Edge-to-Edge support, responsive layout, smooth transitions, and live generation progress.
- **FastAPI Backend**: Asynchronous job handling, storage management, image validation, and provider auto-fallback.
- **CatVTON GPU Worker**: Dedicated GPU server and Google Colab Jupyter Notebook (`worker/catvton_colab_worker.ipynb`) with Cloudflare HTTPS tunnel.
- **Identity & Figure Preservation**: Dedicated mask and face protection algorithms ensuring 100% preservation of facial features, skin tone, hair, and anatomical proportions.

---

## 🚀 Quick Start Guide

### 1. Android Application
1. Open this repository in **Android Studio**.
2. Sync the project with Gradle files.
3. Run the app on an Android device or emulator (Android 8.0+ / API 26+).

### 2. FastAPI Backend
```bash
cd backend
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
python -m uvicorn backend.main:app --host 0.0.0.0 --port 8000
```

### 3. GPU Worker (Google Colab)
1. Upload `worker/catvton_colab_worker.ipynb` to [Google Colab](https://colab.research.google.com).
2. Set runtime type to **T4 GPU**.
3. Run all cells to launch the CatVTON worker and get your public Cloudflare tunnel URL.
4. Add the URL to `backend/.env` under `CATVTON_WORKER_URL`.

---

## 📂 Project Structure

```
├── app/                  # Android Jetpack Compose application source code
├── backend/              # FastAPI application, providers, and REST API
├── worker/               # CatVTON Colab notebook, FastAPI worker, and mask utils
├── gradle/               # Gradle wrapper and version catalog
├── build.gradle.kts      # Root build configuration
└── settings.gradle.kts   # Project settings
```
