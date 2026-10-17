package org.linguafree.core.domain

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.linguafree.core.domain.learning.ExerciseGrader
import org.linguafree.core.domain.learning.LearnerAnswer
import org.linguafree.core.domain.learning.Verdict
import org.linguafree.core.domain.model.Exercise
import org.linguafree.core.domain.model.ExerciseType

class ExerciseGraderTest {
    private fun ex(type: ExerciseType, vararg answers: String) = Exercise("e", "n", 0, type, "t", answers = answers.toList())

    @Test
    fun `tiles are compared exactly but ignore punctuation`() {
        val e = ex(ExerciseType.Tiles, "Hello, Anna!", "Hi, Anna!")
        assertTrue(ExerciseGrader.grade(e, LearnerAnswer.Tiles(listOf("Hi", "Anna"))).correct)
        assertFalse(ExerciseGrader.grade(e, LearnerAnswer.Tiles(listOf("Anna", "Hi"))).correct)
    }

    @Test
    fun `listen type accepts only what was said, with typos`() {
        val e = ex(ExerciseType.ListenType, "I like apples")
        assertTrue(ExerciseGrader.grade(e, LearnerAnswer.Typed("i like aples")) is Verdict.Typo)
    }

    @Test
    fun `speech passes with most words recognised`() {
        val e = ex(ExerciseType.Speak, "Nice to meet you, Anna!")
        assertTrue(ExerciseGrader.grade(e, LearnerAnswer.Spoken(listOf("nice to meet you anna"))).correct)
        assertFalse(ExerciseGrader.grade(e, LearnerAnswer.Spoken(listOf("good morning"))).correct)
    }

    @Test
    fun `pairs allow one mismatch, dialogues none`() {
        assertTrue(ExerciseGrader.grade(ex(ExerciseType.MatchPairs), LearnerAnswer.Completed(1)).correct)
        assertFalse(ExerciseGrader.grade(ex(ExerciseType.MatchPairs), LearnerAnswer.Completed(2)).correct)
        assertFalse(ExerciseGrader.grade(ex(ExerciseType.Dialogue), LearnerAnswer.Completed(1)).correct)
    }
}
