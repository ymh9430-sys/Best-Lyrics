package com.lyrics.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// إعداد ألوان الوضع المظلم لتطابق الصور (أسود AMOLED وأخضر زاهي)
private val LyrixDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),          // اللون الأخضر الأساسي من الصور
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1B3D1E),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF4CAF50),
    background = Color.Black,             // خلفية سوداء نقية كما في الصور
    surface = Color.Black,                // أسطح سوداء
    surfaceVariant = Color(0xFF1A1A1A),   // لون رمادي غامق جداً لشريط البحث والبطاقات
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.Gray         // للنصوص الثانوية مثل اسم الفنان
)

// إعداد ألوان الوضع الفاتح (اختياري، ولكن يفضل إبقاؤه بسيطاً)
private val LyrixLightColorScheme = lightColorScheme(
    primary = Color(0xFF1B6E24),
    onPrimary = Color.White,
    background = Color(0xFFF5FBF5),
    surface = Color.White,
)

@Composable
fun LyricsAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // في هذا التصميم، الصور توضح تفضيل الوضع المظلم الصريح
    val colorScheme = if (darkTheme) LyrixDarkColorScheme else LyrixLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
