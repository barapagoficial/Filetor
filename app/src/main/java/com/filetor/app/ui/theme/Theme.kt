package com.filetor.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.filetor.app.data.Prefs
import com.filetor.app.data.ThemeMode

data class Accent(val name: String, val light: Color, val dark: Color, val tertiary: Color)

val Accents = listOf(
    Accent("Índigo", Color(0xFF4C5BD4), Color(0xFFBAC3FF), Color(0xFFE0619A)),
    Accent("Océano", Color(0xFF0F6FB8), Color(0xFF9CCBFF), Color(0xFF14A38B)),
    Accent("Esmeralda", Color(0xFF16855B), Color(0xFF7DDBAA), Color(0xFF3A7FC2)),
    Accent("Coral", Color(0xFFD1503A), Color(0xFFFFB4A5), Color(0xFF8E6BD8)),
    Accent("Ámbar", Color(0xFFB86E00), Color(0xFFFFC07A), Color(0xFF2C8FA6)),
    Accent("Orquídea", Color(0xFF9C3FBF), Color(0xFFEDB1FF), Color(0xFFDB6B3A)),
    Accent("Grafito", Color(0xFF4A5568), Color(0xFFC3CCDA), Color(0xFF4C8DD1)),
)

private fun lightScheme(a: Accent): ColorScheme {
    val p = a.light
    val neutral = lerp(Color(0xFFF7F8FC), p, 0.03f)
    return lightColorScheme(
        primary = p,
        onPrimary = Color.White,
        primaryContainer = lerp(Color.White, p, 0.16f),
        onPrimaryContainer = lerp(p, Color.Black, 0.55f),
        secondary = lerp(p, Color(0xFF5F6273), 0.55f),
        onSecondary = Color.White,
        secondaryContainer = lerp(Color.White, p, 0.11f),
        onSecondaryContainer = lerp(p, Color.Black, 0.6f),
        tertiary = a.tertiary,
        onTertiary = Color.White,
        tertiaryContainer = lerp(Color.White, a.tertiary, 0.16f),
        onTertiaryContainer = lerp(a.tertiary, Color.Black, 0.55f),
        background = neutral,
        onBackground = Color(0xFF1A1B22),
        surface = neutral,
        onSurface = Color(0xFF1A1B22),
        surfaceVariant = lerp(Color(0xFFE6E8F0), p, 0.05f),
        onSurfaceVariant = Color(0xFF5A5D6B),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = lerp(Color(0xFFF2F3F8), p, 0.025f),
        surfaceContainer = lerp(Color(0xFFEDEEF4), p, 0.035f),
        surfaceContainerHigh = lerp(Color(0xFFE8E9F0), p, 0.045f),
        surfaceContainerHighest = lerp(Color(0xFFE2E3EB), p, 0.055f),
        outline = Color(0xFF8A8D9C),
        outlineVariant = Color(0xFFD5D7E1),
        error = Color(0xFFD33A3A),
        errorContainer = Color(0xFFFFE1DE),
        onErrorContainer = Color(0xFF5C0C0C),
    )
}

private fun darkScheme(a: Accent, amoled: Boolean): ColorScheme {
    val p = a.dark
    val base = if (amoled) Color.Black else lerp(Color(0xFF111217), p, 0.02f)
    fun tone(t: Float) = if (amoled) lerp(Color.Black, lerp(Color(0xFF1C1D24), p, 0.04f), t) else lerp(Color(0xFF111217), lerp(Color(0xFF2C2E38), p, 0.06f), t)
    return darkColorScheme(
        primary = p,
        onPrimary = lerp(p, Color.Black, 0.75f),
        primaryContainer = lerp(Color(0xFF1A1B22), p, 0.30f),
        onPrimaryContainer = lerp(p, Color.White, 0.6f),
        secondary = lerp(p, Color(0xFFC4C6D4), 0.5f),
        onSecondary = Color(0xFF22232C),
        secondaryContainer = lerp(Color(0xFF1E1F27), p, 0.18f),
        onSecondaryContainer = lerp(p, Color.White, 0.7f),
        tertiary = lerp(a.tertiary, Color.White, 0.45f),
        onTertiary = Color(0xFF2A1520),
        tertiaryContainer = lerp(Color(0xFF1A1B22), a.tertiary, 0.35f),
        onTertiaryContainer = lerp(a.tertiary, Color.White, 0.75f),
        background = base,
        onBackground = Color(0xFFE4E5EE),
        surface = base,
        onSurface = Color(0xFFE4E5EE),
        surfaceVariant = tone(0.8f),
        onSurfaceVariant = Color(0xFFA9ACBB),
        surfaceContainerLowest = if (amoled) Color.Black else Color(0xFF0C0D11),
        surfaceContainerLow = tone(0.35f),
        surfaceContainer = tone(0.5f),
        surfaceContainerHigh = tone(0.68f),
        surfaceContainerHighest = tone(0.85f),
        outline = Color(0xFF7A7D8C),
        outlineVariant = Color(0xFF393B47),
        error = Color(0xFFFF8A80),
        errorContainer = Color(0xFF5C1A17),
        onErrorContainer = Color(0xFFFFDAD5),
    )
}

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

val MonoStyle = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp)

@Composable
fun isAppInDarkTheme(): Boolean = when (Prefs.themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun FiletorTheme(content: @Composable () -> Unit) {
    val dark = isAppInDarkTheme()
    val context = LocalContext.current
    val accent = Accents.getOrElse(Prefs.accent) { Accents[0] }
    var scheme = when {
        Prefs.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkScheme(accent, Prefs.amoled)
        else -> lightScheme(accent)
    }
    if (dark && Prefs.amoled && Prefs.dynamicColor) {
        scheme = scheme.copy(background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val ctl = WindowCompat.getInsetsController(window, view)
            ctl.isAppearanceLightStatusBars = !dark
            ctl.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
}

@Suppress("unused")
private fun Color.argb() = toArgb()
