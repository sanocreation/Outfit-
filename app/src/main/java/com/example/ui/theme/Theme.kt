package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = FashionWhite,
    onPrimary = FashionBlack,
    primaryContainer = FashionCharcoal,
    onPrimaryContainer = FashionWhite,
    secondary = FashionGold,
    onSecondary = FashionBlack,
    secondaryContainer = FashionCharcoal,
    onSecondaryContainer = FashionGold,
    background = FashionDarkBackground,
    onBackground = FashionWhite,
    surface = FashionDarkSurface,
    onSurface = FashionWhite,
    surfaceVariant = FashionCharcoal,
    onSurfaceVariant = FashionMutedGray,
    outline = FashionBorderDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = FashionBlack,
    onPrimary = FashionWhite,
    primaryContainer = FashionBlack,
    onPrimaryContainer = FashionWhite,
    secondary = FashionGoldDark,
    onSecondary = FashionWhite,
    secondaryContainer = FashionGoldLight,
    onSecondaryContainer = FashionCharcoal,
    background = FashionOffWhite,
    onBackground = FashionBlack,
    surface = FashionWhite,
    onSurface = FashionBlack,
    surfaceVariant = FashionOffWhite,
    onSurfaceVariant = FashionMutedGray,
    outline = FashionBorder,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
