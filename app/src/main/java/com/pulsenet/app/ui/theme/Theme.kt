package com.pulsenet.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PulseColorScheme = darkColorScheme(
    primary = PulseBlue,
    onPrimary = PulseBackground,
    secondary = SOSRed,
    onSecondary = TextPrimary,
    background = PulseBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    error = SOSRed,
    onError = TextPrimary
)

/** Dark-only theme: disaster scenarios don't get a light mode, and it saves OLED battery. */
@Composable
fun PulseNetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PulseColorScheme,
        typography = PulseTypography,
        content = content
    )
}
