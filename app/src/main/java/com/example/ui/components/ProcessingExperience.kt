package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.TryOnUiState
import com.example.ui.theme.FashionBlack
import com.example.ui.theme.FashionBorder
import com.example.ui.theme.FashionError
import com.example.ui.theme.FashionGold
import com.example.ui.theme.FashionGoldDark
import com.example.ui.theme.FashionMutedGray
import com.example.ui.theme.FashionOffWhite
import com.example.ui.theme.FashionSuccess
import com.example.ui.theme.FashionWhite

@Composable
fun TryOnCtaBar(
    isReadyToGenerate: Boolean,
    uiState: TryOnUiState,
    onTryOnClicked: () -> Unit,
    onViewResultClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        val buttonText: String
        val isButtonEnabled: Boolean
        val buttonColor: Color

        when (uiState) {
            is TryOnUiState.Idle -> {
                buttonText = "✨ TRY IT ON →"
                isButtonEnabled = isReadyToGenerate
                buttonColor = if (isReadyToGenerate) FashionBlack else FashionMutedGray.copy(alpha = 0.5f)
            }
            is TryOnUiState.Validating, is TryOnUiState.Processing -> {
                buttonText = "GENERATING..."
                isButtonEnabled = false
                buttonColor = FashionBlack
            }
            is TryOnUiState.Success -> {
                buttonText = "VIEW RESULT"
                isButtonEnabled = true
                buttonColor = FashionBlack
            }
            is TryOnUiState.Error -> {
                buttonText = "TRY AGAIN"
                isButtonEnabled = true
                buttonColor = FashionError
            }
        }

        Button(
            onClick = {
                if (uiState is TryOnUiState.Success) {
                    onViewResultClicked()
                } else {
                    onTryOnClicked()
                }
            },
            enabled = isButtonEnabled,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = FashionWhite,
                disabledContainerColor = FashionOffWhite,
                disabledContentColor = FashionMutedGray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(if (isButtonEnabled) 4.dp else 0.dp, RoundedCornerShape(16.dp))
                .testTag("try_it_on_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (uiState is TryOnUiState.Processing || uiState is TryOnUiState.Validating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = FashionWhite,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        if (!isReadyToGenerate && uiState is TryOnUiState.Idle) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Upload both your photo and an outfit to continue",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = FashionMutedGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ProcessingOverlay(
    processingState: TryOnUiState.Processing,
    onDismissRequest: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val stepMessages = listOf(
        "Analyzing your photo...",
        "Understanding the clothing...",
        "Mapping the outfit...",
        "Creating your virtual look...",
        "Finalizing your result..."
    )

    Dialog(
        onDismissRequest = { /* Non-cancellable while processing */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, FashionBorder),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .testTag("processing_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Creating Your Look",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = FashionBlack
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "AI Virtual Try-On in progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FashionMutedGray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Fashion-tech animated scanner visualization
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(FashionOffWhite)
                        .border(1.5.dp, FashionGold.copy(alpha = pulseAlpha), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        FashionGold.copy(alpha = pulseAlpha * 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Processing",
                        tint = FashionGoldDark,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Animated current step message
                AnimatedContent(
                    targetState = processingState.currentMessage,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                    label = "stepMessage"
                ) { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center,
                        color = FashionBlack
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Indicator
                LinearProgressIndicator(
                    progress = { processingState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = FashionGoldDark,
                    trackColor = FashionOffWhite,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Step dots checklist
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    stepMessages.forEachIndexed { idx, _ ->
                        val isDone = idx < processingState.stepIndex
                        val isCurrent = idx == processingState.stepIndex
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> FashionSuccess
                                        isCurrent -> FashionGoldDark
                                        else -> FashionBorder
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}
