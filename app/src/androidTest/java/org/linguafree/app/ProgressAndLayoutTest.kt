package org.linguafree.app

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressAndLayoutTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()

    @Before fun seedExistingLearner(){
        compose.activity.getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE).edit()
            .putBoolean("onboarded",true).putBoolean("showWelcome",false)
            .putInt("lesson",0).putInt("xp",123).putString("completed","0,1")
            .putString("learnerName","Тест").putBoolean("effectsEnabled",false)
            .putInt("reminders",0).commit()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
    }

    @Test fun changingAvatarAndRecreatingPreservesOldProgress(){
        compose.onNodeWithText("🦊 Моя лисичка: Моя первая лисичка").performScrollTo().performClick()
        compose.onNodeWithText("Космос").performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        val p=compose.activity.getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE)
        assertEquals(123,p.getInt("xp",0))
        assertEquals("0,1",p.getString("completed",""))
        assertEquals("Тест",p.getString("learnerName",""))
        assertEquals(3,p.getInt("foxAvatar",0))
        assertTrue(p.getBoolean("onboarded",false))
    }

    @Test fun lessonAnswersAndContinueStayInsideTheAvailableWindow(){
        compose.onNode(hasText("УРОК 1") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Hello",substring=false).assertIsDisplayed().performClick()
        val footer=compose.onNodeWithText("ПРОДОЛЖИТЬ 🚀").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val answer=compose.onNodeWithText("✓ Hello",substring=false).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Continue is below the usable screen",footer.bottom<=root.bottom+1f)
        assertTrue("Answer overlaps Continue",answer.bottom<=footer.top+1f)
        assertTrue("Answer is clipped at the top",answer.top>=root.top)
    }
}
