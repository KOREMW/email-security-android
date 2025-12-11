package com.example.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = TextWhite,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = TextPrimary,
    secondary = PrimaryDark,
    onSecondary = TextWhite,
    secondaryContainer = SafeGreenLight,
    onSecondaryContainer = TextPrimary,
    tertiary = WarningYellow,
    onTertiary = TextWhite,
    tertiaryContainer = WarningYellowLight,
    onTertiaryContainer = TextPrimary,
    error = DangerRed,
    onError = TextWhite,
    errorContainer = DangerRedLight,
    onErrorContainer = TextPrimary,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundCard,
    onSurfaceVariant = TextSecondary,
    outline = TextHint
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDarkTheme,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = TextWhite,
    secondary = PrimaryDarkTheme,
    onSecondary = TextPrimary,
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = TextWhite,
    tertiary = WarningYellow,
    onTertiary = TextPrimary,
    tertiaryContainer = Color(0xFF5D4037),
    onTertiaryContainer = TextWhite,
    error = Color(0xFFCF6679),
    onError = TextPrimary,
    errorContainer = Color(0xFF8B0000),
    onErrorContainer = TextWhite,
    background = BackgroundDark,
    onBackground = TextWhite,
    surface = SurfaceDarkTheme,
    onSurface = TextWhite,
    surfaceVariant = CardDark,
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF5C5C5C)
)

@Composable
fun EmailSecurityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Dynamic color 비활성화 (브랜드 컬러 유지)
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
