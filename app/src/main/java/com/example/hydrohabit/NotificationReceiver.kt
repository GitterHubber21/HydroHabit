package com.example.hydrohabit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.*

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "water_reminder_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val sharedPrefs = context.getSharedPreferences("secure_cookies", Context.MODE_PRIVATE)
        val isNotificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", false)

        if (!isNotificationsEnabled) {
            return
        }

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        if (currentHour in 9..20) {
            sendNotification(context)
        }

        NotificationScheduler.scheduleNotifications(context)
    }

    private fun sendNotification(context: Context) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val messages = arrayOf(
            "💧 Time to hydrate! Your body needs water.",
            "🌊 Stay refreshed - drink some water!",
            "💙 Hydration check! Don't forget to drink water.",
            "🚰 Your water reminder is here - time to drink up!",
            "✨ Keep glowing! Stay hydrated with some water."
        )

        val randomMessage = messages.random()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.img)
            .setContentTitle("Reminder")
            .setContentText(randomMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250))

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: SecurityException) {
            // TODO:handle the exception
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Water Reminders"
            val descriptionText = "Notifications to remind you to drink water"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}