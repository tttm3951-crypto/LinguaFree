package org.linguafree.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.linguafree.core.domain.model.LearningGoal
import org.linguafree.core.domain.model.UserSettings
import org.linguafree.core.domain.repository.SettingsRepository

val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "linguafree_settings")

/** User settings in DataStore. Nothing here ever leaves the device. */
class SettingsRepositoryImpl(private val store: DataStore<Preferences>) : SettingsRepository {

    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val name = stringPreferencesKey("name")
        val goal = stringPreferencesKey("goal")
        val goalMinutes = intPreferencesKey("daily_goal_minutes")
        val reminders = booleanPreferencesKey("reminders")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val evening = booleanPreferencesKey("evening_reminder")
        val sound = booleanPreferencesKey("sound")
        val haptics = booleanPreferencesKey("haptics")
        val noHearts = booleanPreferencesKey("hearts_disabled")
        val dark = intPreferencesKey("dark_mode")
        val accent = stringPreferencesKey("accent")
        val outfit = stringPreferencesKey("outfit")
        val speakOff = longPreferencesKey("speaking_disabled_until")
        val listenOff = longPreferencesKey("listening_disabled_until")
        val voice = floatPreferencesKey("voice_speed")
        val placement = booleanPreferencesKey("placement_done")
        val created = longPreferencesKey("created_at")
    }

    override val settings: Flow<UserSettings> = store.data.map { it.toSettings() }

    override suspend fun current(): UserSettings = settings.first()

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        store.edit { p ->
            val n = transform(p.toSettings())
            p[K.onboarded] = n.onboarded
            p[K.name] = n.name
            p[K.goal] = n.goal.name
            p[K.goalMinutes] = n.dailyGoalMinutes
            p[K.reminders] = n.remindersEnabled
            p[K.reminderMinute] = n.reminderMinute
            p[K.evening] = n.eveningStreakReminder
            p[K.sound] = n.soundEnabled
            p[K.haptics] = n.hapticsEnabled
            p[K.noHearts] = n.heartsDisabled
            p[K.dark] = n.darkMode
            p[K.accent] = n.accent
            p[K.outfit] = n.outfit
            p[K.speakOff] = n.speakingDisabledUntil
            p[K.listenOff] = n.listeningDisabledUntil
            p[K.voice] = n.voiceSpeed
            p[K.placement] = n.placementDone
            p[K.created] = if (n.createdAt == 0L) System.currentTimeMillis() else n.createdAt
        }
    }

    private fun Preferences.toSettings(): UserSettings {
        val d = UserSettings()
        return UserSettings(
            onboarded = this[K.onboarded] ?: d.onboarded,
            name = this[K.name] ?: d.name,
            goal = runCatching { LearningGoal.valueOf(this[K.goal] ?: "") }.getOrDefault(d.goal),
            dailyGoalMinutes = this[K.goalMinutes] ?: d.dailyGoalMinutes,
            remindersEnabled = this[K.reminders] ?: d.remindersEnabled,
            reminderMinute = this[K.reminderMinute] ?: d.reminderMinute,
            eveningStreakReminder = this[K.evening] ?: d.eveningStreakReminder,
            soundEnabled = this[K.sound] ?: d.soundEnabled,
            hapticsEnabled = this[K.haptics] ?: d.hapticsEnabled,
            heartsDisabled = this[K.noHearts] ?: d.heartsDisabled,
            darkMode = this[K.dark] ?: d.darkMode,
            accent = this[K.accent] ?: d.accent,
            outfit = this[K.outfit] ?: d.outfit,
            speakingDisabledUntil = this[K.speakOff] ?: d.speakingDisabledUntil,
            listeningDisabledUntil = this[K.listenOff] ?: d.listeningDisabledUntil,
            voiceSpeed = this[K.voice] ?: d.voiceSpeed,
            placementDone = this[K.placement] ?: d.placementDone,
            createdAt = this[K.created] ?: d.createdAt,
        )
    }

    /** Internal flags not exposed in [UserSettings]. */
    suspend fun flag(name: String): Boolean = store.data.first()[booleanPreferencesKey(name)] ?: false
    suspend fun setFlag(name: String, value: Boolean) { store.edit { it[booleanPreferencesKey(name)] = value } }
}
