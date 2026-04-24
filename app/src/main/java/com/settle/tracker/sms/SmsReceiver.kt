package com.settle.tracker.sms

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.provider.Telephony
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.settle.tracker.AppDatabase
import com.settle.tracker.MainActivity
import com.settle.tracker.R
import com.settle.tracker.db.ExpenseEntity
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.utils.formatCurrency
import com.settle.tracker.utils.getExpenseCategoryLargeIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

fun ExpenseScheme.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    amount = amount,
    details = details,
    category = category,
    paidFrom = paidFrom,
    timestamp = timestamp
)

const val SMS_CHANNEL_ID = "sms_expense_channel"
const val GROUP_ID = "EXPENSE_GROUP"

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun sendNotification(
    context: Context,
    title: String,
    text: String,
    category: String,
    pendingIntent: PendingIntent? = null
) {
    val largeIcon = BitmapFactory.decodeResource(
        context.resources,
        getExpenseCategoryLargeIcon(category)
    )

    val notification = NotificationCompat.Builder(
        context,
        SMS_CHANNEL_ID
    )
        .setSmallIcon(R.drawable.logo)
        .setLargeIcon(largeIcon)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(
            NotificationCompat.BigTextStyle()
                .setBigContentTitle(title)
                .bigText(text)
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setGroup(GROUP_ID)
        .setAutoCancel(true)

    pendingIntent?.let {
        notification.setContentIntent(pendingIntent)
    }

    NotificationManagerCompat
        .from(context)
        .notify(System.currentTimeMillis().toInt(), notification.build())
}

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val db = AppDatabase.getInstance(context)

        messages.forEach { sms ->
            val sender = sms.displayOriginatingAddress ?: return@forEach
            val body = sms.displayMessageBody ?: return@forEach
            val timestamp = sms.timestampMillis

            val draft = SmsParse.parse(
                sender = sender,
                body = body,
                receivedAt = timestamp
            )

            if (draft != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    db.expenseDraftDao().insert(draft.toEntity())

                    val openAppIntent = Intent(
                        context,
                        MainActivity::class.java
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

                        putExtra("destination", "add_edit_expense")
                        putExtra("mode", "SMS_ADD")
                        putExtra("smsExpenseId", draft.id)
                    }

                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        draft.id.hashCode().absoluteValue,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        sendNotification(
                            context,
                            title = "Spent ${formatCurrency(draft.amount)}",
                            text = "Paid to ${draft.details} using ${draft.paidFrom}",
                            category = draft.category,
                            pendingIntent
                        )
                    }
                }
            }
        }
    }
}
