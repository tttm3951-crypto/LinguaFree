package org.linguafree.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.linguafree.core.data.content.ContentLoader
import org.linguafree.core.data.db.LinguaFreeDatabase
import org.linguafree.core.data.migration.LegacyMigration
import org.linguafree.core.data.repository.ContentRepositoryImpl
import org.linguafree.core.data.repository.GameRepositoryImpl
import org.linguafree.core.data.repository.ProgressRepositoryImpl
import org.linguafree.core.data.settings.SettingsRepositoryImpl
import org.linguafree.core.data.settings.settingsStore
import org.linguafree.core.domain.repository.ContentStatus

/** Progress of LinguaFree 0.9 (SharedPreferences) is imported into the 1.0 database once. */
@RunWith(AndroidJUnit4::class)
class LegacyMigrationTest {
    @Test
    fun importsProgressFrom09() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.getSharedPreferences(LegacyMigration.LEGACY_PREFS, Context.MODE_PRIVATE).edit().clear()
            .putInt("xp", 321).putString("completed", "0,1").putString("learnerName", "Upgrade learner")
            .putBoolean("onboarded", true).putStringSet("beginnerMastered", setOf("start-01")).commit()
        val db = Room.inMemoryDatabaseBuilder(ctx, LinguaFreeDatabase::class.java).build()
        val content = ContentRepositoryImpl(ContentLoader(ctx, db.content()), db.content())
        assertTrue(content.ensureContent() is ContentStatus.Ready)
        val progress = ProgressRepositoryImpl(db.progress(), db.content())
        val game = GameRepositoryImpl(db.game())
        val settings = SettingsRepositoryImpl(ctx.settingsStore)
        settings.setFlag("migrated_from_0_9", false)

        val migration = LegacyMigration(ctx, content, progress, game, settings)
        migration.runOnce()
        migration.runOnce() // idempotent

        assertEquals(321, progress.results().first().sumOf { it.xp })
        val done = progress.nodeProgressNow().filterValues { it.completed }.keys
        assertTrue("legacy-L000" in done && "legacy-L001" in done)
        val s = settings.current()
        assertEquals("Upgrade learner", s.name)
        assertTrue(s.onboarded)
        // Library state stays in the 0.9 keys.
        assertEquals(setOf("start-01"), ctx.getSharedPreferences(LegacyMigration.LEGACY_PREFS, Context.MODE_PRIVATE).getStringSet("beginnerMastered", emptySet()))
        db.close()
    }
}
