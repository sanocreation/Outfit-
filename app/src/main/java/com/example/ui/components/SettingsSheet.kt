package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppConfig
import com.example.ui.theme.FashionBlack
import com.example.ui.theme.FashionBorder
import com.example.ui.theme.FashionGold
import com.example.ui.theme.FashionGoldDark
import com.example.ui.theme.FashionGoldLight
import com.example.ui.theme.FashionMutedGray
import com.example.ui.theme.FashionOffWhite
import com.example.ui.theme.FashionWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    config: AppConfig,
    onSaveConfig: (AppConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var demoMode by remember { mutableStateOf(config.demoMode) }
    var backendUrl by remember { mutableStateOf(config.backendUrl) }
    var selectedProvider by remember { mutableStateOf(config.selectedProvider) }
    var apiKey by remember { mutableStateOf(config.apiKey) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(FashionBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Engine Architecture & Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = FashionBlack
            )

            // Demo Mode Switch
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = FashionOffWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, FashionBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DEMO_MODE",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = FashionBlack
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (demoMode) "Local simulation active. Perfect for UI & MVP testing without remote GPU."
                            else "Live remote VTON model endpoint active.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = FashionMutedGray
                        )
                    }
                    Switch(
                        checked = demoMode,
                        onCheckedChange = { demoMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FashionWhite,
                            checkedTrackColor = FashionBlack,
                            uncheckedThumbColor = FashionMutedGray,
                            uncheckedTrackColor = FashionBorder
                        ),
                        modifier = Modifier.testTag("demo_mode_switch")
                    )
                }
            }

            // Provider Selection
            Column {
                Text(
                    text = "VTON PROVIDER ABSTRACTION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FashionMutedGray
                )
                Spacer(modifier = Modifier.height(8.dp))

                val providers = listOf(
                    Triple("DEMO_VTON", "TRYON AI Studio Demo Engine", "Built-in offline neural compositing"),
                    Triple("REMOTE_FASTAPI", "FastAPI Remote Microservice", "POST /api/try-on async job runner"),
                    Triple("CAT_VTON", "CatVTON Inpainting Pipeline", "Concatenation diffusion on GPU"),
                    Triple("IDM_VTON", "IDM-VTON Provider", "High-fidelity photorealistic clothing transfer")
                )

                providers.forEach { (id, name, desc) ->
                    val isSelected = selectedProvider == id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedProvider = id }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedProvider = id },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = FashionBlack,
                                unselectedColor = FashionMutedGray
                            )
                        )
                        Column {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = FashionBlack
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                color = FashionMutedGray
                            )
                        }
                    }
                }
            }

            // Backend Endpoint Configuration
            Column {
                Text(
                    text = "BACKEND ENDPOINT (PHASE 2/3)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FashionMutedGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = { backendUrl = it },
                    label = { Text("BACKEND_URL") },
                    placeholder = { Text("http://10.0.2.2:8000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FashionBlack,
                        unfocusedBorderColor = FashionBorder,
                        focusedLabelColor = FashionBlack
                    )
                )
            }

            // Architecture Flow Note
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = FashionGoldLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, FashionGold)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = FashionGoldDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Modular Pipeline Design",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = FashionGoldDark
                        )
                        Text(
                            text = "Phone (Client UI) → VirtualTryOnService → VtonProvider → Remote GPU / Demo Engine. The frontend never tightly couples to any single model.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = FashionBlack
                        )
                    }
                }
            }

            // Save CTA
            Button(
                onClick = {
                    onSaveConfig(
                        config.copy(
                            demoMode = demoMode,
                            backendUrl = backendUrl,
                            selectedProvider = selectedProvider,
                            apiKey = apiKey
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FashionBlack,
                    contentColor = FashionWhite
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Apply Configuration",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
