package org.linguafree.core.data.backup

import android.content.Context
import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.linguafree.core.data.db.AchievementEntity
import org.linguafree.core.data.db.CounterEntity
import org.linguafree.core.data.db.InventoryEntity
import org.linguafree.core.data.db.LeagueEntryEntity
import org.linguafree.core.data.db.LeagueStateEntity
import org.linguafree.core.data.db.LessonResultEntity
import org.linguafree.core.data.db.LinguaFreeDatabase
import org.linguafree.core.data.db.MistakeEntity
import org.linguafree.core.data.db.QuestEntity
import org.linguafree.core.data.db.StreakDayEntity
import org.linguafree.core.data.db.StreakEntity
import org.linguafree.core.data.db.UserProgressEntity
import org.linguafree.core.data.db.WalletEntity
import org.linguafree.core.data.migration.LegacyMigration
import org.linguafree.core.data.settings.SettingsRepositoryImpl
import org.linguafree.core.domain.model.LearningGoal

@Serializable data class BackupMemory(val id: String, val strength: Double, val lastSeen: Long, val halfLife: Double, val right: Int, val error: Int)
@Serializable data class BackupProgress(val lessonId: String, val completed: Boolean, val legendary: Boolean, val times: Int, val best: Int, val last: Int, val at: Long)
@Serializable data class BackupResult(val lessonId: String? = null, val kind: String, val xp: Int, val accuracy: Int, val durationMs: Long, val mistakes: Int, val correct: Int, val total: Int, val bestCombo: Int, val listening: Int, val speaking: Int, val finishedAt: Long, val passed: Boolean)
@Serializable data class BackupQuest(val id: String, val period: String, val periodKey: String, val type: String, val target: Int, val progress: Int, val reward: Int, val claimed: Boolean)
@Serializable data class BackupWallet(val gems: Int, val hearts: Int, val heartsRefillAt: Long? = null, val freezes: Int, val doubleXpUntil: Long)
@Serializable data class BackupLeague(val tier: Int, val processedWeek: String, val lastRank: Int, val lastTierChange: Int, val lastWeek: String, val top3: Int)
@Serializable data class BackupLeagueWeek(val week: String, val tier: Int, val rank: Int, val change: Int)
@Serializable data class BackupSettings(
    val name: String = "", val goal: String = "Other", val dailyGoalMinutes: Int = 10, val remindersEnabled: Boolean = true,
    val reminderMinute: Int = 1140, val eveningStreakReminder: Boolean = true, val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true, val heartsDisabled: Boolean = false, val darkMode: Int = 0, val accent: String = "Forest",
    val outfit: String = "", val voiceSpeed: Float = 1f, val createdAt: Long = 0,
)

@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val app: String = "",
    val exportedAt: Long,
    val settings: BackupSettings,
    val progress: List<BackupProgress>,
    val results: List<BackupResult>,
    val memory: List<BackupMemory>,
    val mistakes: Map<String, Long>,
    val studyDays: List<String>,
    val frozenDays: List<String>,
    val longestStreak: Int,
    val streakCheckedThrough: String? = null,
    val wallet: BackupWallet,
    val quests: List<BackupQuest>,
    val achievements: Map<String, Int>,
    val league: BackupLeague? = null,
    val leagueHistory: List<BackupLeagueWeek> = emptyList(),
    val inventory: List<String>,
    val counters: Map<String, Int>,
    /** LinguaFree 0.9 preferences still used by grammar, dictionary, cards and teacher tools. */
    val legacyPrefs: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
) {
    companion object {
        const val FORMAT = "LinguaFreeProgress"
        const val VERSION = 2
    }
}

