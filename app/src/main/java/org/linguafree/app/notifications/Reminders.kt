package org.linguafree.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.linguafree.app.MainActivity
import org.linguafree.app.R
import org.linguafree.core.domain.game.StreakRules
import org.linguafree.core.domain.model.UserSettings
import org.linguafree.core.domain.repository.GameRepository
import org.linguafree.core.domain.repository.SettingsRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun settings(): SettingsRepository
    fun game(): GameRepository
}

/**
 * Daily reminder at the chosen time and an evening one when the streak is at risk.
 * Each is a one-time WorkManager job for the next occurrence; the worker schedules the next day.
 * Works offline; nothing leaves the phone.
 */
object Reminders {
    const val CHANNEL = "reminders"
    private const val DAILY = "reminder-daily"
    private const val EVENING = "reminder-evening"
    private const val KEY_KIND = "kind"
    /** Evening check time: 20:30 local. */
    val EVENING_TIME: LocalTime = LocalTime.of(20, 30)

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL, context.getString(R.string.notification_channel_reminders), NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = context.getString(R.string.notification_channel_reminders_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context, s: UserSettings, now: ZonedDateTime = ZonedDateTime.now()) {
        val wm = WorkManager.getInstance(context)
        if (s.onboarded && s.remindersEnabled) {
            enqueue(wm, DAILY, delayUntil(now, LocalTime.of(s.reminderMinute / 60, s.reminderMinute % 60)))
        } else wm.cancelUniqueWork(DAILY)
        if (s.onboarded && s.eveningStreakReminder) enqueue(wm, EVENING, delayUntil(now, EVENING_TIME))
        else wm.cancelUniqueWork(EVENING)
    }

    /** Time until the next [at] strictly after [now] (today or tomorrow). */
    fun delayUntil(now: ZonedDateTime, at: LocalTime): Duration {
        var next = now.with(at).withSecond(0).withNano(0)
        if (!next.isAfter(now.plusSeconds(30))) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    private fun enqueue(wm: WorkManager, name: String, delay: Duration) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_KIND to name))
            .addTag("reminder")
            .build()
        wm.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
    }

    internal fun isEvening(kind: String?) = kind == EVENING
    internal fun kindOf(params: WorkerParameters) = params.inputData.getString(KEY_KIND)

    fun canNotify(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}

class ReminderWorker(context: Context, private val params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val deps = EntryPointAccessors.fromApplication(ctx, ReminderEntryPoint::class.java)
        val settings = deps.settings().current()
        val evening = Reminders.isEvening(Reminders.kindOf(params))
        val today = LocalDate.now()
        val streak = deps.game().streakNow()
        val studiedToday = today in streak.studyDays
        val current = StreakRules.current(streak.studyDays, streak.frozenDays, today)

        if (Reminders.canNotify(ctx) && !studiedToday) {
            if (evening && settings.eveningStreakReminder && current > 0) {
                val freezes = deps.game().walletNow().freezes
                show(ctx, 2, ctx.getString(R.string.reminder_evening_title, current),
                    ctx.getString(if (freezes > 0) R.string.reminder_evening_text_freeze else R.string.reminder_evening_text))
            } else if (!evening && settings.remindersEnabled) {
                val titles = ctx.resources.getStringArray(R.array.reminder_daily_titles)
                val texts = ctx.resources.getStringArray(R.array.reminder_daily_texts)
                val i = today.dayOfYear
                show(ctx, 1, titles[i % titles.size], texts[(i / titles.size) % texts.size])
            }
        }
        // Schedule tomorrow's reminders (and pick up any settings change).
        Reminders.schedule(ctx, settings, ZonedDateTime.now().plusMinutes(1))
        return Result.success()
    }

    private fun show(ctx: Context, id: Int, title: String, text: String) {
        val open = PendingIntent.getActivity(
            ctx, id, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, Reminders.CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF4CC417.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call: nothing to do.
        }
    }
}

/** Re-creates reminders after a reboot or a clock/time-zone change. */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val deps = EntryPointAccessors.fromApplication(context.applicationContext, ReminderEntryPoint::class.java)
                Reminders.schedule(context.applicationContext, deps.settings().current())
            } finally {
                pending.finish()
            }
        }
    }
}
