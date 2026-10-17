package org.linguafree.feature.lesson.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import org.linguafree.core.domain.learning.LearnerAnswer
import org.linguafree.core.domain.model.Direction
import org.linguafree.core.domain.model.ExerciseType
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.theme.LfTheme

/** 2 and 5b: type a translation or what you heard. Small typos are tolerated by the checker. */
@Composable
fun TypeExercise(scope: ExerciseScope) {
    val ex = scope.exercise
    val c = LfTheme.colors
    var text by rememberSaveable(ex.id) { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val listening = ex.type == ExerciseType.ListenType
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Instruction(ex.displayInstruction)
        if (listening) {
            ListenButtons(scope, ex.audio ?: ex.answers.first())
        } else {
            FoxPrompt(scope, ex.text, FoxEmotion.Thinking, withAudio = if (ex.direction == Direction.EnToRu) ex.audio ?: ex.text else null)
        }
        OutlinedTextField(
            value = text,
            onValueChange = {
                if (scope.locked) return@OutlinedTextField
                text = it.take(200)
                scope.answer(if (text.isBlank()) null else LearnerAnswer.Typed(text))
            },
            readOnly = scope.locked,
            placeholder = { Text(if (ex.answerIsEnglish) "Напишите по-английски" else "Напишите по-русски", color = c.textMuted) },
            textStyle = MaterialTheme.typography.titleMedium,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.gems, unfocusedBorderColor = c.border,
                focusedContainerColor = c.surfaceRaised, unfocusedContainerColor = c.surfaceRaised,
                focusedTextColor = c.text, unfocusedTextColor = c.text,
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, autoCorrectEnabled = false, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp).focusRequester(focus).testTag("type-input"),
        )
    }
    LaunchedEffect(ex.id) { if (!listening) runCatching { focus.requestFocus() } }
}