/** Export/import of all learner data as one JSON file the learner keeps wherever they like. */
class BackupManager(
    private val context: Context,
    private val db: LinguaFreeDatabase,
    private val settings: SettingsRepositoryImpl,
    private val migration: LegacyMigration,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    suspend fun export(appVersion: String): String {
        val p = db.progress()
        val g = db.game()
        val s = settings.current()
        val prefs = context.getSharedPreferences(LegacyMigration.LEGACY_PREFS, Context.MODE_PRIVATE).all.mapNotNull { (k, v) ->
            when (v) {
                is String -> k to JsonPrimitive(v)
                is Int -> k to JsonPrimitive(v)
                is Long -> k to JsonPrimitive(v)
                is Float -> k to JsonPrimitive(v)
                is Boolean -> k to JsonPrimitive(v)
                is Set<*> -> k to JsonArray(v.filterIsInstance<String>().map { JsonPrimitive(it) })
                else -> null
            }
        }.toMap()
        val streakDays = g.streakDays()
        val league = g.leagueState()
        val file = BackupFile(
            app = "LinguaFree $appVersion",
            exportedAt = System.currentTimeMillis(),
            settings = BackupSettings(s.name, s.goal.name, s.dailyGoalMinutes, s.remindersEnabled, s.reminderMinute, s.eveningStreakReminder,
                s.soundEnabled, s.hapticsEnabled, s.heartsDisabled, s.darkMode, s.accent, s.outfit, s.voiceSpeed, s.createdAt),
            progress = p.progress().map { BackupProgress(it.lessonId, it.completed, it.legendary, it.timesCompleted, it.bestAccuracy, it.lastAccuracy, it.completedAt) },
            results = p.results().map { BackupResult(it.lessonId, it.kind, it.xp, it.accuracy, it.durationMs, it.mistakes, it.correct, it.total, it.bestCombo, it.listeningDone, it.speakingDone, it.finishedAt, it.passed) },
            memory = db.content().lexemes().filter { it.lastSeen > 0 }.map { BackupMemory(it.id, it.strength, it.lastSeen, it.halfLife, it.rightCount, it.errorCount) },
            mistakes = p.mistakes().associate { it.exerciseId to it.at },
            studyDays = streakDays.filter { !it.frozen }.map { it.date },
            frozenDays = streakDays.filter { it.frozen }.map { it.date },
            longestStreak = g.streak()?.longest ?: 0,
            streakCheckedThrough = g.streak()?.checkedThrough,
            wallet = (g.wallet() ?: WalletEntity(0, 50, 5, null, 0, 0)).let { BackupWallet(it.gems, it.hearts, it.heartsRefillAt, it.freezes, it.doubleXpUntil) },
            quests = g.quests().map { BackupQuest(it.id, it.period, it.periodKey, it.type, it.target, it.progress, it.reward, it.claimed) },
            achievements = g.achievements().associate { it.id to it.claimedLevel },
            league = league?.let { BackupLeague(it.tier, it.processedWeek, it.lastRank, it.lastTierChange, it.lastWeek, it.top3Count) },
            leagueHistory = g.leagueEntries().map { BackupLeagueWeek(it.weekKey, it.tier, it.rank, it.tierChange) },
            inventory = g.inventory().map { it.item },
            counters = g.counters().associate { it.key to it.value },
            legacyPrefs = prefs,
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    sealed interface ImportResult {
        data object Ok : ImportResult
        data class Error(val message: String) : ImportResult
    }

    /** Validates the whole file before changing anything; an invalid file never half-applies. */
    suspend fun import(text: String): ImportResult {
        if (text.length > 20_000_000) return ImportResult.Error("Файл слишком большой")
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrElse { return ImportResult.Error("Это не JSON-файл") }
        val format = root["format"]?.jsonPrimitive?.content
        val version = root["version"]?.jsonPrimitive?.intOrNull
        if (format != BackupFile.FORMAT) return ImportResult.Error("Это не резервная копия LinguaFree")
        return when (version) {
            1 -> importLegacy(root)
            2 -> importV2(text)
            else -> ImportResult.Error("Версия резервной копии $version не поддерживается этой версией приложения")
        }
    }

    private suspend fun importV2(text: String): ImportResult {
        val f = runCatching { json.decodeFromString(BackupFile.serializer(), text) }.getOrElse { return ImportResult.Error("Файл повреждён: ${it.message?.take(120)}") }
        val dateRe = Regex("\\d{4}-\\d{2}-\\d{2}")
        if (f.studyDays.any { !dateRe.matches(it) } || f.frozenDays.any { !dateRe.matches(it) }) return ImportResult.Error("Неверные даты серии")
        if (f.wallet.gems < 0 || f.wallet.hearts !in 0..5 || f.wallet.freezes !in 0..2) return ImportResult.Error("Неверные данные кошелька")
        if (f.results.any { it.xp < 0 || it.xp > 10_000 }) return ImportResult.Error("Неверные результаты уроков")
        val p = db.progress()
        val g = db.game()
        db.withTransaction {
            p.clearProgress(); p.clearResults(); p.clearMistakes(); p.clearMemory()
            g.clearWallet(); g.clearStreakDays(); g.clearStreak(); g.clearQuests(); g.clearAchievements()
            g.clearLeagueState(); g.clearLeagueEntries(); g.clearInventory(); g.clearCounters()
            p.upsertProgress(f.progress.map { UserProgressEntity(it.lessonId, it.completed, it.legendary, it.times, it.best, it.last, it.at) })
            p.insertResults(f.results.map { LessonResultEntity(0, it.lessonId, it.kind, it.xp, it.accuracy, it.durationMs, it.mistakes, it.correct, it.total, it.bestCombo, it.listening, it.speaking, it.finishedAt, it.passed) })
            f.memory.forEach { db.content().updateMemory(it.id, it.strength, it.lastSeen, it.halfLife, it.right, it.error) }
            p.upsertMistakes(f.mistakes.map { MistakeEntity(it.key, it.value) })
            g.upsertStreakDays(f.studyDays.map { StreakDayEntity(it, false) } + f.frozenDays.filter { it !in f.studyDays }.map { StreakDayEntity(it, true) })
            g.upsertStreak(StreakEntity(0, f.longestStreak, f.streakCheckedThrough))
            g.upsertWallet(WalletEntity(0, f.wallet.gems, f.wallet.hearts, f.wallet.heartsRefillAt, f.wallet.freezes, f.wallet.doubleXpUntil))
            g.insertQuests(f.quests.map { QuestEntity(it.id, it.period, it.periodKey, it.type, it.target, it.progress, it.reward, it.claimed) })
            f.achievements.forEach { (id, lvl) -> g.upsertAchievement(AchievementEntity(id, lvl)) }
            f.league?.let { g.upsertLeagueState(LeagueStateEntity(0, it.tier.coerceIn(0, 9), it.processedWeek, it.lastRank, it.lastTierChange, it.lastWeek, it.top3)) }
            f.leagueHistory.forEach { g.upsertLeagueEntry(LeagueEntryEntity(it.week, it.tier, it.rank, it.change)) }
            f.inventory.forEach { g.addInventory(InventoryEntity(it, f.exportedAt)) }
            f.counters.forEach { (k, v) -> g.upsertCounter(CounterEntity(k, v)) }
        }
        val s = f.settings
        settings.update {
            it.copy(
                onboarded = true, name = s.name.take(24), goal = runCatching { LearningGoal.valueOf(s.goal) }.getOrDefault(LearningGoal.Other),
                dailyGoalMinutes = s.dailyGoalMinutes.coerceIn(5, 20), remindersEnabled = s.remindersEnabled,
                reminderMinute = s.reminderMinute.coerceIn(0, 1439), eveningStreakReminder = s.eveningStreakReminder,
                soundEnabled = s.soundEnabled, hapticsEnabled = s.hapticsEnabled, heartsDisabled = s.heartsDisabled,
                darkMode = s.darkMode.coerceIn(0, 2), accent = s.accent, outfit = s.outfit, voiceSpeed = s.voiceSpeed.coerceIn(0.5f, 1.5f),
                placementDone = true, createdAt = s.createdAt,
            )
        }
        writeLegacyPrefs(f.legacyPrefs)
        settings.setFlag("migrated_from_0_9", true)
        return ImportResult.Ok
    }

    /** LinguaFree 0.9 backup: restore its preferences and run the 0.9 migration again. */
    private suspend fun importLegacy(root: JsonObject): ImportResult {
        val prefs = root["preferences"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: return ImportResult.Error("В файле нет данных")
        writeLegacyPrefs(prefs)
        settings.setFlag("migrated_from_0_9", false)
        migration.runOnce()
        return ImportResult.Ok
    }

    private fun writeLegacyPrefs(values: Map<String, kotlinx.serialization.json.JsonElement>) {
        if (values.isEmpty()) return
        val editor = context.getSharedPreferences(LegacyMigration.LEGACY_PREFS, Context.MODE_PRIVATE).edit()
        for ((k, v) in values) {
            if (k.length > 120 || !k.all { it.isLetterOrDigit() || it == '_' || it == '-' }) continue
            when {
                v is JsonArray -> editor.putStringSet(k, v.mapNotNull { (it as? JsonPrimitive)?.content }.toSet())
                v is JsonPrimitive && v.isString -> editor.putString(k, v.content)
                v is JsonPrimitive && v.booleanOrNull != null -> editor.putBoolean(k, v.booleanOrNull!!)
                v is JsonPrimitive && v.intOrNull != null -> editor.putInt(k, v.int)
                v is JsonPrimitive && v.longOrNull != null -> editor.putLong(k, v.longOrNull!!)
                v is JsonPrimitive && v.doubleOrNull != null -> editor.putFloat(k, v.doubleOrNull!!.toFloat())
            }
        }
        editor.apply()
    }
}
