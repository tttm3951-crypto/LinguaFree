package org.linguafree.feature.lesson.exercises

import androidx.compose.runtime.Composable
import org.linguafree.core.data.platform.SpeechInput
import org.linguafree.core.domain.learning.LearnerAnswer
import org.linguafree.core.domain.model.Exercise
import org.linguafree.core.domain.model.SessionKind

/**
 * The common interface of all exercise composables. Every type gets the same scope and
 * reports progress through [answer] (enables «Проверить») or [submit] (self-checking types).
 */
interface ExerciseScope {
    val exercise: Exercise
    val kind: SessionKind
    /** True after «Проверить»: inputs are frozen and the right answer may be highlighted. */
    val locked: Boolean
    /** Whether the checked answer was right (null before checking). */
    val checkedCorrect: Boolean?
    val speechInput: SpeechInput
    fun hint(word: String): String?
    fun speak(text: String, slow: Boolean = false)
    fun answer(answer: LearnerAnswer?)
    fun submit(answer: LearnerAnswer)
    fun cantDoNow(speaking: Boolean)
    fun tap()
}

/** Signature shared by every exercise type. */
typealias ExerciseContent = @Composable (ExerciseScope) -> Unit
