package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.TryOnResult
import com.example.model.UploadedImage
import com.example.ui.theme.FashionBlack
import com.example.ui.theme.FashionBorder
import com.example.ui.theme.FashionGold
import com.example.ui.theme.FashionGoldDark
import com.example.ui.theme.FashionGoldLight
import com.example.ui.theme.FashionMutedGray
import com.example.ui.theme.FashionOffWhite
import com.example.ui.theme.FashionWhite
import com.example.util.ImageUtils

@Composable
fun ResultScreen(
    result: TryOnResult,
    originalPersonImage: UploadedImage?,
    onTryAnotherOutfit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var comparisonMode by remember { mutableIntStateOf(0) } // 0 = Toggle View, 1 = Interactive Slider
    var selectedToggleTab by remember { mutableIntStateOf(1) } // 0 = Original, 1 = AI Try-On
    var sliderFraction by remember { mutableFloatStateOf(0.5f) }
    var savedUri by remember { mutableStateOf<Uri?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Badge: “✨ AI RESULT”
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = FashionGoldLight,
            border = androidx.compose.foundation.BorderStroke(1.dp, FashionGold),
            modifier = Modifier.shadow(1.dp, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = FashionGoldDark,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "AI RESULT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FashionGoldDark
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title: “Your Virtual Look”
        Text(
            text = "Your Virtual Look",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = FashionBlack
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Generated by ${result.providerUsed}",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = FashionMutedGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Comparison mode switch (Tabs: "AI Try-On" vs "Original" vs "Interactive Split")
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = FashionOffWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, FashionBorder)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("Original", "AI Try-On", "Split Slider")
                tabs.forEachIndexed { idx, label ->
                    val isSelected = when (idx) {
                        0 -> comparisonMode == 0 && selectedToggleTab == 0
                        1 -> comparisonMode == 0 && selectedToggleTab == 1
                        2 -> comparisonMode == 1
                        else -> false
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) FashionBlack else Color.Transparent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (idx == 2) {
                                    comparisonMode = 1
                                } else {
                                    comparisonMode = 0
                                    selectedToggleTab = idx
                                }
                            }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) FashionWhite else FashionBlack,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Result Container Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, FashionBorder),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(22.dp))
                .testTag("result_image_container")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(FashionOffWhite)
            ) {
                if (comparisonMode == 0) {
                    // Toggle view
                    Crossfade(targetState = selectedToggleTab, label = "toggleImage") { tab ->
                        if (tab == 0) {
                            // Show Original Person
                            RenderUploadedImage(
                                image = originalPersonImage,
                                contentDescription = "Original Photo",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Show AI Result
                            RenderResultImage(
                                result = result,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    // Interactive Split Slider View
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val widthPx = constraints.maxWidth.toFloat()

                        // Base layer: Original
                        RenderUploadedImage(
                            image = originalPersonImage,
                            contentDescription = "Original Photo",
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top clipped layer: AI Result
                        val totalWidth = this@BoxWithConstraints.maxWidth
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(totalWidth * sliderFraction)
                                .clipToBounds()
                        ) {
                            RenderResultImage(
                                result = result,
                                modifier = Modifier
                                    .width(totalWidth)
                                    .fillMaxHeight()
                            )
                        }

                        // Divider Line & Handle
                        Box(
                            modifier = Modifier
                                .offset { IntOffset((widthPx * sliderFraction).toInt() - 16.dp.roundToPx(), 0) }
                                .fillMaxHeight()
                                .width(32.dp)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        val newFraction = (sliderFraction + dragAmount / widthPx).coerceIn(0.05f, 0.95f)
                                        sliderFraction = newFraction
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Vertical Line
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(FashionWhite)
                            )
                            // Circle Handle with arrows
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(FashionBlack)
                                    .border(2.dp, FashionWhite, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Drag to compare",
                                    tint = FashionWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // AI Result Tag overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FashionBlack.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (comparisonMode == 1) "Drag slider to compare" else if (selectedToggleTab == 1) "✨ AI Try-On" else "Original",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = FashionWhite
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mandatory disclaimer: “AI-generated preview. Actual fit, color and appearance may vary.”
        Text(
            text = "AI-generated preview. Actual fit, color and appearance may vary.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = FashionMutedGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Primary Action Buttons: Download, Share, Try Another Outfit
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Download Result Button
                Button(
                    onClick = {
                        val bitmapToSave = if (result.resultBitmap != null) {
                            result.resultBitmap
                        } else if (result.resultDrawableResId != null) {
                            ImageUtils.loadResourceBitmap(context, result.resultDrawableResId)
                        } else null

                        if (bitmapToSave != null) {
                            val saveResult = ImageUtils.saveImageToGallery(context, bitmapToSave)
                            saveResult.onSuccess { uri ->
                                savedUri = uri
                                Toast.makeText(context, "Saved to Pictures as tryon-ai-result.jpg", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, "Could not save: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FashionBlack,
                        contentColor = FashionWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("download_result_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Download",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Share Button (Native Android Intent.ACTION_SEND)
                OutlinedButton(
                    onClick = {
                        ImageUtils.shareResult(
                            context = context,
                            resultUri = savedUri,
                            shareText = "See how this outfit looks on me with TRYON AI."
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FashionBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = FashionBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_result_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = FashionBlack
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Try Another Outfit button
            OutlinedButton(
                onClick = onTryAnotherOutfit,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FashionBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("try_another_outfit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = FashionBlack,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "↻ Try Another Outfit",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = FashionBlack
                    )
                )
            }
        }
    }
}

@Composable
fun RenderUploadedImage(
    image: UploadedImage?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (image == null) {
        Image(
            painter = painterResource(id = R.drawable.img_sample_model),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (image.drawableResId != null) {
        Image(
            painter = painterResource(id = image.drawableResId),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (image.uri != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(image.uri)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (image.bitmap != null) {
        Image(
            bitmap = image.bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun RenderResultImage(
    result: TryOnResult,
    modifier: Modifier = Modifier
) {
    if (result.resultBitmap != null) {
        Image(
            bitmap = result.resultBitmap.asImageBitmap(),
            contentDescription = "AI Virtual Look",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (result.resultDrawableResId != null) {
        Image(
            painter = painterResource(id = result.resultDrawableResId),
            contentDescription = "AI Virtual Look",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_sample_result),
            contentDescription = "AI Virtual Look",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}
