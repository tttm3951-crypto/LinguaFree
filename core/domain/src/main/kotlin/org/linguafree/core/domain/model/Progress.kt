package org.linguafree.core.domain.model

import java.time.LocalDate

/** State of a node on the path. */
enum class NodeState { Locked, Current, Completed, Legendary }

data class NodeProgress(
    val nodeId: String,
    val completed: Boolean,
    val legendary: Boolean,
    val timesCompleted: Int,
    val bestAccuracy: Int,
    /** Last accuracy (0–100), drives adaptive review in the next lesson. */
    val lastAccuracy: Int,
)

/** Why a lesson-like session was started. */
enum class SessionKind(val key: String) {
    Lesson("lesson"),
    Story("story"),
    UnitReview("review"),
    UnitTest("test"),
    /** Jump ahead: test of a locked unit; passing completes everything up to it. */
    SkipTest("skip"),
    Legendary("legendary"),
    Placement("placement"),
    PracticeMistakes("mistakes"),
    PracticeWeakWords("weak"),
    PracticeListening("listening"),
    PracticeSpeaking("speaking"),
    PracticeMatch("match"),
    /** Free practice that refills all hearts. */
    HeartsRefill("hearts"),
    /** XP carried over from LinguaFree 0.9 (not a real session). */
    Imported("imported");

    val isPractice: Boolean
        get() = this in setOf(PracticeMistakes, PracticeWeakWords, PracticeListening, PracticeSpeaking, PracticeMatch, HeartsRefill, UnitReview)
    /** Test-like sessions: no retries at the end, mistakes are limited, hearts are not spent. */
    val isTest: Boolean get() = this == UnitTest || this == SkipTest || this == Legendary || this == Placement
    /** Hearts are only spent in regular lessons and stories. Practice and tests are free. */
    val spendsHearts: Boolean get() = this == Lesson || this == Story

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: Lesson
    }
}

data class LessonResult(
    val nodeId: String?,
    val kind: SessionKind,
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
) {
    val perfect: Boolean get() = mistakes == 0 && total > 0
}

data class Wallet(
    val gems: Int,
    val hearts: Int,
    /** When the next heart arrives (epoch ms) or null when full. */
    val heartsRefillAt: Long?,
    val freezes: Int,
    /** Double XP boost active until (epoch ms). */
    val doubleXpUntil: Long,
)

data class StreakState(
    val studyDays: Set<LocalDate>,
    val frozenDays: Set<LocalDate>,
    val longest: Int,
    /** Last date for which missed days were processed (freezes applied). */
    val checkedThrough: LocalDate?,
)

enum class QuestPeriod { Daily, Monthly }

enum class QuestType(val title: (Int) -> String) {
    EarnXp({ "Заработать $it XP" }),
    CompleteLessons({ "Пройти уроков: $it" }),
    PerfectLessons({ "Урок без ошибок: $it" }),
    Combo({ "$it верных ответов подряд" }),
    Listening({ "Задания на аудирование: $it" }),
    Speaking({ "Задания на говорение: $it" }),
    Practice({ "Тренировок в «Практике»: $it" }),
    DailyQuests({ "Выполнить ежедневных заданий: $it" }),
}

data class Quest(
    val id: String,
    val period: QuestPeriod,
    /** Day (yyyy-MM-dd) or month (yyyy-MM) the quest belongs to. */
    val periodKey: String,
    val type: QuestType,
    val target: Int,
    val progress: Int,
    val reward: Int,
    val claimed: Boolean,
) {
    val done: Boolean get() = progress >= target
    val title: String get() = type.title(target)
}

data class AchievementState(
    val id: String,
    val title: String,
    val description: String,
    val tiers: List<Int>,
    val value: Int,
    /** Number of tiers reached. */
    val level: Int,
    val claimedLevel: Int,
) {
    val nextTarget: Int? get() = tiers.getOrNull(level)
    val maxed: Boolean get() = level >= tiers.size
}

data class LeagueEntry(
    val id: String,
    val name: String,
    val avatar: Int,
    val xp: Int,
    val isUser: Boolean,
)

data class LeagueStanding(
    val tier: Int,
    val weekKey: String,
    val entries: List<LeagueEntry>,
    /** Epoch ms when the week ends (Sunday 23:59:59 local). */
    val endsAt: Long,
    val joined: Boolean,
)

/** Spaced-repetition state of a lexeme. */
data class LexemeState(
    val id: String,
    val word: String,
    val translation: String,
    val right: Int,
    val wrong: Int,
    /** Days. */
    val halfLife: Double,
    val lastSeen: Long,
    /** Predicted recall probability now, 0..1. */
    val strength: Double,
)

data class UserProfile(
    val name: String,
    val goal: LearningGoal,
    val dailyGoalMinutes: Int,
    val reminderMinuteOfDay: Int,
    val remindersEnabled: Boolean,
    val onboarded: Boolean,
    val createdAt: Long,
) {
    /** Daily goal in XP: 5 min ≈ 10 XP. */
    val dailyGoalXp: Int get() = DailyGoal.xpForMinutes(dailyGoalMinutes)
}

object DailyGoal {
    val minuteOptions = listOf(5, 10, 15, 20)
    fun xpForMinutes(minutes: Int) = when {
        minutes <= 5 -> 10
        minutes <= 10 -> 20
        minutes <= 15 -> 30
        else -> 50
    }
}

enum class LearningGoal(val title: String, val emoji: String) {
    Travel("Путешествия", "✈️"),
    Work("Работа и карьера", "💼"),
    Study("Учёба и экзамены", "🎓"),
    Family("Семья и друзья", "👨‍👩‍👧"),
    Culture("Фильмы, книги, музыка", "🎬"),
    Brain("Тренировка мозга", "🧠"),
    Other("Другое", "✨"),
}
