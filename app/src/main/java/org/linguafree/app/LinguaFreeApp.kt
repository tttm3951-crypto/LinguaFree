package org.linguafree.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import org.linguafree.app.notifications.Reminders
import org.linguafree.core.domain.repository.SettingsRepository
import javax.inject.Inject

/** Application class. No analytics, no ads, no network SDKs are initialised here — by design. */
@HiltAndroidApp
class LinguaFreeApp : Application() {
    @Inject lateinit var settings: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Reminders.createChannel(this)
        // Keep reminder jobs in sync with the reminder settings.
        scope.launch {
            settings.settings
                .distinctUntilChangedBy { Triple(it.onboarded && it.remindersEnabled, it.reminderMinute, it.onboarded && it.eveningStreakReminder) }
                .collect { runCatching { Reminders.schedule(this@LinguaFreeApp, it) } }
        }
    }
}
