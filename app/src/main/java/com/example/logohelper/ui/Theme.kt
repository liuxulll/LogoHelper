package com.example.logohelper.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 清爽蓝白配色 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF2F80ED),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E8FF),
    onPrimaryContainer = Color(0xFF103A6E),
    secondary = Color(0xFF1FA97A),
    onSecondary = Color.White,
    background = Color(0xFFF6F8FB),
    onBackground = Color(0xFF1C2430),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C2430),
    onSurfaceVariant = Color(0xFF5A6675),
    error = Color(0xFFD64545),
    errorContainer = Color(0xFFFDE3E3),
    onErrorContainer = Color(0xFF7A1F1F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FB2F9),
    onPrimary = Color(0xFF0E2A4D),
    primaryContainer = Color(0xFF173A63),
    secondary = Color(0xFF6FD8A8),
    background = Color(0xFF10161F),
    surface = Color(0xFF18212D)
)

@Composable
fun LogoHelperTheme(
    // 默认使用清爽的浅色主题
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
