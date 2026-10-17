package org.linguafree.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.linguafree.core.ui.theme.LfTheme

/** Depth of the "physical" bottom edge. It disappears while pressed — the key presses down. */
val PressDepth: Dp = 4.dp

/**
 * Base of every LinguaFree button and tappable card: a flat face sitting on a darker lip.
 * Pressing moves the face down by [PressDepth], hiding the lip.
 */
@Composable
fun Pressable3D(
    onClick: () -> Unit,
    face: Color,
    lip: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    enabled: Boolean = true,
    role: Role = Role.Button,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val drop by animateDpAsState(if (pressed && enabled) PressDepth else 0.dp, tween(60), label = "press")
    Box(
        modifier
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = role, onClick = onClick)
            .padding(top = drop),
        propagateMinConstraints = true,
    ) {
        // Lip: visible strip under the face.
        Box(
            Modifier
                .matchParentSize()
                .padding(top = PressDepth)
                .background(lip, shape)
        )
        val faceMod = Modifier
            .padding(bottom = PressDepth - drop)
            .background(face, shape)
            .let { if (borderColor != null) it.border(2.dp, borderColor, shape) else it }
            .padding(contentPadding)
        Box(faceMod, contentAlignment = contentAlignment, content = content)
    }
}

enum class LfButtonStyle { Primary, Secondary, Danger, Gems, Gold, Correct, Ghost }

@Composable
fun LfButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: LfButtonStyle = LfButtonStyle.Primary,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = LfTheme.colors
    val (face, lip, textColor, border) = when {
        !enabled -> Quad(c.locked, c.lockedLip, c.textMuted, null)
        style == LfButtonStyle.Primary -> Quad(c.primary, c.primaryLip, Color.White, null)
        style == LfButtonStyle.Correct -> Quad(c.primary, c.primaryLip, Color.White, null)
        style == LfButtonStyle.Danger -> Quad(c.error, c.errorLip, Color.White, null)
        style == LfButtonStyle.Gems -> Quad(c.gems, c.gemsLip, Color.White, null)
        style == LfButtonStyle.Gold -> Quad(c.xp, c.xpLip, Color(0xFF4A3500), null)
        style == LfButtonStyle.Ghost -> Quad(Color.Transparent, Color.Transparent, c.primary, null)
        else -> Quad(c.surface, c.border, c.primary, c.border)
    }
    Pressable3D(
        onClick = onClick,
        face = face,
        lip = lip,
        borderColor = border,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke()
            Text(
                text.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class Quad(val face: Color, val lip: Color, val text: Color, val border: Color?)

/** Visual state of an answer option card. */
enum class ChoiceState { Idle, Selected, Correct, Wrong, Disabled }

/** Bordered option card (answers, tiles, pairs) with the same 3D press. */
@Composable
fun ChoiceCard(
    onClick: () -> Unit,
    state: ChoiceState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LfTheme.colors
    val (face, lip, border) = when (state) {
        ChoiceState.Idle -> Triple(c.surface, c.border, c.border)
        ChoiceState.Selected -> Triple(c.gems.copy(alpha = if (c.isDark) 0.22f else 0.12f), c.gems, c.gems)
        ChoiceState.Correct -> Triple(c.primarySoft, c.primary, c.primary)
        ChoiceState.Wrong -> Triple(c.errorSoft, c.error, c.error)
        ChoiceState.Disabled -> Triple(c.surfaceRaised, c.border, c.border)
    }
    Pressable3D(
        onClick = onClick,
        face = face,
        lip = lip,
        borderColor = border,
        enabled = state != ChoiceState.Disabled,
        shape = shape,
        modifier = modifier,
        contentPadding = contentPadding,
        content = content,
    )
}

/** Small helper to offset content without layout changes (used by badges). */
fun Modifier.nudge(x: Dp = 0.dp, y: Dp = 0.dp) = this.offset(x, y)
