package org.linguafree.feature.lesson.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.linguafree.core.data.platform.SpeechInput
import org.linguafree.core.domain.learning.LearnerAnswer
import org.linguafree.core.domain.model.Exercise
import org.linguafree.core.domain.model.ExerciseType
import org.linguafree.core.domain.model.SessionKind

private val TilesContent: ExerciseContent = { s -> TilesExercise(s) }
private val TypeContent: ExerciseContent = { s -> TypeExercise(s) }
private val SelectImageContent: ExerciseContent = { s -> SelectImageExercise(s) }
private val MatchContent: ExerciseContent = { s -> MatchPairsExercise(s) }
private val FillGapContent: ExerciseContent = { s -> FillGapExercise(s) }
private val SpeakContent: ExerciseContent = { s -> SpeakExercise(s) }
private val SelectTranslationContent: ExerciseContent = { s -> SelectTranslationExercise(s) }
private val DialogueContent: ExerciseContent = { s -> DialogueExercise(s) }

/** Exercise type → its composable. Every entry has the same [ExerciseContent] signature. */
fun exerciseContent(type: ExerciseType): ExerciseContent = when (type) {
    ExerciseType.Tiles, ExerciseType.ListenTiles -> TilesContent
    ExerciseType.TypeTranslation, ExerciseType.ListenType -> TypeContent
    ExerciseType.SelectImage -> SelectImageContent
    ExerciseType.MatchPairs -> MatchContent
    ExerciseType.FillGap -> FillGapContent
    ExerciseType.Speak -> SpeakContent
    ExerciseType.SelectTranslation -> SelectTranslationContent
    ExerciseType.Dialogue -> DialogueContent
}

/** Types that check themselves (no «Проверить» button). */
val SelfCheckingTypes = setOf(ExerciseType.MatchPairs, ExerciseType.Dialogue, ExerciseType.Speak)

@Composable
fun ExerciseHost(
    exercise: Exercise,
    kind: SessionKind,
    locked: Boolean,
    checkedCorrect: Boolean?,
    hints: Map<String, String>,
    speechInput: SpeechInput,
    onSpeak: (String, Boolean) -> Unit,
    onAnswer: (LearnerAnswer?) -> Unit,
    onSubmit: (LearnerAnswer) -> Unit,
    onCantDoNow: (Boolean) -> Unit,
    onTap: () -> Unit,
) {
    val ex = exercise
    val sessionKind = kind
    val isLocked = locked
    val verdict = checkedCorrect
    val input = speechInput
    val scope = remember(ex.id, isLocked, verdict, hints) {
        object : ExerciseScope {
            override val exercise: Exercise = ex
            override val kind: SessionKind = sessionKind
            override val locked: Boolean = isLocked
            override val checkedCorrect: Boolean? = verdict
            override val speechInput: SpeechInput = input
            override fun hint(word: String): String? {
                val k = hintKey(word)
                return ex.hints[word] ?: ex.hints[k] ?: hints[k]
            }
            override fun speak(text: String, slow: Boolean) = onSpeak(text, slow)
            override fun answer(answer: LearnerAnswer?) = onAnswer(answer)
            override fun submit(answer: LearnerAnswer) = onSubmit(answer)
            override fun cantDoNow(speaking: Boolean) = onCantDoNow(speaking)
            override fun tap() = onTap()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        exerciseContent(exercise.type)(scope)
        if (exercise.type.isListening && !locked) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CantDoNowButton("Не могу слушать сейчас") { onCantDoNow(false) }
            }
        }
    }
}
