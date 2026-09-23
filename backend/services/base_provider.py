"""
Abstract base class definition for Virtual Try-On inference providers.
"""
from abc import ABC, abstractmethod
from typing import Callable, Optional, Tuple


ProgressCallback = Callable[[int, str, float], None]


class VirtualTryOnProvider(ABC):
    """
    Interface for Virtual Try-On engines.
    Allows seamlessly swapping between demo simulation, local models,
    and external GPU clusters (e.g. CatVTON, IDM-VTON) without altering the API layer.
    """

    @property
    @abstractmethod
    def provider_id(self) -> str:
        """Machine-readable provider identifier."""
        pass

    @property
    @abstractmethod
    def display_name(self) -> str:
        """Human-readable provider name."""
        pass

    @abstractmethod
    async def generate_try_on(
        self,
        person_image_path: str,
        garment_image_path: str,
        category: str,
        garment_type: str,
        on_progress: Optional[ProgressCallback] = None
    ) -> Tuple[str, str]:
        """
        Executes try-on inference.
        Returns:
            Tuple[result_filename, result_file_path]
        """
        pass
