package com.driveflow.driver.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = RacingOrange,
    onPrimary = ObsidianBackground,
    primaryContainer = RacingOrangeDark,
    onPrimaryContainer = TextBrightWhite,
    secondary = EmeraldGreen,
    onSecondary = ObsidianBackground,
    secondaryContainer = EmeraldGreenDark,
    onSecondaryContainer = TextBrightWhite,
    background = ObsidianBackground,
    onBackground = TextBrightWhite,
    surface = GraphiteSurface,
    onSurface = TextBrightWhite,
    surfaceVariant = GraphiteSurfaceVariant,
    onSurfaceVariant = TextSilverGray,
    outline = SlateBorder,
    error = VerdictRed,
    onError = TextBrightWhite
)

@Composable
fun DriveFlowTheme(
    isRtl: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ObsidianBackground.toArgb()
                window.navigationBarColor = ObsidianBackground.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
