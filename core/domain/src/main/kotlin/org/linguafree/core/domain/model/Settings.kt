package org.linguafree.core.domain.model

/** Everything the learner can configure. Stored locally (DataStore), never sent anywhere. */
data class UserSettings(
    val onboarded: Boolean = false,
    val name: String = "",
    val goal: LearningGoal = LearningGoal.Other,
    val dailyGoalMinutes: Int = 10,
    val remindersEnabled: Boolean = true,
    /** Minutes after midnight for the daily reminder. */
    val reminderMinute: Int = 19 * 60,
    val eveningStreakReminder: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    /** «Режим без сердец»: mistakes never cost hearts. */
    val heartsDisabled: Boolean = false,
    /** 0 = system, 1 = light, 2 = dark. */
    val darkMode: Int = 0,
    val accent: String = "Forest",
    val outfit: String = "",
    /** «Не могу говорить сейчас»: speaking exercises are hidden until this time (epoch ms). */
    val speakingDisabledUntil: Long = 0,
    val listeningDisabledUntil: Long = 0,
    val voiceSpeed: Float = 1.0f,
    val placementDone: Boolean = false,
    val createdAt: Long = 0,
) {
    val dailyGoalXp: Int get() = DailyGoal.xpForMinutes(dailyGoalMinutes)
    fun speakingEnabled(now: Long) = now >= speakingDisabledUntil
    fun listeningEnabled(now: Long) = now >= listeningDisabledUntil
}
