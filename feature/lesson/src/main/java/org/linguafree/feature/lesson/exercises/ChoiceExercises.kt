package org.linguafree.feature.lesson.exercises

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.linguafree.core.data.content.ContentLoader
import org.linguafree.core.domain.learning.AnswerChecker
import org.linguafree.core.domain.learning.LearnerAnswer
import org.linguafree.core.domain.model.Direction
import org.linguafree.core.domain.model.ExerciseOption
import org.linguafree.core.ui.components.ChoiceCard
import org.linguafree.core.ui.components.ChoiceState
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.theme.LfTheme

/** Visual state of an option before/after checking. */
private fun optionState(scope: ExerciseScope, option: String, selected: String?): ChoiceState {
    val isAnswer = scope.exercise.answers.any { AnswerChecker.normalize(it) == AnswerChecker.normalize(option) }
    return when {
        !scope.locked -> if (option == selected) ChoiceState.Selected else ChoiceState.Idle
        option == selected && scope.checkedCorrect == true -> ChoiceState.Correct
        option == selected -> ChoiceState.Wrong
        isAnswer && scope.checkedCorrect == false -> ChoiceState.Correct
        else -> ChoiceState.Disabled
    }
}

@Composable
private fun rememberImage(name: String?): Int {
    val context = LocalContext.current
    return remember(name) { if (name == null) 0 else ContentLoader.imageRes(context, name) }
}

/** 3. Pick the picture for a word (4 cards). */
@Composable
fun SelectImageExercise(scope: ExerciseScope) {
    val ex = scope.exercise
    var selected by rememberSaveable(ex.id) { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Instruction("Выберите «${ex.text}»")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ex.options.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { option ->
                        ImageCard(option, optionState(scope, option.text, selected), Modifier.weight(1f)) {
                            if (scope.locked) return@ImageCard
                            selected = option.text
                            scope.speak(option.text)
                            scope.answer(LearnerAnswer.Choice(option.text))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageCard(option: ExerciseOption, state: ChoiceState, modifier: Modifier, onClick: () -> Unit) {
    val image = rememberImage(option.image)
    ChoiceCard(onClick = onClick, state = state, modifier = modifier.testTag("image:${option.text}"), contentPadding = PaddingValues(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (image != 0) Image(painterResource(image), contentDescription = null, modifier = Modifier.fillMaxWidth(0.82f).aspectRatio(1f))
            Text(option.text, style = MaterialTheme.typography.titleMedium, color = LfTheme.colors.text, textAlign = TextAlign.Center)
        }
    }
}

/** 8. Multiple-choice translation (words or sentences). */
@Composable
fun SelectTranslationExercise(scope: ExerciseScope) {
    val ex = scope.exercise
    var selected by rememberSaveable(ex.id) { mutableStateOf<String?>(null) }
    val c = LfTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Instruction(if (ex.direction == Direction.EnToRu) "Что это значит?" else "Как сказать по-английски?")
        FoxPrompt(scope, ex.text, FoxEmotion.Waiting, withAudio = if (ex.direction == Direction.EnToRu) ex.audio ?: ex.text else null)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ex.options.forEachIndexed { i, option ->
                ChoiceCard(
                    onClick = {
                        if (scope.locked) return@ChoiceCard
                        selected = option.text
                        if (ex.direction == Direction.RuToEn) scope.speak(option.text) else scope.tap()
                        scope.answer(LearnerAnswer.Choice(option.text))
                    },
                    state = optionState(scope, option.text, selected),
                    modifier = Modifier.fillMaxWidth().testTag("option:${option.text}"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(30.dp).border(2.dp, c.border, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = c.textMuted) }
                        Text(option.text, style = MaterialTheme.typography.titleMedium, color = c.text)
                    }
                }
            }
        }
    }
}

/** 6. Fill the gap by choosing the missing word. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FillGapExercise(scope: ExerciseScope) {
    val ex = scope.exercise
    val c = LfTheme.colors
    var selected by rememberSaveable(ex.id) { mutableStateOf<String?>(null) }
    val parts = ex.text.split("___", limit = 2)
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Instruction(ex.displayInstruction)
        ex.subtitle?.let { Text("«$it»", style = MaterialTheme.typography.bodyLarge, color = c.textMuted) }
        FlowRow(verticalArrangement = Arrangement.Center) {
            if (parts[0].isNotBlank()) TappableText(parts[0].trimEnd(), scope, MaterialTheme.typography.headlineSmall)
            Box(
                Modifier
                    .padding(horizontal = 8.dp)
                    .widthIn(min = 72.dp)
                    .border(0.dp, c.border)
                    .background(if (selected != null) c.gems.copy(alpha = 0.12f) else c.surfaceRaised, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(selected ?: " ", style = MaterialTheme.typography.headlineSmall, color = c.gems)
            }
            if (parts.size > 1 && parts[1].isNotBlank()) TappableText(parts[1].trimStart(), scope, MaterialTheme.typography.headlineSmall)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ex.options.forEach { option ->
                ChoiceCard(
                    onClick = {
                        if (scope.locked) return@ChoiceCard
                        selected = option.text
                        scope.tap()
                        scope.answer(LearnerAnswer.Choice(option.text))
                    },
                    state = optionState(scope, option.text, selected),
                    modifier = Modifier.testTag("option:${option.text}"),
                ) { Text(option.text, style = MaterialTheme.typography.titleMedium, color = c.text) }
            }
        }
    }
}
