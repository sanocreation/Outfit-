"""
Pydantic data models and schemas for TRYON AI API requests and responses.
"""
from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field


class JobStatus(str, Enum):
    QUEUED = "queued"
    PROCESSING = "processing"
    COMPLETED = "completed"
    FAILED = "failed"


class ErrorDetail(BaseModel):
    code: str = Field(..., description="Machine-readable error code")
    message: str = Field(..., description="Human-readable error description")


class ErrorResponse(BaseModel):
    success: bool = Field(False, description="Always false for error responses")
    error: ErrorDetail


class HealthResponse(BaseModel):
    status: str = Field("ok", description="Server status indicator")
    service: str = Field("TRYON AI Backend", description="Service identifier")
    demo_mode: bool = Field(True, description="Whether server is operating in demo mode")
    vton_provider: str = Field("demo", description="Active virtual try-on engine")
    catvton_worker_configured: bool = Field(False, description="Whether external GPU worker URL is set")


class TryOnJobCreateResponse(BaseModel):
    success: bool = Field(True, description="Whether the job creation was successful")
    job_id: str = Field(..., description="Unique job identifier")
    status: str = Field(JobStatus.QUEUED.value, description="Initial job status")


class TryOnStatusResponse(BaseModel):
    success: bool = Field(True, description="Request status")
    job_id: str = Field(..., description="Unique job identifier")
    status: str = Field(..., description="Current processing status")
    result_url: Optional[str] = Field(None, description="Download URL for the generated image once completed")
    message: Optional[str] = Field(None, description="Current progress status message")
    progress: Optional[float] = Field(None, description="Floating progress from 0.0 to 1.0")
    error: Optional[str] = Field(None, description="Error message if processing failed")
