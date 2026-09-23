package com.example.service

import android.content.Context
import com.example.model.AppConfig
import com.example.model.TryOnRequest
import com.example.model.TryOnResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

enum class JobStatus {
    QUEUED, PROCESSING, COMPLETED, FAILED
}

data class JobStatusResponse(
    val status: JobStatus,
    val progress: Float = 0f,
    val message: String = "",
    val resultUrl: String? = null
)

interface VtonProvider {
    val providerId: String
    val displayName: String
    val description: String

    suspend fun executeTryOn(
        context: Context,
        request: TryOnRequest,
        onProgress: (stepIndex: Int, message: String, progress: Float) -> Unit
    ): TryOnResult
}

/**
 * Central service managing AI Virtual Try-On inference providers.
 *
 * Designed with a provider-agnostic abstraction (VtonProvider), allowing the frontend
 * to remain completely decoupled from the specific AI engine (Demo local composite,
 * FastAPI remote GPU worker, CatVTON, IDM-VTON, etc.).
 */
class VirtualTryOnService(
    private val context: Context,
    private val getConfig: () -> AppConfig
) {
    private val demoProvider = DemoVtonProvider()
    private val remoteProvider = RemoteVtonProvider(
        getBaseUrl = { getConfig().backendUrl },
        getApiKey = { getConfig().apiKey }
    )

    val availableProviders: List<VtonProvider> = listOf(
        demoProvider,
        remoteProvider,
        CatVtonProvider(),
        IdmVtonProvider()
    )

    fun getActiveProvider(): VtonProvider {
        val config = getConfig()
        return if (config.demoMode) {
            demoProvider
        } else {
            when (config.selectedProvider) {
                "REMOTE_FASTAPI" -> remoteProvider
                "CAT_VTON" -> CatVtonProvider()
                "IDM_VTON" -> IdmVtonProvider()
                else -> demoProvider
            }
        }
    }

    suspend fun processTryOn(
        request: TryOnRequest,
        onProgress: (stepIndex: Int, message: String, progress: Float) -> Unit
    ): TryOnResult {
        val provider = getActiveProvider()
        return provider.executeTryOn(context, request, onProgress)
    }
}
