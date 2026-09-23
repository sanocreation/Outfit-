package com.example.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * Represents an image uploaded by the user (or selected from presets).
 */
data class UploadedImage(
    val uri: Uri? = null,
    val drawableResId: Int? = null,
    val bitmap: Bitmap? = null,
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "image/jpeg",
    val isValid: Boolean = true,
    val qualityIssues: List<QualityIssue> = emptyList()
) {
    val displaySize: String
        get() {
            return if (fileSizeBytes < 1024 * 1024) {
                "${fileSizeBytes / 1024} KB"
            } else {
                String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0))
            }
        }
}

enum class QualityIssue(val description: String) {
    LOW_RESOLUTION("Image resolution is low (< 500px). Results may be soft."),
    UNUSUAL_ASPECT_RATIO("Unusual aspect ratio. Standard portrait (3:4) is recommended."),
    FILE_TOO_LARGE("File size exceeds 10 MB."),
    NON_STANDARD_FORMAT("Non-standard format. JPG, PNG, and WEBP work best.")
}

enum class ClothingCategory(val title: String) {
    ALL("All Categories"),
    TOPS("Tops & Blouses"),
    BLAZERS("Blazers & Jackets"),
    DRESSES("Dresses"),
    OUTERWEAR("Outerwear")
}

data class TryOnRequest(
    val personImage: UploadedImage,
    val garmentImage: UploadedImage,
    val category: ClothingCategory = ClothingCategory.BLAZERS,
    val garmentType: String = "upper_body"
)

data class TryOnResult(
    val jobId: String,
    val resultDrawableResId: Int? = null,
    val resultBitmap: Bitmap? = null,
    val providerUsed: String = "TRYON AI Studio Demo Engine",
    val isDemo: Boolean = true,
    val processingTimeMs: Long = 2400L,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class TryOnUiState {
    object Idle : TryOnUiState()
    object Validating : TryOnUiState()
    data class Processing(
        val stepIndex: Int,
        val totalSteps: Int = 5,
        val currentMessage: String,
        val progress: Float
    ) : TryOnUiState()
    data class Success(val result: TryOnResult) : TryOnUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : TryOnUiState()
}

data class AppConfig(
    val demoMode: Boolean = true,
    val backendUrl: String = "http://10.0.2.2:8000",
    val selectedProvider: String = "DEMO_VTON",
    val apiKey: String = ""
)
