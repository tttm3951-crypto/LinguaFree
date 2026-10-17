package org.linguafree.app

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import android.graphics.Bitmap
import org.json.JSONObject
import org.json.JSONArray
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@RunWith(AndroidJUnit4::class)
class ProgressAndLayoutTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()

    @Before fun seedExistingLearner(){
        compose.activity.getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE).edit()
            .putBoolean("onboarded",true).putBoolean("showWelcome",false)
            .putInt("foxAvatar",0)
            .putInt("lesson",0).putInt("xp",123).putString("completed","0,1")
            .putString("learnerName","Тест").putBoolean("effectsEnabled",false)
            .putInt("reminders",0).commit()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
    }

    @Test fun changingAvatarAndRecreatingPreservesOldProgress(){
        compose.onNodeWithTag("main-content").performScrollToNode(hasText("🦊 Моя лисичка: Моя первая лисичка"))
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

    @Test fun corruptedBackupCannotPartiallyOverwriteExistingProgress(){
        val p=compose.activity.getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE)
        val invalid=JSONObject().put("format","LinguaFreeProgress").put("version",1)
            .put("preferences",JSONObject().put("xp",900).put("lesson","bad type"))
        assertTrue(runCatching{ProgressBackup.restore(p,invalid)}.isFailure)
        assertEquals(123,p.getInt("xp",0));assertEquals(0,p.getInt("lesson",0))
        val old=JSONObject().put("format","LinguaFreeProgress").put("version",1)
            .put("preferences",JSONObject().put("xp",77).put("lesson",2).put("completed","0,1"))
        assertTrue(ProgressBackup.restore(p,old))
        assertEquals(77,p.getInt("xp",0));assertTrue(p.getBoolean("onboarded",false))
        assertEquals("Тест",p.getString("learnerName",""))
    }

    @Test fun bundledReferenceHas150000DistinctPairsAndSearchesBothLanguages(){
        OfflineReferenceStore.open(compose.activity).use { db ->
            db.rawQuery("SELECT count(*) FROM entries",null).use { c -> c.moveToFirst();assertEquals(150000,c.getInt(0)) }
            db.rawQuery("SELECT count(*) FROM (SELECT en,ru FROM entries GROUP BY en,ru)",null).use { c -> c.moveToFirst();assertEquals(150000,c.getInt(0)) }
        }
        assertTrue(OfflineReferenceStore.find(compose.activity,"hello",0).isNotEmpty())
        assertTrue(OfflineReferenceStore.find(compose.activity,"привет",0).isNotEmpty())
        assertTrue(OfflineReferenceStore.find(compose.activity,"' OR 1=1 --",0).isEmpty())
        assertTrue(OfflineReferenceStore.find(compose.activity,"",0).isEmpty())
        assertTrue(OfflineReferenceStore.find(compose.activity,"the",0).size<=51)
        val p=compose.activity.getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE)
        assertEquals(123,p.getInt("xp",0)); assertEquals("0,1",p.getString("completed",""))
    }

    @Test fun beginnerRouteOpensWithoutResettingExistingLearner(){
        compose.onNodeWithTag("main-content").performScrollToNode(hasText("🌱 Английский с нуля: буквы → первые фразы"))
        compose.onNodeWithText("🌱 Английский с нуля: буквы → первые фразы").performClick()
        compose.onNodeWithText("🌱 Английский с нуля").assertIsDisplayed()
        compose.onNodeWithText("Pre-A1").assertIsDisplayed()
        val p=compose.activity.getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE)
        assertEquals(123,p.getInt("xp",0));assertEquals("Тест",p.getString("learnerName",""))
    }

    @Test fun lateWritingExerciseRemainsReachableWithKeyboard(){
        val p=compose.activity.getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE)
        p.edit().putInt("lesson",50).putBoolean("showWelcome",true).putString("reviewStages","50002:2").commit()
        compose.activityRule.scenario.recreate();compose.waitForIdle()
        compose.onNodeWithText("Продолжить обучение 🚀").performScrollTo().performClick()
        val lesson=JSONArray(compose.activity.assets.open("lessons.json").bufferedReader().use{it.readText()}).getJSONObject(50)
        for(i in 0..5){
            val q=lesson.getJSONArray("questions").getJSONObject(i)
            compose.onNodeWithText(q.getJSONArray("choices").getString(q.getInt("answer")),substring=false).performScrollTo().performClick()
            compose.onNodeWithText("ПРОДОЛЖИТЬ 🚀").performClick()
        }
        compose.onNode(hasSetTextAction()).performScrollTo().performClick().performTextInput("a")
        compose.waitUntil(10000){ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime())==true}
        val field=compose.onNode(hasSetTextAction()).performScrollTo().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(field.top>=root.top&&field.bottom<=root.bottom+1f)
        compose.onNodeWithText("Проверить",substring=false).performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("ПРОДОЛЖИТЬ 🚀").assertIsDisplayed()
        assertEquals(123,p.getInt("xp",0))
    }

    @Test fun lessonAnswersAndContinueStayInsideTheAvailableWindow(){
        compose.onNodeWithTag("main-content").performScrollToNode(hasText("УРОК 1"))
        compose.onNode(hasText("УРОК 1") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Hello",substring=false).assertIsDisplayed().performClick()
        screenshot("lesson-after-answer.png")
        val footer=compose.onNodeWithText("ПРОДОЛЖИТЬ 🚀").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val answer=compose.onNodeWithText("✓ Hello",substring=false).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Continue is below the usable screen",footer.bottom<=root.bottom+1f)
        assertTrue("Answer overlaps Continue",answer.bottom<=footer.top+1f)
        assertTrue("Answer is clipped at the top",answer.top>=root.top)
    }

    private fun screenshot(name:String){
        val folder=File(compose.activity.filesDir,"screenshots").apply{mkdirs()}
        File(folder,name).outputStream().use{compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
}
