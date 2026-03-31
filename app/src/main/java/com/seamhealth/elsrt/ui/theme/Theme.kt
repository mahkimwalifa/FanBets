package com.seamhealth.elsrt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryRed,
    onPrimary = White,
    primaryContainer = PrimaryRedDark,
    onPrimaryContainer = White,
    secondary = DarkBlue,
    onSecondary = White,
    secondaryContainer = DarkBlueMedium,
    onSecondaryContainer = White,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = LightGray,
    onSurfaceVariant = DarkGray,
    outline = MediumGray,
    error = RedCard
)

@Composable
fun FanBetsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
