package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.ClothingCategory
import com.example.model.UploadedImage
import com.example.ui.theme.FashionBlack
import com.example.ui.theme.FashionBorder
import com.example.ui.theme.FashionError
import com.example.ui.theme.FashionGold
import com.example.ui.theme.FashionGoldDark
import com.example.ui.theme.FashionGoldLight
import com.example.ui.theme.FashionMutedGray
import com.example.ui.theme.FashionOffWhite
import com.example.ui.theme.FashionSuccess
import com.example.ui.theme.FashionWhite

@Composable
fun UploadStudioSection(
    userImage: UploadedImage?,
    garmentImage: UploadedImage?,
    selectedCategory: ClothingCategory,
    onCategoryChange: (ClothingCategory) -> Unit,
    onUserImageSelected: (Uri) -> Unit,
    onUserPresetSelected: (Int) -> Unit,
    onRemoveUserImage: () -> Unit,
    onGarmentImageSelected: (Uri) -> Unit,
    onGarmentPresetSelected: (Int) -> Unit,
    onRemoveGarmentImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Category Selector Bar
        Column {
            Text(
                text = "GARMENT CATEGORY",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = FashionMutedGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClothingCategory.values().filter { it != ClothingCategory.ALL }.forEach { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategoryChange(category) },
                        label = {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FashionBlack,
                            selectedLabelColor = FashionWhite,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = FashionBlack
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = FashionBorder,
                            selectedBorderColor = FashionBlack,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Card 1: User Photo Upload
        UploadCard(
            title = "Your Photo",
            subtitle = "Upload a clear photo of yourself.",
            uploadIconText = "📷 Upload Your Photo",
            supportingText = "Use a clear front-facing photo for better results.",
            uploadedImage = userImage,
            presetDrawableRes = R.drawable.img_sample_model,
            presetLabel = "Sample Model",
            onImagePicked = onUserImageSelected,
            onPresetPicked = { onUserPresetSelected(R.drawable.img_sample_model) },
            onRemoveImage = onRemoveUserImage,
            testTagPrefix = "user_photo"
        )

        // Card 2: Clothing / Product Image Upload
        UploadCard(
            title = "Your Outfit",
            subtitle = "Upload the clothing or product image you want to try.",
            uploadIconText = "👗 Upload Outfit",
            supportingText = "For best results, use a clear front-facing clothing image.",
            uploadedImage = garmentImage,
            presetDrawableRes = R.drawable.img_sample_garment,
            presetLabel = "Sample Blazer",
            onImagePicked = onGarmentImageSelected,
            onPresetPicked = { onGarmentPresetSelected(R.drawable.img_sample_garment) },
            onRemoveImage = onRemoveGarmentImage,
            testTagPrefix = "garment_photo"
        )
    }
}

@Composable
fun UploadCard(
    title: String,
    subtitle: String,
    uploadIconText: String,
    supportingText: String,
    uploadedImage: UploadedImage?,
    presetDrawableRes: Int,
    presetLabel: String,
    onImagePicked: (Uri) -> Unit,
    onPresetPicked: () -> Unit,
    onRemoveImage: () -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Android Photo Picker for zero-permission modern gallery selection
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImagePicked(uri)
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, FashionBorder),
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .testTag("${testTagPrefix}_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FashionMutedGray
                    )
                }

                if (uploadedImage != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFF0FDF4))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid",
                                tint = FashionSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Ready",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FashionSuccess
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (uploadedImage == null) {
                // Empty state upload trigger box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.5.dp,
                            FashionBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .background(FashionOffWhite)
                        .clickable { galleryLauncher.launch("image/*") }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uploadIconText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = FashionBlack
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = supportingText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            textAlign = TextAlign.Center,
                            color = FashionMutedGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = FashionWhite,
                                border = androidx.compose.foundation.BorderStroke(1.dp, FashionBorder),
                                modifier = Modifier.clickable { galleryLauncher.launch("image/*") }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Collections,
                                        contentDescription = "Gallery",
                                        modifier = Modifier.size(14.dp),
                                        tint = FashionBlack
                                    )
                                    Text(
                                        text = "Gallery",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = FashionGoldLight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, FashionGold),
                                modifier = Modifier.clickable { onPresetPicked() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "★ $presetLabel",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = FashionGoldDark
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Uploaded preview state
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image thumbnail
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, FashionBorder, RoundedCornerShape(14.dp))
                            .background(FashionOffWhite)
                    ) {
                        if (uploadedImage.drawableResId != null) {
                            Image(
                                painter = painterResource(id = uploadedImage.drawableResId),
                                contentDescription = title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (uploadedImage.uri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(uploadedImage.uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (uploadedImage.bitmap != null) {
                            Image(
                                bitmap = uploadedImage.bitmap.asImageBitmap(),
                                contentDescription = title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Metadata & Actions
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = uploadedImage.fileName.take(20),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = FashionBlack
                        )
                        Text(
                            text = "${uploadedImage.width} × ${uploadedImage.height} px • ${uploadedImage.displaySize}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = FashionMutedGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Replace and Remove action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FashionBorder)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Replace",
                                    modifier = Modifier.size(14.dp),
                                    tint = FashionBlack
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Replace",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = FashionBlack
                                )
                            }

                            OutlinedButton(
                                onClick = onRemoveImage,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FashionBorder)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(14.dp),
                                    tint = FashionError
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Remove",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = FashionError
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
