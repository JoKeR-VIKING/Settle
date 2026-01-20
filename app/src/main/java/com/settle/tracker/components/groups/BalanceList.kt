package com.settle.tracker.components.groups

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.ConfirmAlertDialog
import com.settle.tracker.components.LoadingScreenWrapper
import com.settle.tracker.scheme.ExpenseCategory
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.scheme.GroupScheme
import com.settle.tracker.scheme.SplitMode
import com.settle.tracker.scheme.SplitParticipant
import com.settle.tracker.scheme.UserScheme
import com.settle.tracker.utils.fetchGroupMembersChunked
import com.settle.tracker.utils.formatCurrency
import java.util.UUID
import kotlin.math.absoluteValue

@Composable
fun BalanceList(
    groupData: GroupScheme,
    expenses: List<ExpenseScheme>
) {
    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore

    var balanceList by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    val groupMemberMap = remember {
        mutableStateMapOf<String, UserScheme>()
    }
    var pendingSettlement by remember { mutableStateOf<Triple<UserScheme, UserScheme, Double>?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    fun calculateExpenseBalance(
        expense: ExpenseScheme
    ): Map<String, Double> {
        val expenseBalanceMap = mutableMapOf<String, Double>()

        expense.paidBy.forEach { payer ->
            expenseBalanceMap[payer.id] = (expenseBalanceMap[payer.id] ?: 0.0) + payer.amount
        }

        expense.splits.forEach { split ->
            expenseBalanceMap[split.id] = (expenseBalanceMap[split.id] ?: 0.0) - split.amount
        }

        return expenseBalanceMap
    }

    fun calculatePersonBalance(): Map<String, Double> {
        val balances = mutableMapOf<String, Double>()

        expenses.forEach { expense ->
            val expenseBalanceMap = calculateExpenseBalance(expense)

            expenseBalanceMap.forEach { (personId, amount) ->
                if (personId == currentUser?.uid || amount == 0.0) return@forEach

                val myBalance = expenseBalanceMap[personId] ?: 0.0
                if (myBalance == 0.0) return@forEach

                balances[personId] = (balances[personId] ?: 0.0) + amount * -1
            }
        }

        return balances.filter { (_, amount) ->
            amount != 0.0
        }
    }

    fun settleBalance(
        payerScheme: UserScheme,
        receiverScheme: UserScheme,
        groupId: String,
        amount: Double
    ) {
        isLoading = true
        val expenseId = UUID.randomUUID().toString()

        val settlementData = ExpenseScheme(
            id = expenseId,
            timestamp = System.currentTimeMillis(),
            details = "Settlement with ${receiverScheme.name}",
            amount = amount.absoluteValue,
            category = ExpenseCategory.SETTLEMENT.name,
            paidBy = listOf(
                SplitParticipant(
                    id = payerScheme.id,
                    name = payerScheme.name,
                    amount = amount.absoluteValue
                )
            ),
            splits = listOf(
                SplitParticipant(
                    id = receiverScheme.id,
                    name = receiverScheme.name,
                    amount = amount.absoluteValue
                )
            ),
            splitMode = SplitMode.EQUAL.name,
            createdAt = System.currentTimeMillis()
        )

        db
            .collection("groups")
            .document(groupId)
            .collection("expenses")
            .document(expenseId)
            .set(settlementData)
            .addOnSuccessListener {
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    fun openUpiApp(
        context: Context,
        receiverScheme: UserScheme,
        amount: Double
    ) {
        if (receiverScheme.upiId.isBlank()) return

        val uri = Uri.parse(
            "upi://pay" +
                "?pa=${receiverScheme.upiId}" +
                "&am=${amount.absoluteValue}" +
                "&cu=INR" +
                "&tn=Settlement for ${groupData.groupName}" +
                "&mode=02"
        )

        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)

        context.startActivity(
            Intent.createChooser(intent, "Pay using UPI")
        )
    }

    LaunchedEffect(groupData.members) {
        fetchGroupMembersChunked(
            memberIds = groupData.members,
            updateLoadingStatus = {
                isLoading = it
            },
            db = db,
            updateGroupMembers = { groupMembers ->
                groupMembers.forEach { member ->
                    groupMemberMap[member.id] = member
                }
            }
        )
    }

    LaunchedEffect(expenses) {
        balanceList = calculatePersonBalance()
    }

    LoadingScreenWrapper(
        isLoading = isLoading,
        message = "Fetching balances..."
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            balanceList.forEach { (personId, amount) ->
                val payer = if (amount < 0) currentUser?.uid else personId
                val receiver = if (amount >= 0) currentUser?.uid else personId

                val payerScheme = groupMemberMap[payer]
                val receiverScheme = groupMemberMap[receiver]

                if (payerScheme == null || receiverScheme == null) return@forEach

                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10)
                        )
                        .padding(horizontal = 10.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = payerScheme.photoUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )

                            Text(
                                text = payerScheme.name,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface,
                                        RoundedCornerShape(50)
                                    ),
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10),
                                tonalElevation = 2.dp,
                            ) {
                                Text(
                                    text = (
                                        formatCurrency(amount.absoluteValue)
                                        ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = (
                                        if (amount >= 0.0) MaterialTheme.colorScheme.surfaceBright
                                        else MaterialTheme.colorScheme.error
                                        )
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = receiverScheme.photoUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )

                            Text(
                                text = receiverScheme.name,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }

                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth(0.4f),
                        border = BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        onClick = {
                            pendingSettlement = Triple(
                                payerScheme,
                                receiverScheme,
                                amount
                            )

                            openUpiApp(
                                context = context,
                                receiverScheme = receiverScheme,
                                amount = amount
                            )

                            showConfirmDialog = true
                        }
                    ) {
                        Text(
                            text = "Settle",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (showConfirmDialog) {
            ConfirmAlertDialog(
                title = "Settle Balace",
                text = "Are you sure you want to settle this balance?",
                subText = "This action can be undone, but may cause confusions.",
                onConfirm = {
                    val (payerScheme, receiverScheme, amount) = pendingSettlement!!

                    showConfirmDialog = false
                    pendingSettlement = null

                    settleBalance(
                        payerScheme = payerScheme,
                        receiverScheme = receiverScheme,
                        groupId = groupData.id,
                        amount = amount
                    )
                },
                toggleAlert = { showConfirmDialog = false },
                confirmText = "Settle",
                confirmColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}
