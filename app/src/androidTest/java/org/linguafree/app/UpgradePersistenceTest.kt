package org.linguafree.app

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Run after adb install -r of the signed release. No setup writes: read only. */
@RunWith(AndroidJUnit4::class)
class UpgradePersistenceTest {
    @Test fun signedUpgradeKeepsExistingProgress(){
        assumeTrue(InstrumentationRegistry.getArguments().getString("upgradeCheck")=="true")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val p=context.getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE)
        assertEquals(321,p.getInt("xp",0))
        assertEquals(2,p.getInt("lesson",0))
        assertEquals("0,1",p.getString("completed",""))
        assertEquals("Upgrade learner",p.getString("learnerName",""))
        assertEquals(3,p.getInt("foxAvatar",0))
        assertEquals(setOf("start-01"),p.getStringSet("beginnerMastered",emptySet()))
        assertEquals("A1",p.getString("beginnerLevel",""))
        assertEquals("linguafree-stable-learner",p.getString("learnerId",""))
        assertTrue(p.getBoolean("onboarded",false))
        assertFalse(p.getBoolean("effectsEnabled",true))
        assertFalse(p.getBoolean("hapticsEnabled",true))
        OfflineReferenceStore.open(context).use { db ->
            db.rawQuery("SELECT count(*) FROM entries",null).use { c -> c.moveToFirst(); assertEquals(150000,c.getInt(0)) }
        }
    }
}
