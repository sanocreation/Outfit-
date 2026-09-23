"""
Remote AI Virtual Try-On Provider (Phase 3 Scaffold).
Prepares the interface for external VTON clusters (CatVTON, IDM-VTON, Replicate, RunPod).
"""
from typing import Optional, Tuple
from backend.services.base_provider import VirtualTryOnProvider, ProgressCallback


class RemoteVtonProvider(VirtualTryOnProvider):
    def __init__(self, api_url: str = "", api_key: str = ""):
        self.api_url = api_url
        self.api_key = api_key

    @property
    def provider_id(self) -> str:
        return "REMOTE_VTON_CLUSTER"

    @property
    def display_name(self) -> str:
        return "Remote Neural VTON Cluster"

    async def generate_try_on(
        self,
        person_image_path: str,
        garment_image_path: str,
        category: str,
        garment_type: str,
        on_progress: Optional[ProgressCallback] = None
    ) -> Tuple[str, str]:
        raise NotImplementedError(
            "Real AI VTON models (CatVTON / IDM-VTON) are reserved for Phase 3. "
            "Currently operating in Demo Mode."
        )
