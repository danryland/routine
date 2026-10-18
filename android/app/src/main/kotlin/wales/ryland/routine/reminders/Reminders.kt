package wales.ryland.routine.reminders

import android.Manifest
import android.app.AlarmManager
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import wales.ryland.routine.MainActivity
import wales.ryland.routine.R
import wales.ryland.routine.RoutineApplication
import wales.ryland.routine.data.Storage

/**
 * A daily reminder at the user's chosen time. The alarm checks the routine when
 * it fires, so nothing is shown on days when everything's already done.
 */
object Reminders {
    private const val CHANNEL = "reminders"
    private const val NOTIFICATION_ID = 1

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Sets (or clears) the next alarm to match the current settings. */
    fun schedule(context: Context) {
        val settings = (context.applicationContext as RoutineApplication).settings.current
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = alarmIntent(context)
        alarms.cancel(pending)
        if (!settings.remindersOn || !settings.isUnlocked) return

        val now = LocalDateTime.now()
        var next = LocalDate.now().atStartOfDay().plusMinutes(settings.reminderMinute.toLong())
        if (!next.isAfter(now)) next = next.plusDays(1)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Inexact is fine for a daily nudge, and needs no special permission.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
    }

    internal fun remind(context: Context) {
        val app = context.applicationContext as RoutineApplication
        if (!app.settings.current.remindersOn || !app.settings.current.isUnlocked || !hasPermission(context)) return
        val today = LocalDate.now()
        val routine = Storage.store(context).load(today)
        val left = routine.habits.count { it.isTracked(today) && !it.isDone(today) }
        if (left == 0) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Routine")
            .setContentText(if (left == 1) "1 habit left today." else "$left habits left today.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission was revoked between the check and now.
        }
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** Fires at reminder time, then books tomorrow's. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.remind(context)
        Reminders.schedule(context)
    }
}

/** Alarms don't survive a restart or a clock change, so re-book them. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.schedule(context)
    }
}
