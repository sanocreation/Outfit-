package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TryOnUiState
import com.example.ui.TryOnViewModel
import com.example.ui.components.HeroSection
import com.example.ui.components.HowItWorksSection
import com.example.ui.components.ImageQualityAssistant
import com.example.ui.components.ProcessingOverlay
import com.example.ui.components.ResultScreen
import com.example.ui.components.SettingsSheet
import com.example.ui.components.TryOnCtaBar
import com.example.ui.components.UploadStudioSection
import com.example.ui.theme.FashionBlack
import com.example.ui.theme.FashionBorder
import com.example.ui.theme.FashionError
import com.example.ui.theme.FashionErrorLight
import com.example.ui.theme.FashionOffWhite
import com.example.ui.theme.FashionWhite
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val viewModel: TryOnViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        TryOnApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun TryOnApp(viewModel: TryOnViewModel) {
  val userImage by viewModel.userImage.collectAsState()
  val garmentImage by viewModel.garmentImage.collectAsState()
  val selectedCategory by viewModel.selectedCategory.collectAsState()
  val uiState by viewModel.uiState.collectAsState()
  val config by viewModel.config.collectAsState()

  val scrollState = rememberScrollState()
  val coroutineScope = rememberCoroutineScope()
  var showSettings by remember { mutableStateOf(false) }

  val isReadyToGenerate = userImage != null && garmentImage != null &&
          userImage!!.isValid && garmentImage!!.isValid

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (uiState is TryOnUiState.Success) {
        // Result Screen
        val successState = uiState as TryOnUiState.Success
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
        ) {
          // Top Navigation Bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            IconButton(
              onClick = { viewModel.tryAnotherOutfit(keepPerson = true) },
              modifier = Modifier.testTag("back_to_studio_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Studio",
                tint = FashionBlack
              )
            }
            Text(
              text = "TRYON AI",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
              ),
              color = FashionBlack
            )
            IconButton(onClick = { showSettings = true }) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Settings",
                tint = FashionBlack
              )
            }
          }

          ResultScreen(
            result = successState.result,
            originalPersonImage = userImage,
            onTryAnotherOutfit = {
              viewModel.tryAnotherOutfit(keepPerson = true)
            }
          )
        }
      } else {
        // Main Home / Studio Feed
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Hero & Header
          HeroSection(
            config = config,
            onScrollToUpload = {
              coroutineScope.launch {
                scrollState.animateScrollTo(650)
              }
            },
            onLoadSamplePreset = {
              viewModel.loadSamplePreset()
              coroutineScope.launch {
                scrollState.animateScrollTo(650)
              }
            },
            onOpenSettings = { showSettings = true }
          )

          // Upload Studio Cards
          UploadStudioSection(
            userImage = userImage,
            garmentImage = garmentImage,
            selectedCategory = selectedCategory,
            onCategoryChange = { viewModel.setCategory(it) },
            onUserImageSelected = { viewModel.setUserImageFromUri(it) },
            onUserPresetSelected = { viewModel.setUserPreset(it) },
            onRemoveUserImage = { viewModel.removeUserImage() },
            onGarmentImageSelected = { viewModel.setGarmentImageFromUri(it) },
            onGarmentPresetSelected = { viewModel.setGarmentPreset(it) },
            onRemoveGarmentImage = { viewModel.removeGarmentImage() }
          )

          // Image Quality Assistant & Warning Banner
          ImageQualityAssistant(
            userImage = userImage,
            garmentImage = garmentImage
          )

          // Error Banner if in Error State
          if (uiState is TryOnUiState.Error) {
            val errorMsg = (uiState as TryOnUiState.Error).message
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = FashionErrorLight,
              border = androidx.compose.foundation.BorderStroke(1.dp, FashionError.copy(alpha = 0.3f)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .testTag("error_banner")
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ErrorOutline,
                  contentDescription = "Error",
                  tint = FashionError,
                  modifier = Modifier.size(20.dp)
                )
                Text(
                  text = errorMsg,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                  ),
                  color = FashionError,
                  modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.dismissError() }) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = FashionError,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Prominent Try It On Button (Section 9)
          TryOnCtaBar(
            isReadyToGenerate = isReadyToGenerate,
            uiState = uiState,
            onTryOnClicked = { viewModel.startTryOn() },
            onViewResultClicked = { /* already handled by success state */ }
          )

          // Landing Information: How it Works, Why TRYON AI?, Privacy Notice
          HowItWorksSection()
        }
      }

      // Processing Modal Experience (Section 10)
      if (uiState is TryOnUiState.Processing) {
        ProcessingOverlay(
          processingState = uiState as TryOnUiState.Processing,
          onDismissRequest = { /* Modal during active inference */ }
        )
      }

      // Architecture & Settings Bottom Sheet
      if (showSettings) {
        SettingsSheet(
          config = config,
          onSaveConfig = { viewModel.updateConfig(it) },
          onDismiss = { showSettings = false }
        )
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

