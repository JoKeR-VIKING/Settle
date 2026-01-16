package com.settle.tracker.sms

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

                    val largeIcon = BitmapFactory.decodeResource(
                        context.resources,
                        getExpenseCategoryLargeIcon(draft.category)
                    )

                    val notification = NotificationCompat.Builder(
                        context,
                        SMS_CHANNEL_ID
                    )
                        .setSmallIcon(R.drawable.logo)
                        .setLargeIcon(largeIcon)
                        .setContentTitle("Spent ${formatCurrency(draft.amount)}")
                        .setContentText("Paid to ${draft.details} using ${draft.paidFrom}")
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .setBigContentTitle("Spent ${formatCurrency(draft.amount)}")
                                .bigText("Paid to ${draft.details} using ${draft.paidFrom}")
                        )
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setGroup(GROUP_ID)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .build()

                    NotificationManagerCompat
                        .from(context)
                        .notify(System.currentTimeMillis().toInt(), notification)
                }
            }
        }
    }
}
