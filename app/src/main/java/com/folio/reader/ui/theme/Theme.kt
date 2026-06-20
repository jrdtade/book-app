package com.folio.reader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FolioColorScheme = lightColorScheme(
    primary = Blue,
    onPrimary = Paper2,
    secondary = Gold,
    background = Paper,
    onBackground = Ink,
    surface = Paper2,
    onSurface = Ink,
    surfaceVariant = Paper3,
    onSurfaceVariant = Ink2,
    outline = LineColor,
)

@Composable
fun FolioTheme(content: @Composable () -> Unit) {
    // The reading surfaces (reader screen) manage their own theme switching;
    // the shell chrome always uses the warm paper palette regardless of system
    // dark mode, matching the original design.
    MaterialTheme(
        colorScheme = FolioColorScheme,
        typography = FolioTypography,
        content = content,
    )
}
