package com.example.hydrohabit

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*
import androidx.core.content.edit

object NotificationScheduler {

    private const val REQUEST_CODE = 1001
    private const val TWO_HOURS_IN_MILLIS = 2 * 60 * 60 * 1000L
    private const val PREFS_KEY_NEXT_NOTIFICATION = "next_notification_time"

    fun scheduleNotifications(context: Context) {
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedNextNotificationTime = sharedPrefs.getLong(PREFS_KEY_NEXT_NOTIFICATION, 0L)
        val currentTime = System.currentTimeMillis()


        if (savedNextNotificationTime > currentTime) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        alarmManager.cancel(pendingIntent)


        val nextNotificationTime = getNextNotificationTime()


        sharedPrefs.edit { putLong(PREFS_KEY_NEXT_NOTIFICATION, nextNotificationTime) }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextNotificationTime,
            pendingIntent
        )
    }

    fun cancelNotifications(context: Context) {
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)


        sharedPrefs.edit { remove(PREFS_KEY_NEXT_NOTIFICATION) }
    }

    fun forceScheduleNotifications(context: Context) {
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        alarmManager.cancel(pendingIntent)


        val nextNotificationTime = getNextNotificationTime()


        sharedPrefs.edit { putLong(PREFS_KEY_NEXT_NOTIFICATION, nextNotificationTime) }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextNotificationTime,
            pendingIntent
        )
    }

    private fun getNextNotificationTime(): Long {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)


        if (currentHour < 9) {
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            return calendar.timeInMillis
        }


        if (currentHour >= 21) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            return calendar.timeInMillis
        }


        val twoHoursLater = System.currentTimeMillis() + TWO_HOURS_IN_MILLIS
        val twoHoursLaterCalendar = Calendar.getInstance().apply {
            timeInMillis = twoHoursLater
        }


        if (twoHoursLaterCalendar.get(Calendar.HOUR_OF_DAY) > 20) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            return calendar.timeInMillis
        }

        return twoHoursLater
    }
}