package org.linguafree.app

import android.Manifest
import android.os.Build
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.linguafree.core.domain.model.LearningGoal

/**
 * End-to-end on a real device: onboarding → path → first lesson (answer, check, feedback, exit)
 * → practice → dictionary → profile → about. Runs fully offline.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(
            if (Build.VERSION.SDK_INT >= 33) GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.RECORD_AUDIO)
            else GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO),
        )
        .around(compose)

    /** Waits for a node; on timeout fails with what is on screen (tags and texts) for the CI log. */
    private fun waitFor(matcher: SemanticsMatcher, what: String, ms: Long = 15_000) {
        try {
            compose.waitUntilAtLeastOneExists(matcher, ms)
        } catch (e: Throwable) {
            val screen = compose.onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true).fetchSemanticsNodes().mapNotNull { n ->
                val tag = n.config.getOrNull(SemanticsProperties.TestTag)
                val text = n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ")
                listOfNotNull(tag?.let { "#$it" }, text?.take(60)).joinToString(" ").ifBlank { null }
            }.distinct().joinToString(" | ")
            throw AssertionError("Timed out waiting for $what. On screen: ${screen.take(3500)}", e)
        }
    }
    private fun waitTag(tag: String, ms: Long = 15_000) = waitFor(hasTestTag(tag), "#$tag", ms)

    private fun click(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).performScrollTo().performClick()

    @Test
    fun onboardingLessonPracticeAndProfile() {
        // Startup imports the course; wait for the first screen.
        waitFor(hasTestTag("onboarding-continue") or hasTestTag("path"), "first screen", 30_000)
        if (compose.onAllNodes(hasTestTag("onboarding-continue")).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("onboarding-continue").performClick() // welcome
            click("goal-${LearningGoal.entries.first().name}")
            compose.onNodeWithTag("onboarding-continue").performClick()
            click("minutes-10")
            compose.onNodeWithTag("onboarding-continue").performClick()
            click("level-zero")
            compose.onNodeWithTag("onboarding-continue").performClick()
            compose.onNodeWithTag("onboarding-continue").performClick() // reminders (permission pre-granted)
            waitTag("name-input", 5_000)
            compose.onNodeWithTag("name-input").performTextInput("Тест")
            compose.onNodeWithTag("onboarding-continue").performClick()
        }

        // Path → first lesson.
        waitTag("node-u01-l1", 15_000)
        compose.onNodeWithTag("node-u01-l1").performClick()
        waitTag("node-start", 5_000)
        compose.onNodeWithTag("node-start").performClick()

        // Exercise 1 of lesson 1 is «привет» → picture «hello».
        waitTag("image:hello", 15_000)
        compose.onNodeWithTag("image:hello").performClick()
        compose.onNodeWithTag("check").performClick()
        waitTag("continue", 5_000)
        compose.onNodeWithTag("continue").performClick()

        // Leave the lesson with confirmation.
        compose.onNodeWithTag("lesson-close").performClick()
        waitFor(hasText("Выйти"), "text", 5_000)
        compose.onNodeWithText("Выйти").performClick()
        waitTag("path", 10_000)

        // Practice → dictionary.
        compose.onNodeWithTag("tab-Practice").performClick()
        waitTag("practice", 10_000)
        click("practice-dictionary")
        waitTag("dict-search", 10_000)
        compose.onNodeWithTag("dict-search").performTextInput("apple")
        waitFor(hasText("Apple", substring = true, ignoreCase = true), "text", 10_000)
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        // Profile → about: the app states it is free.
        compose.onNodeWithTag("tab-Profile").performClick()
        waitTag("profile", 10_000)
        click("open-about")
        waitTag("about-free", 10_000)
    }
}
