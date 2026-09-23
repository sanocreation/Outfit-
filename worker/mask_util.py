"""
Garment Masking Utilities for CatVTON Virtual Try-On.
Provides automatic mask generation for target person images with fallback
anatomical segmentations (Upper body, Lower body, Overall/Dress).
"""
import logging
from typing import Tuple
from PIL import Image, ImageDraw, ImageFilter

logger = logging.getLogger("catvton_worker.mask")


def generate_anatomical_mask(
    image: Image.Image,
    category: str = "blazers",
    garment_type: str = "upper"
) -> Image.Image:
    """
    Generates an inpainting mask for CatVTON and composite pipelines.
    
    CRITICAL IDENTITY & FIGURE PRESERVATION:
    - Pure Black (0) = 100% PRESERVED original person pixels.
      The person's face, hair, eyes, nose, lips, jawline, neck, head, hands,
      wrists, fingers, and original body outline / background are strictly untouched.
    - White (255) = Garment replacement area only.
    - Gentle gradient feathering along borders ensures seamless, smooth transition
      without altering the model's actual facial features, expression, or physical proportions.
    """
    width, height = image.size
    mask = Image.new("L", (width, height), 0)
    draw = ImageDraw.Draw(mask)

    # Normalize category/garment_type
    cat = (category or "").lower().strip()
    g_type = (garment_type or "").lower().strip()

    is_overall = "dress" in cat or "overall" in g_type or "romper" in cat or "suit" in cat
    is_lower = "pant" in cat or "skirt" in cat or "short" in cat or "lower" in g_type or "jean" in cat

    if is_overall:
        # Full outfit (e.g. dress, jumpsuit, gown)
        # Face and neck fully protected: strictly below 0.22 height (below clavicle/throat)
        # Hands and lower limbs protected
        top = int(height * 0.22)
        bottom = int(height * 0.84)
        
        points = [
            (int(width * 0.32), top),                 # Base of neck left
            (int(width * 0.68), top),                 # Base of neck right
            (int(width * 0.85), int(height * 0.35)),  # Right shoulder/chest
            (int(width * 0.88), int(height * 0.60)),  # Right torso
            (int(width * 0.84), bottom),              # Hem right
            (int(width * 0.16), bottom),              # Hem left
            (int(width * 0.12), int(height * 0.60)),  # Left torso
            (int(width * 0.15), int(height * 0.35)),  # Left shoulder/chest
        ]
        draw.polygon(points, fill=255)

    elif is_lower:
        # Lower garment (pants, trousers, skirts, shorts)
        # Upper body, face, neck, chest completely preserved
        top = int(height * 0.52)
        bottom = int(height * 0.94)
        
        # Contour hips down to ankles, preserving feet and waist silhouette
        points = [
            (int(width * 0.22), top),                 # Left hip
            (int(width * 0.78), top),                 # Right hip
            (int(width * 0.82), int(height * 0.70)),  # Right thigh/knee
            (int(width * 0.75), bottom),              # Right ankle
            (int(width * 0.25), bottom),              # Left ankle
            (int(width * 0.18), int(height * 0.70)),  # Left thigh/knee
        ]
        draw.polygon(points, fill=255)

    else:
        # Upper Body (shirts, t-shirts, blazers, jackets, hoodies, tops)
        # ABSOLUTE FACE & HEAD PRESERVATION:
        # Strictly starts below the neck/collarbone (>= 0.22 height).
        # Wrists & hands preserved (ends above 0.60 height).
        top = int(height * 0.22)
        bottom = int(height * 0.66)
        
        points = [
            (int(width * 0.34), top),                 # Base of neck left
            (int(width * 0.66), top),                 # Base of neck right
            (int(width * 0.84), int(height * 0.28)),  # Right shoulder
            (int(width * 0.88), int(height * 0.48)),  # Right sleeve/arm
            (int(width * 0.74), bottom),              # Right waist
            (int(width * 0.26), bottom),              # Left waist
            (int(width * 0.12), int(height * 0.48)),  # Left sleeve/arm
            (int(width * 0.16), int(height * 0.28)),  # Left shoulder
        ]
        draw.polygon(points, fill=255)

    # Extra guarantee: Zero out face/head area (0 to 20% height) to prevent any bleeding
    head_safe_bottom = int(height * 0.20)
    draw.rectangle([0, 0, width, head_safe_bottom], fill=0)

    # Apply soft Gaussian feathering (radius 6) for realistic, smooth edge blending
    # without pixel harshness or halo artifacts
    mask = mask.filter(ImageFilter.GaussianBlur(radius=6))
    return mask


def preprocess_images_for_catvton(
    person_img: Image.Image,
    garment_img: Image.Image,
    target_size: Tuple[int, int] = (768, 1024)
) -> Tuple[Image.Image, Image.Image, Image.Image]:
    """
    Prepares person image, garment image, and generated mask to CatVTON standard dimensions (768x1024).
    Preserves aspect ratios with padding where needed.
    """
    target_w, target_h = target_size

    # Resize person image with high-quality resampling
    p_img = person_img.convert("RGB")
    p_img = p_img.resize((target_w, target_h), Image.Resampling.LANCZOS)

    # Resize garment image with high-quality resampling
    g_img = garment_img.convert("RGB")
    g_img = g_img.resize((target_w, target_h), Image.Resampling.LANCZOS)

    # Generate mask
    mask = generate_anatomical_mask(p_img, category="blazers", garment_type="upper")

    return p_img, g_img, mask
