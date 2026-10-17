package org.linguafree.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ---------- Content (re-imported when contentVersion changes) ----------

@Entity(tableName = "course")
data class CourseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val schemaVersion: Int,
    val contentVersion: Int,
)

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val ord: Int,
    val title: String,
    val cefr: String,
    val description: String,
)

@Entity(tableName = "units", indices = [Index("sectionId")])
data class UnitEntity(
    @PrimaryKey val id: String,
    val sectionId: String,
    val ord: Int,
    val title: String,
    val description: String,
    val color: Long,
    /** JSON list of {title, text, examples:[{en, ru}]}. */
    val guidebookJson: String,
)

/** A node of the learning path: lesson, story, chest, unit review or unit test. */
@Entity(tableName = "lessons", indices = [Index("unitId")])
data class LessonEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val ord: Int,
    val type: String,
    val title: String,
    val newWordsCsv: String,
)

@Entity(tableName = "exercises", indices = [Index("lessonId")])
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val ord: Int,
    val type: String,
    /** Main text of the task (the "prompt"). */
    val prompt: String,
    val instruction: String?,
    val subtitle: String?,
    val direction: String?,
    /** JSON list of {text, image}. */
    val optionsJson: String,
    /** JSON list of accepted answers. */
    val correctAnswersJson: String,
    val audioRef: String?,
    val imageRef: String?,
    /** JSON object with pairs/lines/questions for match and dialogue. */
    val extraJson: String?,
    val lexemesCsv: String,
    val hintsJson: String?,
)

/** Word or phrase with its spaced-repetition memory (kept across content updates). */
@Entity(tableName = "lexemes")
data class LexemeEntity(
    @PrimaryKey val id: String,
    val word: String,
    val translation: String,
    val image: String?,
    /** Predicted recall right after the last practice (0..1); current strength is computed on read. */
    val strength: Double = 0.0,
    val lastSeen: Long = 0,
    /** Days. */
    val halfLife: Double = 0.0,
    val rightCount: Int = 0,
    val errorCount: Int = 0,
)

// ---------- Progress ----------

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val lessonId: String,
    val completed: Boolean,
    val legendary: Boolean,
    val timesCompleted: Int,
    val bestAccuracy: Int,
    val lastAccuracy: Int,
    val completedAt: Long,
)

@Entity(tableName = "lesson_results", indices = [Index("finishedAt")])
data class LessonResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonId: String?,
    val kind: String,
    val xp: Int,
    val accuracy: Int,
    val durationMs: Long,
    val mistakes: Int,
    val correct: Int,
    val total: Int,
    val bestCombo: Int,
    val listeningDone: Int,
    val speakingDone: Int,
    val finishedAt: Long,
    val passed: Boolean,
)

@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey val exerciseId: String,
    val at: Long,
)

/** A study day (yyyy-MM-dd) or a day saved by a streak freeze. */
@Entity(tableName = "streak_days")
data class StreakDayEntity(
    @PrimaryKey val date: String,
    val frozen: Boolean,
)

@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val id: Int = 0,
    val longest: Int,
    val checkedThrough: String?,
)

@Entity(tableName = "wallet")
data class WalletEntity(
    @PrimaryKey val id: Int = 0,
    val gems: Int,
    val hearts: Int,
    val heartsRefillAt: Long?,
    val freezes: Int,
    val doubleXpUntil: Long,
)

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: String,
    val period: String,
    val periodKey: String,
    val type: String,
    val target: Int,
    val progress: Int,
    val reward: Int,
    val claimed: Boolean,
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val claimedLevel: Int,
)

/** A finished league week of the learner (history shown in the profile). */
@Entity(tableName = "league_entries")
data class LeagueEntryEntity(
    @PrimaryKey val weekKey: String,
    val tier: Int,
    val rank: Int,
    val tierChange: Int,
)

@Entity(tableName = "league_state")
data class LeagueStateEntity(
    @PrimaryKey val id: Int = 0,
    val tier: Int,
    val processedWeek: String,
    val lastRank: Int,
    val lastTierChange: Int,
    val lastWeek: String,
    val top3Count: Int,
)

@Entity(tableName = "inventory")
data class InventoryEntity(@PrimaryKey val item: String, val boughtAt: Long)

@Entity(tableName = "counters")
data class CounterEntity(@PrimaryKey val key: String, val value: Int)
