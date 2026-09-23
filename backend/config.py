"""
Configuration module for the TRYON AI backend.
Loads settings from environment variables with sensible defaults.
"""
from functools import lru_cache
from typing import List
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_env: str = "development"
    host: str = "0.0.0.0"
    port: int = 8000
    max_image_size_mb: int = 10
    demo_mode: bool = True
    allowed_origins: str = "*"
    base_url: str = "http://10.0.2.2:8000"

    # Virtual Try-On Provider Configuration
    # Options: "demo", "catvton"
    vton_provider: str = "demo"

    # External GPU Worker Configuration (for CatVTON)
    catvton_worker_url: str = ""
    catvton_worker_api_key: str = ""
    catvton_worker_timeout_seconds: int = 120
    catvton_worker_poll_interval_seconds: float = 2.0

    # Failsafe: automatically fallback to Demo provider if GPU worker fails
    auto_fallback_to_demo: bool = True

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

    @property
    def max_image_bytes(self) -> int:
        return self.max_image_size_mb * 1024 * 1024

    @property
    def is_catvton_worker_configured(self) -> bool:
        return bool(self.catvton_worker_url and self.catvton_worker_url.strip())

    def get_allowed_origins_list(self) -> List[str]:
        if not self.allowed_origins or self.allowed_origins.strip() == "*":
            return ["*"]
        return [origin.strip() for origin in self.allowed_origins.split(",") if origin.strip()]


@lru_cache()
def get_settings() -> Settings:
    return Settings()
