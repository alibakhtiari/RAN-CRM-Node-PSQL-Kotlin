package com.ran.crm.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
        darkColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFF9C27B0),
                onPrimary = androidx.compose.ui.graphics.Color.White,
                primaryContainer = androidx.compose.ui.graphics.Color(0xFF4A148C),
                onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
                secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6),
                onSecondary = androidx.compose.ui.graphics.Color.Black,
                secondaryContainer = androidx.compose.ui.graphics.Color(0xFF004D40),
                onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF03DAC6),
                tertiary = androidx.compose.ui.graphics.Color(0xFFFF9800),
                onTertiary = androidx.compose.ui.graphics.Color.Black,
                tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFE65100),
                onTertiaryContainer = androidx.compose.ui.graphics.Color.White,
                error = androidx.compose.ui.graphics.Color(0xFFCF6679),
                onError = androidx.compose.ui.graphics.Color.Black,
                errorContainer = androidx.compose.ui.graphics.Color(0xFFB00020),
                onErrorContainer = androidx.compose.ui.graphics.Color.White,
                background = androidx.compose.ui.graphics.Color(0xFF121212),
                onBackground = androidx.compose.ui.graphics.Color.White,
                surface = androidx.compose.ui.graphics.Color(0xFF121212),
                onSurface = androidx.compose.ui.graphics.Color.White,
                surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
                onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFBBBBBB),
                outline = androidx.compose.ui.graphics.Color(0xFF888888),
                outlineVariant = androidx.compose.ui.graphics.Color(0xFF444444)
        )

private val LightColorScheme =
        lightColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFF6200EE),
                onPrimary = androidx.compose.ui.graphics.Color.White,
                primaryContainer = androidx.compose.ui.graphics.Color(0xFFE8EAF6),
                onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF1A237E),
                secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6),
                onSecondary = androidx.compose.ui.graphics.Color.Black,
                secondaryContainer = androidx.compose.ui.graphics.Color(0xFFB2DFDB),
                onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF004D40),
                tertiary = androidx.compose.ui.graphics.Color(0xFFFF9800),
                onTertiary = androidx.compose.ui.graphics.Color.Black,
                tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFFF3E0),
                onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFE65100),
                error = androidx.compose.ui.graphics.Color(0xFFB00020),
                onError = androidx.compose.ui.graphics.Color.White,
                errorContainer = androidx.compose.ui.graphics.Color(0xFFFFCDD2),
                onErrorContainer = androidx.compose.ui.graphics.Color(0xFFB00020),
                background = androidx.compose.ui.graphics.Color.White,
                onBackground = androidx.compose.ui.graphics.Color.Black,
                surface = androidx.compose.ui.graphics.Color.White,
                onSurface = androidx.compose.ui.graphics.Color.Black,
                surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
                onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF666666),
                outline = androidx.compose.ui.graphics.Color(0xFF999999),
                outlineVariant = androidx.compose.ui.graphics.Color(0xFFCCCCCC)
        )

@Composable
fun RANCRMTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        fontScale: Float = 1.0f,
        // Dynamic color is available on Android 12+
        dynamicColor: Boolean = true,
        content: @Composable () -> Unit
) {
    val colorScheme =
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)
                }
                darkTheme -> DarkColorScheme
                else -> LightColorScheme
            }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val scaledDensity = androidx.compose.ui.unit.Density(currentDensity.density, fontScale)

    androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
                colorScheme = colorScheme,
                typography = androidx.compose.material3.Typography(),
                content = content
        )
    }
}
