package org.linguafree.feature.lesson

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.linguafree.core.data.platform.SpeechInput
import org.linguafree.core.domain.learning.AnswerChecker
import org.linguafree.core.domain.learning.LearnerAnswer
import org.linguafree.core.domain.model.Direction
import org.linguafree.core.domain.model.Exercise
import org.linguafree.core.domain.model.ExerciseOption
import org.linguafree.core.domain.model.ExerciseType
import org.linguafree.core.domain.model.SessionKind
import org.linguafree.core.domain.model.WordPair
import org.linguafree.core.ui.theme.LinguaFreeTheme
import org.linguafree.feature.lesson.exercises.ExerciseScope
import org.linguafree.feature.lesson.exercises.exerciseContent

/** Each exercise composable reports the learner's answer through the shared ExerciseScope. */
@RunWith(AndroidJUnit4::class)
class ExercisesUiTest {
    @get:Rule val compose = createComposeRule()

    private class FakeScope(override val exercise: Exercise, override val kind: SessionKind = SessionKind.Lesson) : ExerciseScope {
        override val locked = false
        override val checkedCorrect: Boolean? = null
        override val speechInput = SpeechInput(InstrumentationRegistry.getInstrumentation().targetContext)
        var answer: LearnerAnswer? = null
        var submitted: LearnerAnswer? = null
        override fun hint(word: String): String? = null
        override fun speak(text: String, slow: Boolean) = Unit
        override fun answer(answer: LearnerAnswer?) { this.answer = answer }
        override fun submit(answer: LearnerAnswer) { submitted = answer }
        override fun cantDoNow(speaking: Boolean) = Unit
        override fun tap() = Unit
    }

    private fun show(scope: FakeScope) = compose.setContent { LinguaFreeTheme { exerciseContent(scope.exercise.type)(scope) } }

    @Test
    fun selectTranslationReportsTheChosenOption() {
        val scope = FakeScope(Exercise("e1", "n", 1, ExerciseType.SelectTranslation, "привет", options = listOf(ExerciseOption("hello"), ExerciseOption("bye"), ExerciseOption("tea")), answers = listOf("hello")))
        show(scope)
        compose.onNodeWithTag("option:hello").performClick()
        compose.runOnIdle { assertEquals(LearnerAnswer.Choice("hello"), scope.answer) }
    }

    @Test
    fun tilesBuildTheSentenceInTapOrder() {
        val scope = FakeScope(Exercise("e2", "n", 1, ExerciseType.Tiles, "Я пью чай", options = listOf("tea", "I", "drink", "coffee").map { ExerciseOption(it) }, answers = listOf("I drink tea"), direction = Direction.RuToEn))
        show(scope)
        listOf("I", "drink", "tea").forEach { compose.onNodeWithTag("tile:$it").performClick() }
        compose.runOnIdle {
            val a = scope.answer as LearnerAnswer.Tiles
            assertEquals(listOf("I", "drink", "tea"), a.words)
            assertEquals(true, AnswerChecker.checkExact(a.words.joinToString(" "), listOf("I drink tea")).correct)
        }
    }

    @Test
    fun typingReportsTheText() {
        val scope = FakeScope(Exercise("e3", "n", 1, ExerciseType.TypeTranslation, "Спасибо", answers = listOf("thank you")))
        show(scope)
        compose.onNodeWithTag("type-input").performTextInput("thank you")
        compose.runOnIdle { assertEquals(LearnerAnswer.Typed("thank you"), scope.answer) }
    }

    @Test
    fun matchingAllPairsSubmitsWithoutMistakes() {
        val pairs = listOf("cat" to "кошка", "dog" to "собака", "tea" to "чай", "milk" to "молоко", "bread" to "хлеб")
        val scope = FakeScope(Exercise("e4", "n", 1, ExerciseType.MatchPairs, "", pairs = pairs.map { WordPair(it.first, it.second) }))
        show(scope)
        pairs.forEach { (en, ru) ->
            compose.onNodeWithTag("pair-en:$en").performClick()
            compose.onNodeWithTag("pair-ru:$ru").performClick()
        }
        compose.waitUntil(5_000) { scope.submitted != null }
        assertEquals(LearnerAnswer.Completed(0), scope.submitted)
    }
}
