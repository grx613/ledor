package org.ledor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ledor.R
import org.ledor.data.prefs.ThemeMode
import org.ledor.domain.event.EventType

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B3FAE), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF), onPrimaryContainer = Color(0xFF001A4D),
    secondary = Color(0xFF7A5900), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE9B3), onSecondaryContainer = Color(0xFF261A00),
    tertiary = Color(0xFF5B4B8A), onTertiary = Color.White,
    background = Color(0xFFFBF9F4), onBackground = Color(0xFF141B2D),
    surface = Color.White, onSurface = Color(0xFF141B2D),
    surfaceVariant = Color(0xFFEEF1F8), onSurfaceVariant = Color(0xFF4A5368),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFDFCF9),
    surfaceContainer = Color(0xFFF6F4EE), surfaceContainerHigh = Color(0xFFF1EFE9), surfaceContainerHighest = Color(0xFFECEAE4),
    outline = Color(0xFFC3C8D6), outlineVariant = Color(0xFFE2E6EF),
    error = Color(0xFFB3261E), onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DB8FF), onPrimary = Color(0xFF002B75),
    primaryContainer = Color(0xFF0B3FAE), onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFF2C75C), onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF5A4300), onSecondaryContainer = Color(0xFFFFE9B3),
    tertiary = Color(0xFFC9BCF5), onTertiary = Color(0xFF2B1F57),
    background = Color(0xFF0B1220), onBackground = Color(0xFFE6EAF5),
    surface = Color(0xFF111A2E), onSurface = Color(0xFFE6EAF5),
    surfaceVariant = Color(0xFF1C2740), onSurfaceVariant = Color(0xFFB4BCD0),
    surfaceContainerLowest = Color(0xFF0B1220), surfaceContainerLow = Color(0xFF0F1828),
    surfaceContainer = Color(0xFF111A2E), surfaceContainerHigh = Color(0xFF172238), surfaceContainerHighest = Color(0xFF1C2740),
    outline = Color(0xFF414B63), outlineVariant = Color(0xFF2A344B),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
)

@Immutable
data class ExtraColors(
    val gold: Color,
    val birthday: Color, val yahrzeit: Color, val barMitzvah: Color, val batMitzvah: Color,
    val wedding: Color, val other: Color, val shabbat: Color, val success: Color,
) {
    fun forType(t: EventType) = when (t) {
        EventType.BIRTHDAY -> birthday; EventType.YAHRZEIT -> yahrzeit; EventType.BAR_MITZVAH -> barMitzvah
        EventType.BAT_MITZVAH -> batMitzvah; EventType.WEDDING -> wedding; EventType.OTHER -> other
    }
}

private val LightExtra = ExtraColors(
    gold = Color(0xFFD4A72C), birthday = Color(0xFFD4A72C), yahrzeit = Color(0xFF64748B), barMitzvah = Color(0xFF2563EB),
    batMitzvah = Color(0xFFB4418E), wedding = Color(0xFFD9466F), other = Color(0xFF0F9D8A),
    shabbat = Color(0x800B3FAE).copy(alpha = 0.05f), success = Color(0xFF2E7D32),
)
private val DarkExtra = ExtraColors(
    gold = Color(0xFFF2C75C), birthday = Color(0xFFF2C75C), yahrzeit = Color(0xFFA3B1C6), barMitzvah = Color(0xFF8FB4FF),
    batMitzvah = Color(0xFFF0A6D6), wedding = Color(0xFFFF9DB5), other = Color(0xFF5FD6C4),
    shabbat = Color(0xFF9DB8FF).copy(alpha = 0.06f), success = Color(0xFF81C784),
)

val ListColors = listOf(
    Color(0xFF0B3FAE), Color(0xFFD4A72C), Color(0xFF0F9D8A), Color(0xFFD9466F),
    Color(0xFF5B4B8A), Color(0xFFE0792B), Color(0xFF3B8E3B), Color(0xFF64748B),
)

val LocalExtraColors = staticCompositionLocalOf { LightExtra }

private fun rubik(w: Int) = Font(R.font.rubik, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
val Rubik = FontFamily(rubik(400), rubik(500), rubik(600), rubik(700))

private fun style(size: Int, line: Int, w: Int) =
    TextStyle(fontFamily = Rubik, fontSize = size.sp, lineHeight = line.sp, fontWeight = FontWeight(w))

private val AppTypography = Typography(
    displaySmall = style(34, 40, 600),
    headlineLarge = style(30, 36, 600),
    headlineMedium = style(26, 32, 600),
    headlineSmall = style(22, 28, 600),
    titleLarge = style(20, 26, 600),
    titleMedium = style(17, 24, 500),
    titleSmall = style(15, 20, 500),
    bodyLarge = style(16, 24, 400),
    bodyMedium = style(14, 20, 400),
    bodySmall = style(12, 16, 400),
    labelLarge = style(14, 20, 500),
    labelMedium = style(12, 16, 500),
    labelSmall = style(11, 14, 500),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun KalendarTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalExtraColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object KalendarColors {
    val extra: ExtraColors @Composable get() = LocalExtraColors.current
}
