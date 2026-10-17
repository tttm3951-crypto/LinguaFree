package org.linguafree.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** LinguaFree's own palette. Bright flat colors; each accent has a darker "lip" used for 3D buttons. */
object LfPalette {
    val Fern = Color(0xFF4CC417)
    val FernLip = Color(0xFF3A9A0F)
    val FernSoft = Color(0xFFDDF6CF)

    val Berry = Color(0xFFF2464B)
    val BerryLip = Color(0xFFC4313A)
    val BerrySoft = Color(0xFFFFDEDF)

    val Honey = Color(0xFFFFC21F)
    val HoneyLip = Color(0xFFD99A00)

    val Ember = Color(0xFFFF8A1F)
    val EmberLip = Color(0xFFD96A06)

    val Ice = Color(0xFF2EB8F2)
    val IceLip = Color(0xFF1C8FC4)
    val IceSoft = Color(0xFFD8F2FD)

    val Plum = Color(0xFFA56BF7)
    val PlumLip = Color(0xFF7F48D1)

    val Heart = Color(0xFFFF4B6E)
    val Sky = Color(0xFF55A9FF)
    val Mint = Color(0xFF34D1A2)
}

/** Selectable color theme — "темы оформления" bought for gems change the primary accent only. */
enum class ThemeAccent(val title: String, val primary: Color, val primaryLip: Color, val primarySoft: Color) {
    Forest("Лес", LfPalette.Fern, LfPalette.FernLip, LfPalette.FernSoft),
    Ocean("Океан", Color(0xFF1FA9E6), Color(0xFF167FAF), Color(0xFFD5F0FC)),
    Sunset("Закат", Color(0xFFFF7A45), Color(0xFFD45A2A), Color(0xFFFFE3D6)),
    Lilac("Сирень", Color(0xFF9B6BF2), Color(0xFF7448C7), Color(0xFFEDE3FD)),
}

/** Semantic colors that Material3's scheme doesn't cover. */
@Immutable
data class LfColors(
    val primary: Color,
    val primaryLip: Color,
    val primarySoft: Color,
    val error: Color = LfPalette.Berry,
    val errorLip: Color = LfPalette.BerryLip,
    val errorSoft: Color,
    val xp: Color = LfPalette.Honey,
    val xpLip: Color = LfPalette.HoneyLip,
    val streak: Color = LfPalette.Ember,
    val gems: Color = LfPalette.Ice,
    val gemsLip: Color = LfPalette.IceLip,
    val heart: Color = LfPalette.Heart,
    val legendary: Color = LfPalette.Honey,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val border: Color,
    val borderLip: Color,
    val text: Color,
    val textMuted: Color,
    val locked: Color,
    val lockedLip: Color,
    val correctPanel: Color,
    val wrongPanel: Color,
    val isDark: Boolean,
)

fun lightLfColors(accent: ThemeAccent) = LfColors(
    primary = accent.primary,
    primaryLip = accent.primaryLip,
    primarySoft = accent.primarySoft,
    errorSoft = LfPalette.BerrySoft,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF7F7F7),
    border = Color(0xFFE5E5E5),
    borderLip = Color(0xFFD0D0D0),
    text = Color(0xFF3B3B3B),
    textMuted = Color(0xFF8A8A8A),
    locked = Color(0xFFE5E5E5),
    lockedLip = Color(0xFFC9C9C9),
    correctPanel = Color(0xFFD9F7C6),
    wrongPanel = Color(0xFFFFE0E1),
    isDark = false,
)

fun darkLfColors(accent: ThemeAccent) = LfColors(
    primary = accent.primary,
    primaryLip = accent.primaryLip,
    primarySoft = accent.primary.copy(alpha = 0.22f),
    errorSoft = Color(0xFF4A2327),
    background = Color(0xFF11181C),
    surface = Color(0xFF162127),
    surfaceRaised = Color(0xFF1E2B32),
    border = Color(0xFF34434B),
    borderLip = Color(0xFF26333A),
    text = Color(0xFFF1F6F8),
    textMuted = Color(0xFF94A6AE),
    locked = Color(0xFF37464E),
    lockedLip = Color(0xFF26333A),
    correctPanel = Color(0xFF1F3A1C),
    wrongPanel = Color(0xFF41201F),
    isDark = true,
)

val LocalLfColors = staticCompositionLocalOf { lightLfColors(ThemeAccent.Forest) }
