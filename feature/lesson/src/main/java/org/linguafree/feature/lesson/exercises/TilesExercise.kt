package org.linguafree.feature.lesson.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.linguafree.core.domain.learning.LearnerAnswer
import org.linguafree.core.domain.model.Direction
import org.linguafree.core.domain.model.ExerciseType
import org.linguafree.core.ui.components.ChoiceCard
import org.linguafree.core.ui.components.ChoiceState
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.theme.LfTheme

/** 1 and 5a: translate (or write what you hear) by tapping or dragging word tiles. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TilesExercise(scope: ExerciseScope) {
    val ex = scope.exercise
    val c = LfTheme.colors
    val selected = remember(ex.id) { mutableStateListOf<Int>() }
    val bounds = remember(ex.id) { mutableStateMapOf<String, Rect>() }
    var answerArea by remember { mutableStateOf(Rect.Zero) }

    fun publish() = scope.answer(if (selected.isEmpty()) null else LearnerAnswer.Tiles(selected.map { ex.options[it].text }))
    fun add(i: Int) { if (!scope.locked && i !in selected) { selected.add(i); scope.tap(); publish() } }
    fun remove(i: Int) { if (!scope.locked) { selected.remove(i); scope.tap(); publish() } }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Instruction(ex.displayInstruction)
        if (ex.type == ExerciseType.ListenTiles) {
            ListenButtons(scope, ex.audio ?: ex.answers.first())
        } else {
            FoxPrompt(scope, ex.text, FoxEmotion.Waiting, withAudio = if (ex.direction == Direction.EnToRu) ex.audio ?: ex.text else null)
        }

        // Answer area with writing lines.
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp)
                .onGloballyPositioned { answerArea = it.boundsInRoot() }
                .testTag("tiles-answer"),
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 50.dp), verticalArrangement = Arrangement.spacedBy(54.dp)) {
                repeat(2) { Box(Modifier.fillMaxWidth().height(2.dp).background(c.border)) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selected.forEach { i ->
                    DraggableTile(
                        text = ex.options[i].text,
                        key = "a$i",
                        enabled = !scope.locked,
                        bounds = bounds,
                        onTap = { remove(i) },
                        onDrop = { center -> if (!answerArea.contains(center)) remove(i) },
                    )
                }
            }
        }

        // Word bank: used tiles leave a grey placeholder of the same size.
        FlowRow(
            Modifier.fillMaxWidth().testTag("tiles-bank"),
            horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ex.options.forEachIndexed { i, option ->
                if (i in selected) {
                    Box(Modifier.padding(top = 4.dp).clip(RoundedCornerShape(14.dp)).background(c.locked)) {
                        Text(option.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.alpha(0f).padding(horizontal = 14.dp, vertical = 12.dp))
                    }
                } else {
                    DraggableTile(
                        text = option.text,
                        key = "b$i",
                        enabled = !scope.locked,
                        bounds = bounds,
                        onTap = { add(i) },
                        onDrop = { center -> if (answerArea.contains(center)) add(i) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggableTile(
    text: String,
    key: String,
    enabled: Boolean,
    bounds: MutableMap<String, Rect>,
    onTap: () -> Unit,
    onDrop: (Offset) -> Unit,
) {
    var drag by remember(key) { mutableStateOf(Offset.Zero) }
    var dragging by remember(key) { mutableStateOf(false) }
    Box(
        Modifier
            .zIndex(if (dragging) 10f else 0f)
            .graphicsLayer { translationX = drag.x; translationY = drag.y; scaleX = if (dragging) 1.06f else 1f; scaleY = scaleX }
            .onGloballyPositioned { if (!dragging) bounds[key] = it.boundsInRoot() }
            .pointerInput(key, enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        val b = bounds[key]
                        if (b != null) onDrop(b.center + drag)
                        drag = Offset.Zero
                        dragging = false
                    },
                    onDragCancel = { drag = Offset.Zero; dragging = false },
                    onDrag = { change, amount -> change.consume(); drag += amount },
                )
            }
            .testTag("tile:$text"),
    ) {
        ChoiceCard(
            onClick = onTap,
            state = if (enabled) ChoiceState.Idle else ChoiceState.Disabled,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(text, style = MaterialTheme.typography.titleMedium, color = LfTheme.colors.text)
        }
    }
}
