package org.linguafree.core.data.migration

import android.content.Context
import org.linguafree.core.data.settings.SettingsRepositoryImpl
import org.linguafree.core.domain.model.LessonResult
import org.linguafree.core.domain.model.SessionKind
import org.linguafree.core.domain.model.StreakState
import org.linguafree.core.domain.repository.ContentRepository
import org.linguafree.core.domain.repository.GameRepository
import org.linguafree.core.domain.repository.ProgressRepository
import java.time.LocalDate

/**
 * One-time import of LinguaFree 0.9 progress (SharedPreferences "linguafree_progress").
 * The old preferences are left untouched: grammar topics, dictionary favourites, word cards
 * and teacher reports keep using them, and they stay a fallback if anything goes wrong.
 */
class LegacyMigration(
    private val context: Context,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val game: GameRepository,
    private val settings: SettingsRepositoryImpl,
) {
    suspend fun runOnce() {
        if (settings.flag(FLAG)) return
        val prefs = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("onboarded", false) && prefs.getInt("xp", 0) == 0) {
            settings.setFlag(FLAG, true)
            return
        }
        val nodes = content.course().sections.flatMap { s -> s.units.flatMap { it.nodes } }
        val nodeIds = nodes.map { it.id }.toSet()

        // Completed 0.9 lessons → converted nodes (split lessons complete both parts).
        val completed = prefs.getString("completed", "").orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }
        val ids = completed.flatMap { i -> listOf("legacy-L%03d".format(i), "legacy-L%03da".format(i), "legacy-L%03db".format(i)) }
            .filter { it in nodeIds }
        progress.completeNodes(ids, null)

        // XP is kept as one historical record (outside any day or league week).
        val xp = prefs.getInt("xp", 0)
        if (xp > 0) progress.addResult(LessonResult(null, SessionKind.Imported, xp, 100, 0, 0, 0, 0, 0, 0, 0, finishedAt = 1L, passed = true))

        val gems = prefs.getInt("gems", 0)
        if (gems > 0) game.updateWallet { it.copy(gems = it.gems + gems) }

        val days = prefs.getString("studiedDates", "").orEmpty().split(',')
            .mapNotNull { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }.toSet()
        if (days.isNotEmpty()) {
            val st = game.streakNow()
            game.saveStreak(StreakState(st.studyDays + days, st.frozenDays, maxOf(st.longest, prefs.getInt("dailyStreak", 0)), st.checkedThrough))
        }

        // Pending mistakes: old keys are lesson*1000 + question.
        val all = content.exercisesByNode()
        val mistakes = prefs.getString("mistakes", "").orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }
        val mistakeIds = mistakes.mapNotNull { key ->
            val lesson = key / 1000
            val q = key % 1000
            listOf("", "a", "b").firstNotNullOfOrNull { suf -> all["legacy-L%03d%s".format(lesson, suf)]?.firstOrNull { it.id.endsWith("-q$q") }?.id }
        }
        progress.addMistakes(mistakeIds, System.currentTimeMillis())

        val name = prefs.getString("learnerName", "").orEmpty().trim()
        val goal = prefs.getInt("dailyGoal", 10)
        settings.update {
            it.copy(
                onboarded = it.onboarded || prefs.getBoolean("onboarded", false),
                name = it.name.ifBlank { name },
                dailyGoalMinutes = when { goal <= 5 -> 5; goal <= 10 -> 10; goal <= 15 -> 15; else -> 20 },
                soundEnabled = prefs.getBoolean("effectsEnabled", true),
                hapticsEnabled = prefs.getBoolean("hapticsEnabled", prefs.getBoolean("effectsEnabled", true)),
                remindersEnabled = prefs.getInt("reminders", 3) > 0,
                voiceSpeed = (prefs.getFloat("voiceSpeed", 0.93f) / 0.93f).coerceIn(0.6f, 1.4f),
                placementDone = true,
            )
        }
        settings.setFlag(FLAG, true)
    }

    companion object {
        const val LEGACY_PREFS = "linguafree_progress"
        private const val FLAG = "migrated_from_0_9"
    }
}
