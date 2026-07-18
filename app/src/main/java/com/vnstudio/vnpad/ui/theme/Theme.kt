package com.vnstudio.vnpad.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// VNPad is a dark, high-contrast "stage" so the coloured pads pop. We commit to
// a single dark scheme regardless of the system setting.
val VnBg = Color(0xFF0B0E14)
val VnSurface = Color(0xFF141922)
val VnSurfaceHi = Color(0xFF1E2530)
val VnAccent = Color(0xFF007CF0)
val VnText = Color(0xFFE6EAF0)
val VnTextDim = Color(0xFF8A94A6)

private val VNPadColors = darkColorScheme(
    primary = VnAccent,
    onPrimary = Color.White,
    background = VnBg,
    onBackground = VnText,
    surface = VnSurface,
    onSurface = VnText,
    surfaceVariant = VnSurfaceHi,
    onSurfaceVariant = VnTextDim,
)

private val VNPadTypography = Typography(
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.Bold),
)

@Composable
fun VNPadTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = VNPadColors,
        typography = VNPadTypography,
        content = content,
    )
}
