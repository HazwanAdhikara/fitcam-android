package com.example.fitcam.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// We will use the same "Fun" palette for both Light and Dark mode for consistency
private val BrandColorScheme = lightColorScheme(
    primary = FitCamBlue,
    onPrimary = FitCamCream,
    primaryContainer = FitCamYellow,
    onPrimaryContainer = FitCamBlue,
    secondary = FitCamRed,
    onSecondary = FitCamCream,
    background = FitCamCream,
    onBackground = FitCamBlue,
    surface = FitCamCream,
    onSurface = FitCamBlue,
    error = FitCamRed
)

@Composable
fun FitcamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force the brand colors (ignoring dynamic colors for now to keep the identity strong)
    MaterialTheme(
        colorScheme = BrandColorScheme,
        typography = Typography,
        content = content
    )
}