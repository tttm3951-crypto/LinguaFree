package org.linguafree.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.linguafree.core.ui.R

/** Rounded grotesque used everywhere: Nunito (SIL OFL 1.1), bundled — no downloadable fonts. */
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

private fun style(size: Int, weight: FontWeight, line: Int = (size * 1.3f).toInt()) =
    TextStyle(fontFamily = Nunito, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

val LfTypography = Typography(
    displayLarge = style(40, FontWeight.Black),
    displayMedium = style(34, FontWeight.Black),
    displaySmall = style(30, FontWeight.Black),
    headlineLarge = style(28, FontWeight.ExtraBold),
    headlineMedium = style(24, FontWeight.ExtraBold),
    headlineSmall = style(21, FontWeight.ExtraBold),
    titleLarge = style(20, FontWeight.ExtraBold),
    titleMedium = style(17, FontWeight.Bold),
    titleSmall = style(15, FontWeight.Bold),
    bodyLarge = style(17, FontWeight.SemiBold, 24),
    bodyMedium = style(15, FontWeight.SemiBold, 21),
    bodySmall = style(13, FontWeight.SemiBold, 18),
    labelLarge = style(16, FontWeight.ExtraBold),
    labelMedium = style(14, FontWeight.Bold),
    labelSmall = style(12, FontWeight.Bold),
)

val LfShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** How the user chose the light/dark mode in settings. */
enum class DarkMode { System, Light, Dark }

@Composable
fun LinguaFreeTheme(
    darkMode: DarkMode = DarkMode.System,
    accent: ThemeAccent = ThemeAccent.Forest,
    content: @Composable () -> Unit,
) {
    val dark = when (darkMode) {
        DarkMode.System -> isSystemInDarkTheme()
        DarkMode.Light -> false
        DarkMode.Dark -> true
    }
    val lf = remember(dark, accent) { if (dark) darkLfColors(accent) else lightLfColors(accent) }
    val scheme = if (dark) {
        darkColorScheme(
            primary = lf.primary, onPrimary = Color.White, primaryContainer = lf.primarySoft,
            secondary = lf.gems, onSecondary = Color.White,
            tertiary = lf.xp, error = lf.error, onError = Color.White,
            background = lf.background, onBackground = lf.text,
            surface = lf.surface, onSurface = lf.text, surfaceVariant = lf.surfaceRaised,
            onSurfaceVariant = lf.textMuted, outline = lf.border, outlineVariant = lf.borderLip,
            surfaceContainer = lf.surface, surfaceContainerHigh = lf.surfaceRaised,
        )
    } else {
        lightColorScheme(
            primary = lf.primary, onPrimary = Color.White, primaryContainer = lf.primarySoft,
            secondary = lf.gems, onSecondary = Color.White,
            tertiary = lf.xp, error = lf.error, onError = Color.White,
            background = lf.background, onBackground = lf.text,
            surface = lf.surface, onSurface = lf.text, surfaceVariant = lf.surfaceRaised,
            onSurfaceVariant = lf.textMuted, outline = lf.border, outlineVariant = lf.borderLip,
            surfaceContainer = lf.surface, surfaceContainerHigh = lf.surfaceRaised,
        )
    }
    CompositionLocalProvider(LocalLfColors provides lf) {
        MaterialTheme(colorScheme = scheme, typography = LfTypography, shapes = LfShapes, content = content)
    }
}

/** Shorthand: `LfTheme.colors.xp`. */
object LfTheme {
    val colors: LfColors
        @Composable @ReadOnlyComposable get() = LocalLfColors.current
}
