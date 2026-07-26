package com.avelcam.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme

val Dark = Color(0xFF0F1116)

private val AvelCamDarkColorScheme = darkColorScheme(
    primary = Color(0xFF55E2FF),
    onPrimary = Color(0xFF07262D),
    secondary = Color(0xFF7FD3FF),
    onSecondary = Color(0xFF03212F),
    background = Dark,
    onBackground = Color(0xFFE6EBF2),
    surface = Color(0xFF171C26),
    onSurface = Color(0xFFE6EBF2)
)

@Composable
fun AvelCamTheme(
    useDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) {
        AvelCamDarkColorScheme
    } else {
        AvelCamDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

