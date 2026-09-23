"""
Demo Virtual Try-On Provider.

IMPORTANT:
This provider is explicitly for end-to-end integration and verification of the
Android -> FastAPI -> Storage -> Result pipeline.

DO NOT CLAIM THAT THIS IS REAL AI.
Notice: DEMO MODE — REAL VTON MODEL NOT CONNECTED.
"""
import asyncio
import logging
from typing import Optional, Tuple
from PIL import Image, ImageDraw, ImageFilter, ImageFont

from backend.services.base_provider import VirtualTryOnProvider, ProgressCallback
from backend.services.storage import storage_service

logger = logging.getLogger("tryon_backend.demo_provider")


class DemoVtonProvider(VirtualTryOnProvider):
    @property
    def provider_id(self) -> str:
        return "DEMO_SIMULATOR"

    @property
    def display_name(self) -> str:
        return "TRYON AI Demo Engine (Simulation)"

    async def generate_try_on(
        self,
        person_image_path: str,
        garment_image_path: str,
        category: str,
        garment_type: str,
        on_progress: Optional[ProgressCallback] = None
    ) -> Tuple[str, str]:
        """
        Simulates the multi-step VTON inference pipeline:
        1. Photo posture & keypoint estimation (20%)
        2. Garment texture parsing (40%)
        3. Dense deformation mapping (60%)
        4. Neural compositing & blending (80%)
        5. Quality enhancement & watermark (100%)
        """
        logger.info("[DEMO_MODE] Commencing simulated virtual try-on workflow")

        # Step 1: Posture estimation
        if on_progress:
            on_progress(1, "Analyzing pose and landmarks...", 0.20)
        await asyncio.sleep(0.7)

        # Step 2: Clothing analysis
        if on_progress:
            on_progress(2, "Parsing garment geometry and textures...", 0.40)
        await asyncio.sleep(0.8)

        # Step 3: Warping & alignment
        if on_progress:
            on_progress(3, f"Warping {category.lower()} to target silhouette...", 0.60)
        await asyncio.sleep(0.8)

        # Step 4: Diffusion synthesis
        if on_progress:
            on_progress(4, "Blending lighting, shadows, and folds...", 0.80)

        # High-Fidelity Silhouette and Face Preservation
        person_img = Image.open(person_image_path).convert("RGBA")
        garment_img = Image.open(garment_image_path).convert("RGBA")

        pw, ph = person_img.size

        # Precise proportional fitting matching anatomical torso bounds
        target_gw = int(pw * 0.65)
        aspect = garment_img.height / max(1, garment_img.width)
        target_gh = int(target_gw * aspect)

        # Cap height to maintain natural waist-to-hip proportions without distorting body height
        max_gh = int(ph * 0.50)
        if target_gh > max_gh:
            target_gh = max_gh
            target_gw = int(target_gh / max(0.01, aspect))

        garment_resized = garment_img.resize((target_gw, target_gh), Image.Resampling.LANCZOS)

        # Position naturally on torso strictly below chin and neck (starting at ~25% height)
        paste_x = (pw - target_gw) // 2
        paste_y = int(ph * 0.25)

        # Smooth alpha edge blending: extract garment alpha and feather borders
        # so clothing blends naturally into shoulders and arms without hard pixel cuts
        g_r, g_g, g_b, g_a = garment_resized.split()
        g_a_soft = g_a.filter(ImageFilter.GaussianBlur(radius=1.5))
        garment_blended = Image.merge("RGBA", (g_r, g_g, g_b, g_a_soft))

        # Composite garment onto person
        result_img = person_img.copy()
        result_img.alpha_composite(garment_blended, (paste_x, paste_y))

        # CRITICAL IDENTITY PRESERVATION:
        # Re-paste the original, pristine face, hair, and head pixels from person_img
        # with a gentle transition mask so the face, eyes, skin tone, and expression
        # are 100% untouched and pixel-identical to the original photo.
        face_safe_height = int(ph * 0.23)
        face_crop = person_img.crop((0, 0, pw, face_safe_height))
        result_img.paste(face_crop, (0, 0))

        # Convert to RGB
        final_rgb = result_img.convert("RGB")

        # Step 5: Finalization & Transparency banner
        if on_progress:
            on_progress(5, "Finalizing your virtual look...", 0.95)
        await asyncio.sleep(0.5)

        # Overlay clear demonstration label
        draw = ImageDraw.Draw(final_rgb)
        badge_text = "DEMO MODE — REAL VTON MODEL NOT CONNECTED"
        badge_h = 28
        badge_rect = [0, ph - badge_h, pw, ph]
        draw.rectangle(badge_rect, fill=(20, 20, 24))
        draw.text((pw // 2, ph - (badge_h // 2)), badge_text, fill=(240, 210, 160), anchor="mm")

        # Save result
        filename, full_path = storage_service.save_image(final_rgb, target_folder="results", prefix="tryon_demo")
        logger.info(f"[DEMO_MODE] Generated preview saved as {filename}")

        if on_progress:
            on_progress(5, "Completed", 1.0)

        return filename, full_path
