package com.example.poco.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = PocoGreen,
    onPrimary = Color.White,
    primaryContainer = PocoGreenCardBackground,
    onPrimaryContainer = PocoGreenDark,
    secondary = PocoNavy,
    onSecondary = Color.White,
    secondaryContainer = PocoCardBackground,
    onSecondaryContainer = PocoNavy,
    tertiary = PocoAmber,
    onTertiary = Color.White,
    tertiaryContainer = PocoAmberBackground,
    onTertiaryContainer = PocoAmber,
    error = PocoRed,
    onError = Color.White,
    errorContainer = PocoRedCardBackground,
    onErrorContainer = PocoRedDark,
    background = Color.White,
    onBackground = PocoTextPrimary,
    surface = Color.White,
    onSurface = PocoTextPrimary,
    surfaceVariant = PocoCardBackground,
    onSurfaceVariant = PocoTextMuted,
    outline = PocoDivider,
    outlineVariant = PocoDivider
)

@Composable
fun POCOTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep the POCO brand palette everywhere — Material You dynamic color would otherwise
    // override it with per-device wallpaper colors on Android 12+.
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}