package org.linguafree.feature.lesson.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.linguafree.core.ui.R
import org.linguafree.core.ui.components.LfIcon
import org.linguafree.core.ui.components.Pressable3D
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.mascot.FoxMascot
import org.linguafree.core.ui.theme.LfTheme

@Composable
fun Instruction(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.headlineSmall, color = LfTheme.colors.text, modifier = modifier)
}

/** Splits text into words and punctuation, keeping the original characters. */
internal fun splitWords(text: String): List<String> = Regex("[\\p{L}\\p{N}'’-]+|[^\\p{L}\\p{N}'’-]+").findAll(text).map { it.value }.toList()

internal fun hintKey(word: String) = word.lowercase().trim('\'', '’', '-').replace('ё', 'е')

/**
 * Sentence whose words show a translation on tap («подсказки»). Words with a hint get a
 * dotted underline. Tapping also speaks English words.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TappableText(
    text: String,
    scope: ExerciseScope,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    speakWords: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val c = LfTheme.colors
    var openIndex by remember(text) { mutableStateOf(-1) }
    FlowRow(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        splitWords(text).forEachIndexed { i, token ->
            val isWord = token.any { it.isLetterOrDigit() }
            if (!isWord) {
                Text(token, style = style, color = c.text)
                return@forEachIndexed
            }
            val hint = scope.hint(token)
            Box {
                Text(
                    token,
                    style = style,
                    color = c.text,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(role = Role.Button) {
                            openIndex = if (openIndex == i) -1 else i
                            if (speakWords && token.any { it in 'a'..'z' || it in 'A'..'Z' }) scope.speak(token)
                        }
                        .drawBehind {
                            if (hint != null) {
                                val y = size.height - 2.dp.toPx()
                                drawLine(
                                    c.textMuted, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                                )
                            }
                        }
                        .semantics { if (hint != null) contentDescription = "$token — $hint" },
                )
                if (openIndex == i) {
                    Popup(
                        alignment = Alignment.TopStart,
                        offset = IntOffset(0, -120),
                        onDismissRequest = { openIndex = -1 },
                        properties = PopupProperties(focusable = false),
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.surface)
                                .border(2.dp, c.border, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(hint ?: "Нет подсказки", style = MaterialTheme.typography.bodyMedium, color = if (hint != null) c.text else c.textMuted)
                        }
                    }
                }
            }
        }
    }
}

/** Fox with a speech bubble — the prompt is "said" by the mascot. */
@Composable
fun FoxPrompt(scope: ExerciseScope, text: String, emotion: FoxEmotion, withAudio: String?, modifier: Modifier = Modifier) {
    val c = LfTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FoxMascot(emotion, Modifier.width(96.dp), breathing = true)
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, c.border, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (withAudio != null) {
                SpeakerButton(onClick = { scope.speak(withAudio) }, size = 40)
            }
            TappableText(text, scope, MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun SpeakerButton(onClick: () -> Unit, size: Int = 64, slow: Boolean = false) {
    val c = LfTheme.colors
    Pressable3D(
        onClick = onClick,
        face = c.gems,
        lip = c.gemsLip,
        shape = RoundedCornerShape(if (size > 50) 20.dp else 12.dp),
        contentPadding = PaddingValues(if (size > 50) 14.dp else 6.dp),
        modifier = Modifier
            .size(size.dp)
            .semantics { contentDescription = if (slow) "Прослушать медленно" else "Прослушать" },
    ) {
        LfIcon(if (slow) R.drawable.ic_turtle else R.drawable.ic_speaker, size = (size * 0.5f).dp)
    }
}

/** Big «обычно» + «медленно» listening buttons. */
@Composable
fun ListenButtons(scope: ExerciseScope, text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
        SpeakerButton(onClick = { scope.speak(text) }, size = 104)
        Box(Modifier.width(20.dp))
        SpeakerButton(onClick = { scope.speak(text, slow = true) }, size = 68, slow = true)
    }
}

@Composable
fun CantDoNowButton(text: String, onClick: () -> Unit) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = LfTheme.colors.textMuted,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

/** Thin line used as a writing guide in answer areas. */
@Composable
fun GuideLine(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(top = 2.dp).height(2.dp).background(LfTheme.colors.border))
}
