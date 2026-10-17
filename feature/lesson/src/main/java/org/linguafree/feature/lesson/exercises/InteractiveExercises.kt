package org.linguafree.feature.lesson.exercises

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.linguafree.core.domain.learning.LearnerAnswer
import org.linguafree.core.domain.model.SessionKind
import org.linguafree.core.ui.components.ChoiceCard
import org.linguafree.core.ui.components.ChoiceState
import org.linguafree.core.ui.components.LfButton
import org.linguafree.core.ui.components.LfProgressBar
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.mascot.FoxMascot
import org.linguafree.core.ui.theme.LfTheme

/** 4. Match five pairs. In practice mode there is a 60-second timer. */
@Composable
fun MatchPairsExercise(scope: ExerciseScope) {
    val ex = scope.exercise
    val c = LfTheme.colors
    val left = remember(ex.id) { ex.pairs.map { it.en }.shuffled() }
    val right = remember(ex.id) { ex.pairs.map { it.ru }.shuffled() }
    val matched = remember(ex.id) { mutableStateListOf<String>() }
    var pickLeft by remember(ex.id) { mutableStateOf<String?>(null) }
    var pickRight by remember(ex.id) { mutableStateOf<String?>(null) }
    var wrongFlash by remember(ex.id) { mutableStateOf<Pair<String, String>?>(null) }
    var mistakes by remember(ex.id) { mutableIntStateOf(0) }
    val timed = scope.kind == SessionKind.PracticeMatch
    val total = 60
    var secondsLeft by remember(ex.id) { mutableIntStateOf(total) }
    var done by remember(ex.id) { mutableStateOf(false) }

    fun finish(extraMistakes: Int = 0) {
        if (done) return
        done = true
        scope.submit(LearnerAnswer.Completed(mistakes + extraMistakes))
    }

    fun tryMatch() {
        val l = pickLeft ?: return
        val r = pickRight ?: return
        if (ex.pairs.any { it.en == l && it.ru == r }) {
            matched += l
            scope.speak(l)
            if (matched.size == ex.pairs.size) finish()
        } else {
            mistakes++
            wrongFlash = l to r
        }
        pickLeft = null; pickRight = null
    }

    LaunchedEffect(wrongFlash) { if (wrongFlash != null) { delay(500); wrongFlash = null } }
    if (timed) LaunchedEffect(ex.id) {
        while (secondsLeft > 0 && !done) { delay(1000); secondsLeft-- }
        if (!done) finish(extraMistakes = 5)
    }

    fun stateOf(text: String, isLeft: Boolean): ChoiceState {
        val pairDone = if (isLeft) text in matched else ex.pairs.any { it.ru == text && it.en in matched }
        return when {
            pairDone -> ChoiceState.Disabled
            wrongFlash?.let { (l, r) -> if (isLeft) l == text else r == text } == true -> ChoiceState.Wrong
            (if (isLeft) pickLeft else pickRight) == text -> ChoiceState.Selected
            else -> ChoiceState.Idle
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Instruction(ex.displayInstruction)
        if (timed) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⏱ $secondsLeft с", style = MaterialTheme.typography.titleMedium, color = if (secondsLeft <= 10) c.error else c.text)
                LfProgressBar(secondsLeft / total.toFloat(), color = if (secondsLeft <= 10) c.error else c.gems, height = 12.dp, label = "Время")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                left.forEach { t ->
                    ChoiceCard(
                        onClick = { if (!scope.locked && t !in matched) { pickLeft = t; scope.speak(t); tryMatch() } },
                        state = stateOf(t, true),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).testTag("pair-en:$t"),
                    ) { Text(t, style = MaterialTheme.typography.titleMedium, color = c.text, textAlign = TextAlign.Center) }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                right.forEach { t ->
                    ChoiceCard(
                        onClick = { if (!scope.locked && ex.pairs.none { it.ru == t && it.en in matched }) { pickRight = t; scope.tap(); tryMatch() } },
                        state = stateOf(t, false),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).testTag("pair-ru:$t"),
                    ) { Text(t, style = MaterialTheme.typography.titleMedium, color = c.text, textAlign = TextAlign.Center) }
                }
            }
        }
    }
}

/** 9. Mini dialogue / story with comprehension questions. */
@Composable
fun DialogueExercise(scope: ExerciseScope) {
    val ex = scope.exercise
    val c = LfTheme.colors
    var shown by remember(ex.id) { mutableIntStateOf(1) }
    var question by remember(ex.id) { mutableIntStateOf(0) }
    var picked by remember(ex.id) { mutableStateOf<Int?>(null) }
    var mistakes by remember(ex.id) { mutableIntStateOf(0) }
    var revealRu by remember(ex.id) { mutableStateOf(setOf<Int>()) }
    val speakers = remember(ex.id) { ex.lines.map { it.speaker }.distinct() }
    val allShown = shown >= ex.lines.size

    LaunchedEffect(shown) { ex.lines.getOrNull(shown - 1)?.let { scope.speak(it.en) } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Instruction(ex.text.ifBlank { ex.displayInstruction })
        ex.lines.take(shown).forEachIndexed { i, line ->
            val mine = speakers.indexOf(line.speaker) % 2 == 1
            AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { it / 3 }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
                    if (!mine) FoxMascot(if (i % 2 == 0) FoxEmotion.Waiting else FoxEmotion.Thinking, Modifier.size(52.dp), breathing = false)
                    Column(
                        Modifier
                            .padding(start = 6.dp)
                            .widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (mine) c.gems.copy(alpha = 0.14f) else c.surfaceRaised)
                            .clickable { scope.speak(line.en); revealRu = if (i in revealRu) revealRu - i else revealRu + i }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        if (line.speaker.isNotBlank()) Text(line.speaker, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                        TappableText(line.en, scope, MaterialTheme.typography.titleMedium)
                        if (i in revealRu && line.ru.isNotBlank()) Text(line.ru, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    }
                }
            }
        }
        if (!allShown) {
            LfButton("Дальше", onClick = { shown++ }, modifier = Modifier.fillMaxWidth())
        } else if (question < ex.questions.size) {
            val q = ex.questions[question]
            Box(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(q.prompt, style = MaterialTheme.typography.titleLarge, color = c.text)
            }
            q.options.forEachIndexed { i, option ->
                ChoiceCard(
                    onClick = {
                        if (picked != null || scope.locked) return@ChoiceCard
                        picked = i
                        if (i == q.answer) scope.tap() else mistakes++
                    },
                    state = when {
                        picked == null -> ChoiceState.Idle
                        i == q.answer -> ChoiceState.Correct
                        i == picked -> ChoiceState.Wrong
                        else -> ChoiceState.Disabled
                    },
                    modifier = Modifier.fillMaxWidth().testTag("dialogue-option:$option"),
                    contentPadding = PaddingValues(14.dp),
                ) { Text(option, style = MaterialTheme.typography.titleMedium, color = c.text) }
            }
            if (picked != null) {
                LfButton(
                    if (question == ex.questions.lastIndex) "Готово" else "Следующий вопрос",
                    onClick = {
                        if (question == ex.questions.lastIndex) scope.submit(LearnerAnswer.Completed(mistakes))
                        else { question++; picked = null }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
