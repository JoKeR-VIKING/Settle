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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.settle.tracker.AppDatabase
import com.settle.tracker.MainActivity
import com.settle.tracker.R
import com.settle.tracker.db.ExpenseEntity
import com.settle.tracker.db.SpendPatternDao
import com.settle.tracker.db.SpendPatternEntity
import com.settle.tracker.scheme.ExpenseCategory
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.scheme.ExpenseSource
import com.settle.tracker.utils.formatCurrency
import com.settle.tracker.utils.getExpenseCategoryLargeIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.absoluteValue

fun ExpenseScheme.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    amount = amount,
    details = details,
    category = category,
    paidFrom = paidFrom,
    timestamp = timestamp,
    patternId = patternId
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
        val currentUserId = Firebase.auth.currentUser?.uid

        messages.forEach { sms ->
            val sender = sms.displayOriginatingAddress ?: return@forEach
            val body = sms.displayMessageBody ?: return@forEach
            val timestamp = sms.timestampMillis

            val parsed = SmsParse.parse(
                sender = sender,
                body = body,
                receivedAt = timestamp
            ) ?: return@forEach

            CoroutineScope(Dispatchers.IO).launch {
                handleParsedExpense(context, db, currentUserId, parsed, timestamp)
            }
        }
    }

    /**
     * Only MISC-category SMS ever reach the pattern engine — anything the
     * grammar/keyword layer already categorized confidently is left alone.
     * See SpendPatternEngine for the matching/graduation logic itself.
     */
    private suspend fun handleParsedExpense(
        context: Context,
        db: AppDatabase,
        currentUserId: String?,
        parsed: ExpenseScheme,
        timestamp: Long
    ) {
        val patternDao = db.spendPatternDao()

        val outcome = if (parsed.category == ExpenseCategory.MISC.name && currentUserId != null) {
            val matchable = patternDao.getAllPatterns().map { pattern ->
                SpendPatternEngine.MatchablePattern(
                    id = pattern.id,
                    state = pattern.state,
                    amountMin = pattern.amountMin,
                    amountMax = pattern.amountMax,
                    timeStartMinutes = pattern.timeStartMinutes,
                    timeEndMinutes = pattern.timeEndMinutes,
                    negatives = patternDao.getNegatives(pattern.id).map { it.amount to it.timeMinutes }
                )
            }
            SpendPatternEngine.matchPattern(parsed.amount, timestamp, matchable)
        } else {
            SpendPatternEngine.MatchOutcome.NoMatch
        }

        when (outcome) {
            is SpendPatternEngine.MatchOutcome.Auto -> {
                val pattern = patternDao.getPattern(outcome.patternId) ?: return
                autoAddExpense(context, currentUserId!!, patternDao, pattern, parsed)
            }

            is SpendPatternEngine.MatchOutcome.Review -> {
                val pattern = patternDao.getPattern(outcome.patternId) ?: return
                val draft = parsed.copy(
                    details = pattern.label,
                    category = pattern.category,
                    patternId = pattern.id
                )
                insertDraftAndNotify(context, db, draft)
            }

            is SpendPatternEngine.MatchOutcome.NoMatch -> {
                insertDraftAndNotify(context, db, parsed)
            }
        }
    }

    private suspend fun autoAddExpense(
        context: Context,
        currentUserId: String,
        patternDao: SpendPatternDao,
        pattern: SpendPatternEntity,
        parsed: ExpenseScheme
    ) {
        val finalExpense = parsed.copy(
            id = UUID.randomUUID().toString(),
            details = pattern.label,
            category = pattern.category,
            source = ExpenseSource.SMS_AUTO.name,
            patternId = pattern.id,
            createdAt = System.currentTimeMillis()
        )

        Firebase.firestore
            .collection("users")
            .document(currentUserId)
            .collection("expenses")
            .document(finalExpense.id)
            .set(finalExpense)

        val updatedPattern = pattern.copy(
            occurrenceCount = pattern.occurrenceCount + 1,
            lastMatchedAt = System.currentTimeMillis()
        )
        patternDao.updatePattern(updatedPattern)

        Firebase.firestore
            .collection("users")
            .document(currentUserId)
            .collection("spendPatterns")
            .document(pattern.id)
            .update(
                mapOf(
                    "occurrenceCount" to updatedPattern.occurrenceCount,
                    "lastMatchedAt" to updatedPattern.lastMatchedAt
                )
            )

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            sendNotification(
                context,
                title = "Auto-added: ${formatCurrency(finalExpense.amount)}",
                text = "${pattern.label} was added automatically — look for the AI icon in your list.",
                category = finalExpense.category
            )
        }
    }

    private suspend fun insertDraftAndNotify(
        context: Context,
        db: AppDatabase,
        draft: ExpenseScheme
    ) {
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
