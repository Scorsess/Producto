package com.producto.timer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF62E6A8),
    onPrimary = Color(0xFF003821),
    secondary = Color(0xFFB7EFD0),
    onSecondary = Color(0xFF123525),
    background = Color(0xFF0B1110),
    onBackground = Color(0xFFE7F3EC),
    surface = Color(0xFF121C19),
    onSurface = Color(0xFFE7F3EC),
    surfaceVariant = Color(0xFF26352F),
    onSurfaceVariant = Color(0xFFB7C9BF),
    outline = Color(0xFF4B6458)
)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF176B45),
    onPrimary = Color.White,
    secondary = Color(0xFF3C6A53),
    background = Color(0xFFF5FAF7),
    onBackground = Color(0xFF17201B),
    surface = Color.White,
    onSurface = Color(0xFF17201B),
    surfaceVariant = Color(0xFFDCEBE1),
    onSurfaceVariant = Color(0xFF405148)
)
@Composable
fun ProductoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    fontFamilyIndex: Int = 0,
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

    val typography = getTypography(fontFamilyIndex)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
