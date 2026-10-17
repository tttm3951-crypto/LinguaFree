package org.linguafree.core.domain.learning

import org.linguafree.core.domain.model.Exercise
import org.linguafree.core.domain.model.ExerciseType

/** What the learner produced for an exercise. */
sealed interface LearnerAnswer {
    /** A tapped option (choices, pictures, fill-the-gap). */
    data class Choice(val text: String) : LearnerAnswer
    /** Typed text (translation or dictation). */
    data class Typed(val text: String) : LearnerAnswer
    /** Assembled word tiles. */
    data class Tiles(val words: List<String>) : LearnerAnswer
    /** Recognised speech alternatives with the best similarity score. */
    data class Spoken(val alternatives: List<String>) : LearnerAnswer
    /** Self-checking exercises (pairs, dialogue) report their own outcome. */
    data class Completed(val mistakes: Int) : LearnerAnswer
}

/** Grades any exercise type in one place, so the rules are the same in lessons, practice and tests. */
object ExerciseGrader {

    fun grade(exercise: Exercise, answer: LearnerAnswer): Verdict {
        val expected = exercise.answers.firstOrNull().orEmpty()
        return when (answer) {
            is LearnerAnswer.Choice -> AnswerChecker.checkExact(answer.text, exercise.answers.ifEmpty { listOf("") })
            is LearnerAnswer.Typed -> when (exercise.type) {
                ExerciseType.ListenType -> AnswerChecker.checkTyped(answer.text, listOf(expected))
                else -> AnswerChecker.checkTyped(answer.text, exercise.answers.ifEmpty { listOf("") })
            }
            is LearnerAnswer.Tiles -> AnswerChecker.checkExact(answer.words.joinToString(" "), exercise.answers.ifEmpty { listOf("") })
            is LearnerAnswer.Spoken -> {
                val best = answer.alternatives.maxOfOrNull { AnswerChecker.speechSimilarity(expected, it) } ?: 0
                if (best >= AnswerChecker.SPEECH_PASS) Verdict.Correct(expected) else Verdict.Wrong(expected)
            }
            is LearnerAnswer.Completed -> {
                // Pairs: two or more mismatches count as a mistake; dialogue: any wrong answer does.
                val limit = if (exercise.type == ExerciseType.MatchPairs) 1 else 0
                if (answer.mistakes <= limit) Verdict.Correct(expected) else Verdict.Wrong(expected)
            }
        }
    }

    /** Best spoken alternative and its score, for the feedback panel. */
    fun bestSpoken(exercise: Exercise, alternatives: List<String>): Pair<String, Int> {
        val expected = exercise.answers.firstOrNull().orEmpty()
        return alternatives.map { it to AnswerChecker.speechSimilarity(expected, it) }.maxByOrNull { it.second } ?: ("" to 0)
    }
}
