package com.takemymeds.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    const val CHANNEL_ID = "tablet_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tablet reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to take your tablets"
                enableVibration(true)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun showDoseNotification(context: Context, doseId: Int, isFollowUp: Boolean) {
        ensureChannel(context)

        val label = Reminders.doseLabel(doseId)
        val title = if (isFollowUp) "Still haven't taken your tablet?" else "Time to take your tablet"
        val text = if (isFollowUp) "$label — you haven't marked this as taken yet" else "$label reminder"

        val takenIntent = Intent(context, TakenActionReceiver::class.java).apply {
            putExtra(Reminders.EXTRA_DOSE_ID, doseId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            doseId, // one "Taken" action pending intent per dose is enough
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = PendingIntent.getActivity(
            context,
            doseId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(false)
            .setOngoing(false)
            .setContentIntent(contentIntent)
            .addAction(0, "Taken", takenPendingIntent)
            .build()

        try {
            // Same notification ID per dose: a follow-up simply replaces the
            // original instead of stacking a second one.
            NotificationManagerCompat.from(context).notify(doseId, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission was not granted; nothing sensible
            // to do here besides not crashing.
        }
    }

    fun cancelDoseNotification(context: Context, doseId: Int) {
        NotificationManagerCompat.from(context).cancel(doseId)
    }
}
