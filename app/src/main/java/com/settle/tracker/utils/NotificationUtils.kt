package com.settle.tracker.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.settle.tracker.sms.SMS_CHANNEL_ID

fun createSmsNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            SMS_CHANNEL_ID,
            "SMS Expenses",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for expenses detected from SMS"
        }
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(channel)
    }
}
