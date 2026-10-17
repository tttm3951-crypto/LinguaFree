package org.linguafree.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.linguafree.core.ui.R
import org.linguafree.core.ui.theme.LfTheme

/** Icon + number, e.g. 🔥 12. */
@Composable
fun StatChip(
    @DrawableRes icon: Int,
    value: String,
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(26.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

/** Top bar of the main screen: course flag · streak · gems · hearts. */
@Composable
fun StatsTopBar(
    streak: Int,
    studiedToday: Boolean,
    gems: Int,
    hearts: Int,
    unlimitedHearts: Boolean,
    modifier: Modifier = Modifier,
    onStreakClick: () -> Unit = {},
    onGemsClick: () -> Unit = {},
    onHeartsClick: () -> Unit = {},
) {
    val c = LfTheme.colors
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatChip(R.drawable.ic_flag_en, "", c.text, "Курс: английский для русскоязычных")
        StatChip(
            if (studiedToday) R.drawable.ic_flame else R.drawable.ic_flame_off,
            streak.toString(), if (studiedToday) c.streak else c.textMuted,
            "Серия: $streak дн.", onClick = onStreakClick,
        )
        StatChip(R.drawable.ic_gem, gems.toString(), c.gems, "Кристаллы: $gems", onClick = onGemsClick)
        if (unlimitedHearts) {
            StatChip(R.drawable.ic_heart_infinite, "∞", c.heart, "Режим без сердец", onClick = onHeartsClick)
        } else {
            StatChip(
                if (hearts > 0) R.drawable.ic_heart else R.drawable.ic_heart_empty,
                hearts.toString(), if (hearts > 0) c.heart else c.textMuted,
                "Сердца: $hearts из 5", onClick = onHeartsClick,
            )
        }
    }
    HorizontalDivider(thickness = 2.dp, color = c.border)
}

/** Thick rounded progress bar with a glossy highlight, animated. */
@Composable
fun LfProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = LfTheme.colors.primary,
    height: Dp = 16.dp,
    label: String = "Прогресс",
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), spring(stiffness = 300f), label = "progress")
    val c = LfTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(c.border)
            .semantics { contentDescription = "$label: ${(progress * 100).toInt()}%" }
    ) {
        if (animated > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(color)
            ) {
                Box(
                    Modifier
                        .padding(start = 8.dp, end = 8.dp, top = height * 0.2f)
                        .fillMaxWidth()
                        .height(height * 0.22f)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
        }
    }
}

/** Plain icon from the LinguaFree set, optionally tinted (for `_mono` icons). */
@Composable
fun LfIcon(@DrawableRes icon: Int, modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color? = null, contentDescription: String? = null) {
    Image(
        painterResource(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = tint?.let { androidx.compose.ui.graphics.ColorFilter.tint(it) },
    )
}
