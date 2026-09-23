package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.model.AppConfig
import com.example.model.ClothingCategory
import com.example.model.TryOnRequest
import com.example.model.TryOnUiState
import com.example.model.UploadedImage
import com.example.service.VirtualTryOnService
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TryOnViewModel(application: Application) : AndroidViewModel(application) {

    private val _config = MutableStateFlow(AppConfig(demoMode = true))
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    private val tryOnService = VirtualTryOnService(application) { _config.value }

    private val _userImage = MutableStateFlow<UploadedImage?>(null)
    val userImage: StateFlow<UploadedImage?> = _userImage.asStateFlow()

    private val _garmentImage = MutableStateFlow<UploadedImage?>(null)
    val garmentImage: StateFlow<UploadedImage?> = _garmentImage.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ClothingCategory.BLAZERS)
    val selectedCategory: StateFlow<ClothingCategory> = _selectedCategory.asStateFlow()

    private val _uiState = MutableStateFlow<TryOnUiState>(TryOnUiState.Idle)
    val uiState: StateFlow<TryOnUiState> = _uiState.asStateFlow()

    // Flag for initial preset load
    private val _hasLoadedPreset = MutableStateFlow(false)
    val hasLoadedPreset: StateFlow<Boolean> = _hasLoadedPreset.asStateFlow()

    fun setUserImageFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val analyzed = ImageUtils.analyzeUri(getApplication(), uri)
            _userImage.value = analyzed
        }
    }

    fun setUserPreset(drawableResId: Int) {
        _userImage.value = UploadedImage(
            drawableResId = drawableResId,
            fileName = "sample_model.jpg",
            width = 768,
            height = 1024,
            fileSizeBytes = 380 * 1024,
            mimeType = "image/jpeg",
            isValid = true,
            qualityIssues = emptyList()
        )
    }

    fun removeUserImage() {
        _userImage.value = null
    }

    fun setGarmentImageFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val analyzed = ImageUtils.analyzeUri(getApplication(), uri)
            _garmentImage.value = analyzed
        }
    }

    fun setGarmentPreset(drawableResId: Int) {
        _garmentImage.value = UploadedImage(
            drawableResId = drawableResId,
            fileName = "sample_garment.jpg",
            width = 768,
            height = 1024,
            fileSizeBytes = 320 * 1024,
            mimeType = "image/jpeg",
            isValid = true,
            qualityIssues = emptyList()
        )
    }

    fun removeGarmentImage() {
        _garmentImage.value = null
    }

    fun setCategory(category: ClothingCategory) {
        _selectedCategory.value = category
    }

    fun loadSamplePreset() {
        setUserPreset(R.drawable.img_sample_model)
        setGarmentPreset(R.drawable.img_sample_garment)
        _hasLoadedPreset.value = true
    }

    fun startTryOn() {
        val person = _userImage.value
        val garment = _garmentImage.value

        if (person == null) {
            _uiState.value = TryOnUiState.Error("Please upload your photo first.")
            return
        }

        if (garment == null) {
            _uiState.value = TryOnUiState.Error("Please upload an outfit image.")
            return
        }

        if (!person.isValid || !garment.isValid) {
            _uiState.value = TryOnUiState.Error("Please upload a clear, valid image.")
            return
        }

        _uiState.value = TryOnUiState.Validating

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = TryOnRequest(
                    personImage = person,
                    garmentImage = garment,
                    category = _selectedCategory.value
                )

                val result = tryOnService.processTryOn(request) { stepIndex, message, progress ->
                    _uiState.value = TryOnUiState.Processing(
                        stepIndex = stepIndex,
                        totalSteps = 5,
                        currentMessage = message,
                        progress = progress
                    )
                }

                _uiState.value = TryOnUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = TryOnUiState.Error(
                    message = e.message ?: "Something went wrong while generating your result. Please try again."
                )
            }
        }
    }

    fun tryAnotherOutfit(keepPerson: Boolean = true) {
        if (!keepPerson) {
            _userImage.value = null
        }
        _garmentImage.value = null
        _uiState.value = TryOnUiState.Idle
    }

    fun dismissError() {
        _uiState.value = TryOnUiState.Idle
    }

    fun updateConfig(newConfig: AppConfig) {
        _config.value = newConfig
    }
}
