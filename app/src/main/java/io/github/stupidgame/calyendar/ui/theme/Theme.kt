package io.github.stupidgame.calyendar.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Ocean,
    onPrimary = Panel,
    primaryContainer = OceanLight,
    onPrimaryContainer = Ink,
    secondary = Ink,
    onSecondary = Panel,
    error = Coral,
    background = Canvas,
    onBackground = Ink,
    surface = Panel,
    onSurface = Ink,
    surfaceVariant = ColorSchemeLightVariant,
    onSurfaceVariant = ColorSchemeLightMuted
)

private val DarkColors = darkColorScheme(
    primary = OceanLight,
    onPrimary = Night,
    primaryContainer = Ocean,
    onPrimaryContainer = NightInk,
    secondary = NightInk,
    onSecondary = Night,
    error = Coral,
    background = Night,
    onBackground = NightInk,
    surface = NightPanel,
    onSurface = NightInk,
    surfaceVariant = NightPanel,
    onSurfaceVariant = ColorSchemeDarkMuted
)

@Composable
fun CalYendarTheme(
    useDarkTheme: Boolean = false,
    useSystemSettings: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = if (useSystemSettings) isSystemInDarkTheme() else useDarkTheme
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(colorScheme = colors, typography = Typography, content = content)
}
