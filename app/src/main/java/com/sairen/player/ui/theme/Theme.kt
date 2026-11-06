package com.sairen.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.sairen.player.data.ThemeMode
import com.sairen.player.ui.util.findActivity

private val DarkColors = darkColorScheme(
    primary = SairenGreen,
    onPrimary = InkOnAccent,
    primaryContainer = SairenGreenDark,
    onPrimaryContainer = InkOnBackground,
    secondary = SairenGreenLight,
    onSecondary = InkOnAccent,
    background = InkBackground,
    onBackground = InkOnBackground,
    surface = InkSurface,
    onSurface = InkOnBackground,
    surfaceVariant = InkSurfaceHigh,
    onSurfaceVariant = InkOnSurfaceMuted,
    surfaceContainer = InkSurface,
    surfaceContainerHigh = InkSurfaceHigh,
    surfaceContainerHighest = InkSurfaceHigh,
    outline = InkOutline,
    outlineVariant = InkOutline,
    error = ErrorRed,
    onError = InkOnBackground,
)

private val LightColors = lightColorScheme(
    primary = SairenGreenDark,
    onPrimary = PaperSurface,
    primaryContainer = SairenGreenLight,
    onPrimaryContainer = PaperOnBackground,
    secondary = SairenGreenDark,
    onSecondary = PaperSurface,
    background = PaperBackground,
    onBackground = PaperOnBackground,
    surface = PaperSurface,
    onSurface = PaperOnBackground,
    surfaceVariant = PaperSurfaceHigh,
    onSurfaceVariant = PaperOnSurfaceMuted,
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperSurfaceHigh,
    surfaceContainerHighest = PaperSurfaceHigh,
    outline = PaperOutline,
    outlineVariant = PaperOutline,
    error = ErrorRed,
    onError = PaperSurface,
)

@Composable
fun SairenTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = shouldUseDarkTheme(themeMode, isSystemInDarkTheme())
    val view = LocalView.current

    // Keep system bar icons readable against the app's own background, even when
    // the in-app theme disagrees with the system setting.
    LaunchedEffect(darkTheme, view) {
        if (view.isInEditMode) return@LaunchedEffect
        val window = view.context.findActivity()?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SairenTypography,
        content = content,
    )
}
