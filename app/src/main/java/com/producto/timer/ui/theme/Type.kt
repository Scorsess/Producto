package com.producto.timer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

fun getTypography(index: Int): Typography {
    // Normal UI text stays default
    return Typography(
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        )
    )
}

/** Returns the special clock font family based on index. */
fun getDisplayFontFamily(index: Int): FontFamily = when (index) {
    0 -> FontFamily.Monospace
    1 -> FontFamily.SansSerif
    2 -> FontFamily.Serif
    3 -> FontFamily.Monospace
    4 -> FontFamily.SansSerif
    5 -> FontFamily.Serif
    6 -> FontFamily.Monospace
    7 -> FontFamily.SansSerif
    8 -> FontFamily.Serif
    9 -> FontFamily.Monospace
    else -> FontFamily.Monospace
}

/** Returns the special clock font weight based on index. */
fun getDisplayFontWeight(index: Int): FontWeight = when (index) {
    0, 1, 2 -> FontWeight.Bold
    3, 4, 5 -> FontWeight.ExtraBold
    6, 7, 8 -> FontWeight.Black
    9 -> FontWeight.Thin
    else -> FontWeight.Bold
}

val FontNames = listOf(
    "Digital Mono",
    "Modern Sans",
    "Classic Serif",
    "Bold Quartz",
    "Futuristic",
    "Industrial",
    "Cyber Black",
    "Neon Glow",
    "Elegant Slab",
    "Ultra Thin"
)
