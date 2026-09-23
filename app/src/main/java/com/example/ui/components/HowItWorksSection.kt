package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FashionBlack
import com.example.ui.theme.FashionBorder
import com.example.ui.theme.FashionGold
import com.example.ui.theme.FashionGoldDark
import com.example.ui.theme.FashionGoldLight
import com.example.ui.theme.FashionMutedGray
import com.example.ui.theme.FashionOffWhite

@Composable
fun HowItWorksSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // "How It Works" Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, FashionBorder),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "HOW IT WORKS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FashionMutedGray
                )
                Spacer(modifier = Modifier.height(14.dp))

                StepRow(
                    stepNumber = "1",
                    title = "Upload Your Photo",
                    description = "Take or choose a clear front-facing portrait in good lighting.",
                    icon = Icons.Default.Person
                )
                Spacer(modifier = Modifier.height(12.dp))
                StepRow(
                    stepNumber = "2",
                    title = "Upload Your Outfit",
                    description = "Add any garment, jacket, or product image you want to try.",
                    icon = Icons.Default.Checkroom
                )
                Spacer(modifier = Modifier.height(12.dp))
                StepRow(
                    stepNumber = "3",
                    title = "See Your Look",
                    description = "The AI maps the outfit onto your body preserving pose and style.",
                    icon = Icons.Default.Visibility
                )
            }
        }

        // "Why TRYON AI?" Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, FashionBorder),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "WHY TRYON AI?",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FashionMutedGray
                )
                Spacer(modifier = Modifier.height(14.dp))

                BenefitRow(
                    title = "Preview Before Buying",
                    description = "Eliminate guesswork and reduce returns on online fashion purchases.",
                    icon = Icons.Default.ShoppingBag
                )
                Spacer(modifier = Modifier.height(10.dp))
                BenefitRow(
                    title = "Fast AI Visualization",
                    description = "State-of-the-art virtual try-on pipelines with realistic folds.",
                    icon = Icons.Default.Speed
                )
                Spacer(modifier = Modifier.height(10.dp))
                BenefitRow(
                    title = "Easy Mobile Experience",
                    description = "Instant camera and photo picker optimized for handheld phones.",
                    icon = Icons.Default.Smartphone
                )
                Spacer(modifier = Modifier.height(10.dp))
                BenefitRow(
                    title = "Try Multiple Outfits",
                    description = "Swap garments in seconds while keeping your base portrait.",
                    icon = Icons.Default.CheckCircle
                )
            }
        }

        // Privacy & Security Notice (Section 20 of prompt)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = FashionOffWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, FashionBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Privacy Protected",
                    tint = FashionGoldDark,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Privacy & Consent",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = FashionBlack
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your uploaded images are used strictly to generate your virtual try-on preview. Do not upload images of other people without their permission.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, lineHeight = 16.sp),
                        color = FashionMutedGray
                    )
                }
            }
        }
    }
}

@Composable
private fun StepRow(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(FashionBlack),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.FashionWhite
                )
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = FashionBlack
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = FashionMutedGray
            )
        }
    }
}

@Composable
private fun BenefitRow(
    title: String,
    description: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(FashionGoldLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FashionGoldDark,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = FashionBlack
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = FashionMutedGray
            )
        }
    }
}
