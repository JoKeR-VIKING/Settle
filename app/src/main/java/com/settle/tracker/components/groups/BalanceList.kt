package com.settle.tracker.components.groups

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.ConfirmAlertDialog
import com.settle.tracker.components.LoadingScreenWrapper
import com.settle.tracker.components.expenses.BalanceCard
import com.settle.tracker.scheme.ExpenseCategory
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.scheme.GroupScheme
import com.settle.tracker.scheme.SplitMode
import com.settle.tracker.scheme.SplitParticipant
import com.settle.tracker.scheme.UserScheme
import com.settle.tracker.utils.calculateNetBalances
import com.settle.tracker.utils.fetchGroupMembersChunked
import java.util.UUID
import kotlin.math.min

const val EPSILON_VALUE = 0.01

data class Settlement(
    val from: String,
    val to: String,
    val amount: Double
)

@Composable
fun BalanceList(
    groupData: GroupScheme,
    expenses: List<ExpenseScheme>
) {
    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser
    val db = Firebase.firestore

    val groupMemberMap = remember {
        mutableStateMapOf<String, UserScheme>()
    }
    var settlements by remember { mutableStateOf(emptyList<Settlement>()) }
    var pendingSettlement by remember { mutableStateOf<Triple<UserScheme, UserScheme, Double>?>(null) }
    var othersExpanded by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    fun simplifyBalances(
        balances: Map<String, Double>
    ): List<Settlement> {
        val debtors = ArrayDeque(
            balances
                .filterValues { it < 0.0 }
                .map { it.key to -it.value }
        )
        val creditors = ArrayDeque(
            balances.filterValues { it > 0.0 }
                .map { it.key to it.value }
        )

        val settlements = mutableListOf<Settlement>()

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val (debtorId, debt) = debtors.removeFirst()
            val (creditorId, credit) = creditors.removeFirst()

            val settled = min(debt, credit)

            settlements += Settlement(
                from = debtorId,
                to = creditorId,
                amount = settled
            )

            if (debt > settled) {
                debtors.addFirst(
                    debtorId to debt - settled
                )
            }

            if (credit > settled) {
                creditors.addFirst(
                    creditorId to credit - settled
                )
            }
        }

        return settlements
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
            amount = amount,
            category = ExpenseCategory.SETTLEMENT.name,
            paidBy = listOf(
                SplitParticipant(
                    id = payerScheme.id,
                    name = payerScheme.name,
                    amount = amount
                )
            ),
            splits = listOf(
                SplitParticipant(
                    id = receiverScheme.id,
                    name = receiverScheme.name,
                    amount = amount
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
        if (receiverScheme.upiId.isBlank() || receiverScheme.id == currentUser?.uid) return

        val uri = Uri.parse(
            "upi://pay" +
                "?pa=${receiverScheme.upiId}" +
                "&am=${amount}" +
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
        val balances = calculateNetBalances(expenses)
        settlements = simplifyBalances(balances)
    }

    LoadingScreenWrapper(
        isLoading = isLoading,
        message = "Fetching balances..."
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            item {
                Text(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    text = "Your Balances",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start
                )
            }

            settlements
                .filter { it.from == currentUser?.uid || it.to == currentUser?.uid }
                .forEach { settlement ->
                    val payerScheme = groupMemberMap[settlement.from]
                    val receiverScheme = groupMemberMap[settlement.to]
                    val amount = settlement.amount

                    if (payerScheme == null || receiverScheme == null) return@forEach

                    item {
                        BalanceCard(
                            payerScheme = payerScheme,
                            receiverScheme = receiverScheme,
                            amount = amount,
                            showSettle = true,
                            openUpiApp = {
                                openUpiApp(
                                    context = context,
                                    receiverScheme = receiverScheme,
                                    amount = amount
                                )
                            },
                            setPendingSettlement = {
                                pendingSettlement = Triple(
                                    payerScheme,
                                    receiverScheme,
                                    amount
                                )
                            },
                            onShowConfirmDialog = {
                                showConfirmDialog = it
                            }
                        )
                    }
                }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable(
                            onClick = {
                                othersExpanded = !othersExpanded
                            }
                        )
                        .padding(horizontal = 4.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Other's Balances",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start
                    )

                    Icon(
                        imageVector = (
                            if (othersExpanded)
                                Icons.Filled.ArrowDropUp
                            else Icons.Filled.ArrowDropDown
                            ),
                        contentDescription = "Arrow Dropdown"
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = othersExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    settlements
                        .filter { it.from != currentUser?.uid && it.to != currentUser?.uid }
                        .forEach { settlement ->
                            val payerScheme = groupMemberMap[settlement.from]
                            val receiverScheme = groupMemberMap[settlement.to]
                            val amount = settlement.amount

                            if (payerScheme == null || receiverScheme == null) return@forEach

                            BalanceCard(
                                payerScheme = payerScheme,
                                receiverScheme = receiverScheme,
                                amount = amount,
                                showSettle = false,
                                openUpiApp = {
                                    openUpiApp(
                                        context = context,
                                        receiverScheme = receiverScheme,
                                        amount = amount
                                    )
                                },
                                setPendingSettlement = {
                                    pendingSettlement = Triple(
                                        payerScheme,
                                        receiverScheme,
                                        amount
                                    )
                                },
                                onShowConfirmDialog = {
                                    showConfirmDialog = it
                                }
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
