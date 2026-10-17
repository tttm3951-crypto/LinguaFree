package org.linguafree.core.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.linguafree.core.domain.learning.AnswerChecker
import org.linguafree.core.domain.learning.Verdict

class AnswerCheckerTest {

    @ParameterizedTest
    @CsvSource(
        "I don't like milk., I do not like milk",
        "I'm Anna, I am Anna",
        "I dont like it, I don't like it",
        "can't, cannot",
        "It’s fine, it is fine",
        "HELLO   anna!!, Hello Anna",
        "Ещё чай, еще чай",
    )
    fun `normalization makes equivalent answers equal`(a: String, b: String) {
        assertEquals(AnswerChecker.normalize(a), AnswerChecker.normalize(b))
    }

    @Test
    fun `typed answer with one typo in a long word is accepted as typo`() {
        val v = AnswerChecker.checkTyped("I like aples", listOf("I like apples."))
        assertTrue(v is Verdict.Typo)
        assertTrue(v.correct)
    }

    @Test
    fun `short words must match exactly`() {
        assertFalse(AnswerChecker.checkTyped("she is here", listOf("He is here.")).correct)
        assertFalse(AnswerChecker.checkTyped("the apple", listOf("an apple")).correct)
    }

    @Test
    fun `too many typos are wrong and return the expected answer`() {
        val v = AnswerChecker.checkTyped("I lik apels", listOf("I like apples."))
        assertFalse(v.correct)
        assertEquals("I like apples.", v.expected)
    }

    @Test
    fun `exact check accepts any listed variant`() {
        assertTrue(AnswerChecker.checkExact("Hi, Anna", listOf("Hello, Anna!", "Hi, Anna!")).correct)
        assertFalse(AnswerChecker.checkExact("Anna hi", listOf("Hi, Anna!")).correct)
    }

    @Test
    fun `levenshtein distance`() {
        assertEquals(0, AnswerChecker.levenshtein("milk", "milk"))
        assertEquals(1, AnswerChecker.levenshtein("milk", "mik"))
        assertEquals(3, AnswerChecker.levenshtein("kitten", "sitting"))
    }

    @Test
    fun `speech similarity counts recognised words`() {
        assertEquals(100, AnswerChecker.speechSimilarity("How are you?", "how are you"))
        assertTrue(AnswerChecker.speechSimilarity("I like learning English", "I like English") in 40..80)
        assertEquals(0, AnswerChecker.speechSimilarity("Hello", ""))
    }
}
