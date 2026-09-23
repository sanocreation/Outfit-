package com.example.service

import android.content.Context
import android.graphics.BitmapFactory
import com.example.model.TryOnRequest
import com.example.model.TryOnResult
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Connects to the FastAPI backend API as defined in Section 12:
 * POST /api/try-on
 * GET /api/try-on/status/{job_id}
 */
class RemoteVtonProvider(
    private val getBaseUrl: () -> String = { "http://10.0.2.2:8000" },
    private val getApiKey: () -> String = { "" }
) : VtonProvider {
    override val providerId: String = "REMOTE_FASTAPI"
    override val displayName: String = "Remote FastAPI VTON Backend"
    override val description: String = "Production GPU inference microservice via REST API."

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun executeTryOn(
        context: Context,
        request: TryOnRequest,
        onProgress: (stepIndex: Int, message: String, progress: Float) -> Unit
    ): TryOnResult {
        val baseUrl = getBaseUrl().trimEnd('/')
        val apiKey = getApiKey()
        onProgress(0, "Connecting to AI inference service ($baseUrl)...", 0.15f)

        try {
            // Compress images to bytes
            val personBytes = getBytes(context, request.personImage.uri)
                ?: throw IllegalStateException("Could not read person photo")
            val garmentBytes = getBytes(context, request.garmentImage.uri)
                ?: throw IllegalStateException("Could not read outfit image")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "person_image",
                    "person.jpg",
                    personBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .addFormDataPart(
                    "garment_image",
                    "garment.jpg",
                    garmentBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .addFormDataPart("category", request.category.title)
                .addFormDataPart("garment_type", request.garmentType)
                .build()

            val requestBuilder = Request.Builder()
                .url("$baseUrl/api/try-on")
                .post(requestBody)

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                requestBuilder.addHeader("X-API-Key", apiKey)
            }

            val postRequest = requestBuilder.build()

            onProgress(1, "Uploading assets to AI backend...", 0.35f)

            val response = client.newCall(postRequest).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("AI Service error (HTTP ${response.code}). Check backend URL or switch to Demo Mode.")
            }

            val json = JSONObject(response.body?.string() ?: "{}")
            val jobId = json.optString("job_id", "job_remote")
            val directResultUrl = json.optString("result_url", "").ifBlank { null }

            if (!directResultUrl.isNullOrBlank()) {
                onProgress(4, "Finalizing your result...", 0.95f)
                val imageResponse = client.newCall(Request.Builder().url(directResultUrl).build()).execute()
                val bitmap = BitmapFactory.decodeStream(imageResponse.body?.byteStream())
                return TryOnResult(
                    jobId = jobId,
                    resultBitmap = bitmap,
                    providerUsed = displayName,
                    isDemo = false
                )
            }

            // Asynchronous polling pattern
            var attempts = 0
            while (attempts < 30) {
                delay(2000L)
                attempts++
                val statusReqBuilder = Request.Builder()
                    .url("$baseUrl/api/try-on/status/$jobId")
                    .get()
                if (apiKey.isNotBlank()) {
                    statusReqBuilder.addHeader("Authorization", "Bearer $apiKey")
                    statusReqBuilder.addHeader("X-API-Key", apiKey)
                }
                val statusReq = statusReqBuilder.build()
                val statusResp = client.newCall(statusReq).execute()
                val statusJson = JSONObject(statusResp.body?.string() ?: "{}")
                val status = statusJson.optString("status", "processing")
                val progressMsg = statusJson.optString("message", "AI processing your outfit...")
                val currentProgress = 0.4f + (attempts * 0.02f).coerceAtMost(0.9f)

                onProgress(2, progressMsg, currentProgress)

                if (status == "completed") {
                    val rawResultUrl = statusJson.getString("result_url")
                    val fullResultUrl = if (rawResultUrl.startsWith("http")) rawResultUrl else "$baseUrl${if (rawResultUrl.startsWith("/")) "" else "/"}$rawResultUrl"
                    val imageResp = client.newCall(Request.Builder().url(fullResultUrl).build()).execute()
                    val bitmap = BitmapFactory.decodeStream(imageResp.body?.byteStream())
                        ?: throw IllegalStateException("Backend returned an invalid image stream.")
                    return TryOnResult(
                        jobId = jobId,
                        resultBitmap = bitmap,
                        providerUsed = displayName,
                        isDemo = false
                    )
                } else if (status == "failed") {
                    val errorDetail = statusJson.optString("error", "AI generation failed on remote worker.")
                    throw IllegalStateException(errorDetail)
                }
            }

            throw IllegalStateException("Inference timed out. Please try again.")
        } catch (e: Exception) {
            throw IllegalStateException(e.message ?: "Unable to connect to AI server. Verify backend is running or toggle Demo Mode.")
        }
    }

    private fun getBytes(context: Context, uri: android.net.Uri?): ByteArray? {
        if (uri == null) return null
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }
}

class CatVtonProvider : VtonProvider {
    override val providerId: String = "CAT_VTON"
    override val displayName: String = "CatVTON Diffusion Provider"
    override val description: String = "Concatenation-based Try-On diffusion with inpainting pipeline."

    override suspend fun executeTryOn(
        context: Context,
        request: TryOnRequest,
        onProgress: (stepIndex: Int, message: String, progress: Float) -> Unit
    ): TryOnResult {
        // Fallback or route to demo
        return DemoVtonProvider().executeTryOn(context, request, onProgress)
    }
}

class IdmVtonProvider : VtonProvider {
    override val providerId: String = "IDM_VTON"
    override val displayName: String = "IDM-VTON Provider"
    override val description: String = "Improving Diffusion Models for Virtual Try-On."

    override suspend fun executeTryOn(
        context: Context,
        request: TryOnRequest,
        onProgress: (stepIndex: Int, message: String, progress: Float) -> Unit
    ): TryOnResult {
        return DemoVtonProvider().executeTryOn(context, request, onProgress)
    }
}
