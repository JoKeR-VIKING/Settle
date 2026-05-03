package com.settle.tracker.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.settle.tracker.AppDatabase
import com.settle.tracker.components.LoadingScreenWrapper
import com.settle.tracker.components.common.SuccessOverlay
import com.settle.tracker.components.expenses.AmountField
import com.settle.tracker.components.expenses.CategoryField
import com.settle.tracker.components.expenses.CategoryPickerModal
import com.settle.tracker.components.expenses.DateField
import com.settle.tracker.components.expenses.DatePickerModal
import com.settle.tracker.components.expenses.DescriptionField
import com.settle.tracker.components.expenses.GroupMemberPickerModal
import com.settle.tracker.components.expenses.PaymentMethodField
import com.settle.tracker.components.expenses.PaymentMethodPickerModal
import com.settle.tracker.components.expenses.RecurringExpenseField
import com.settle.tracker.components.expenses.SplitModeField
import com.settle.tracker.scheme.ExpenseCategory
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.scheme.GroupScheme
import com.settle.tracker.scheme.RecurrenceType
import com.settle.tracker.scheme.RecurringExpensesScheme
import com.settle.tracker.scheme.SplitMode
import com.settle.tracker.scheme.SplitParticipant
import com.settle.tracker.utils.calculateEndAt
import com.settle.tracker.utils.calculateNextOccurrence
import com.settle.tracker.utils.formatCurrency
import com.settle.tracker.utils.formatTimestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    onBack: () -> Unit,
    currentUser: FirebaseUser,
    mode: String,
    expenseId: String? = null,
    groupId: String? = null
) {
    val focusManager = LocalFocusManager.current
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val expenseDao = AppDatabase.getInstance(context).expenseDraftDao()

    val datePickerSheetState = rememberModalBottomSheetState()
    val categoryPickerSheetState = rememberModalBottomSheetState()
    val paidFromSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant()
            .toEpochMilli(),
        selectableDates = BlockFutureDates()
    )

    var groupName by remember { mutableStateOf("") }
    var groupData by remember { mutableStateOf(GroupScheme()) }

    var id by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var expenseDescription by remember { mutableStateOf(TextFieldValue("")) }
    var amount by remember { mutableStateOf("") }
    var displayAmount by remember { mutableStateOf<String?>(null) }
    var dateText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.MISC) }
    var paidFrom by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf(emptyList<SplitParticipant>()) }
    var splitMode by remember { mutableStateOf(SplitMode.EQUAL) }
    var splits by remember { mutableStateOf(emptyList<SplitParticipant>()) }

    var recurringTemplateId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var isRecurring by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf(RecurrenceType.MONTHLY) }
    var durationInMonths by remember { mutableStateOf<Int?>(null) }

    var showDatePickerModal by remember { mutableStateOf(false) }
    var showCategoryPickerModal by remember { mutableStateOf(false) }
    var showPaidFromModal by remember { mutableStateOf(false) }
    var isSubmittingExpense by remember { mutableStateOf(false) }
    var isFetchingExpense by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var previousPaymentMethods by remember { mutableStateOf<List<String>>(emptyList()) }

    fun Double.isAlmostEqualTo(other: Double?): Boolean {
        if (other == null) return false
        return abs(this - other) < 0.001
    }

    fun checkFieldsArePopulated(): Boolean {
        return (expenseDescription.text.isNotBlank() &&
            amount.isNotBlank() &&
            dateText.isNotBlank() &&
            (groupId == null || paidBy.isNotEmpty())) &&
            (groupId == null || splits.isNotEmpty()) &&
            (groupId == null || splits.sumOf { it.amount }.isAlmostEqualTo(amount.toDoubleOrNull())) &&
            (groupId == null || paidBy.sumOf { it.amount }.isAlmostEqualTo(amount.toDoubleOrNull()))
    }

    fun submitExpense() {
        isSubmittingExpense = true

        try {
            val expenseMap = mutableMapOf<String, Any>(
                "id" to id,
                "timestamp" to (datePickerState.selectedDateMillis ?: System.currentTimeMillis()),
                "details" to expenseDescription.text.trim(),
                "paidFrom" to paidFrom.trim().ifBlank { "Cash" },
                "amount" to (amount.toDoubleOrNull() ?: 0.0),
                "category" to category.name,
            )

            if (paidBy.isNotEmpty()) {
                expenseMap.remove("paidFrom")
                expenseMap["paidBy"] = paidBy
                expenseMap["splitMode"] = splitMode
                expenseMap["splits"] = splits
            }

            if (mode != "EDIT") {
                expenseMap["createdAt"] = System.currentTimeMillis()
            }

            if (isRecurring) {
                expenseMap["recurringTemplateId"] = recurringTemplateId
            }

            val ref = db
                .collection(if (groupId == null) "users" else "groups")
                .document(groupId ?: currentUser.uid)

            ref
                .collection("expenses")
                .document(id)
                .set(expenseMap, SetOptions.merge())
                .addOnSuccessListener {
                    expenseId?.let { id ->
                        scope.launch {
                            expenseDao.delete(id)
                        }
                    }

                    if (mode == "EDIT") {
                        isSubmittingExpense = false
                        onBack()
                        return@addOnSuccessListener
                    }

                    // For ADD / SMS_ADD: show celebratory overlay, then go back
                    isSubmittingExpense = false
                    showSuccess = true

                    if (isRecurring) {
                        val recurringTemplate = RecurringExpensesScheme(
                            id = recurringTemplateId,
                            expenseData = ExpenseScheme(
                                id = "",
                                timestamp = 0L,
                                details = expenseDescription.text.trim(),
                                paidFrom = paidFrom.trim().ifBlank { "Cash" },
                                paidBy = paidBy,
                                amount = (amount.toDoubleOrNull() ?: 0.0),
                                category = category.name,
                                splitMode = splitMode.name,
                                splits = splits,
                                recurringTemplateId = recurringTemplateId
                            ),
                            frequency = frequency.name,
                            startAt = (datePickerState.selectedDateMillis ?: System.currentTimeMillis()),
                            nextOccurrenceAt = calculateNextOccurrence(
                                from = (datePickerState.selectedDateMillis ?: System.currentTimeMillis()),
                                after = frequency
                            ),
                            durationInMonths = durationInMonths,
                            endAt = calculateEndAt(
                                startAt = (datePickerState.selectedDateMillis ?: System.currentTimeMillis()),
                                durationInMonths = durationInMonths
                            ),
                            paused = false,
                            createdAt = System.currentTimeMillis()
                        )

                        ref
                            .collection("recurring_expenses")
                            .document(recurringTemplateId)
                            .set(recurringTemplate, SetOptions.merge())
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "${e.message}")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    isSubmittingExpense = false
                    Log.e("Firestore", "${e.message}")
                }
        } catch (e: Exception) {
            isSubmittingExpense = false
            Log.e("Firestore", "${e.message}")
        }
    }

    LaunchedEffect(expenseId, mode) {
        if (expenseId == null) return@LaunchedEffect

        isFetchingExpense = true

        when (mode) {
            "EDIT" -> {
                val ref = db
                    .collection(if (groupId == null) "users" else "groups")
                    .document(groupId ?: currentUser.uid)

                ref
                    .collection("expenses")
                    .document(expenseId)
                    .get()
                    .addOnSuccessListener { document ->
                        val expense = document.toObject(ExpenseScheme::class.java)
                            ?: return@addOnSuccessListener

                        id = expense.id
                        expenseDescription = TextFieldValue(expense.details)
                        amount = String.format(Locale.getDefault(), "%.2f", expense.amount)
                        displayAmount = formatCurrency(expense.amount)
                        category = ExpenseCategory.valueOf(expense.category)
                        paidFrom = expense.paidFrom
                        paidBy = expense.paidBy
                        splitMode = SplitMode.valueOf(expense.splitMode)
                        splits = expense.splits
                        datePickerState.selectedDateMillis = expense.timestamp

                        isFetchingExpense = false
                    }
                    .addOnFailureListener { e ->
                        isFetchingExpense = false
                        Log.e("Firestore", "${e.message}")
                    }
            }

            "SMS_ADD" -> {
                val expense = expenseDao.getOne(expenseId).first()
                if (expense != null) {
                    expenseDescription = TextFieldValue(expense.details)
                    amount = String.format("%.2f", expense.amount)
                    displayAmount = formatCurrency(expense.amount)
                    category = ExpenseCategory.valueOf(expense.category)
                    paidFrom = expense.paidFrom
                    datePickerState.selectedDateMillis = expense.timestamp
                }
                isFetchingExpense = false
            }
        }
    }

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { millis ->
            dateText = formatTimestamp(millis, "dd MMM YYYY")
            delay(200)
            showDatePickerModal = false
        }
    }

    LaunchedEffect(groupId) {
        if (groupId == null) return@LaunchedEffect

        db
            .collection("groups")
            .document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val data = snapshot.toObject(GroupScheme::class.java)
                    groupData = data ?: GroupScheme()
                    groupName = data?.groupName ?: ""
                }
            }
    }

    LaunchedEffect(amount) {
        if (amount.isBlank()) return@LaunchedEffect

        delay(400)
        if (paidBy.size == 1) {
            paidBy = listOf(
                paidBy.first().copy(amount = amount.toDoubleOrNull() ?: 0.0)
            )
        }
    }

    LaunchedEffect(Unit) {
        db
            .collection(if (groupId == null) "users" else "groups")
            .document(groupId ?: currentUser.uid)
            .collection("expenses")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snapshot ->
                val recentPaidFrom = snapshot.documents
                    .mapNotNull { it.getString("paidFrom") }
                    .filterNot { it == "Cash" || it == "Wallet" }
                    .distinct()

                previousPaymentMethods = recentPaidFrom
            }
    }

    LoadingScreenWrapper(
        isSubmittingExpense || isFetchingExpense,
        if (isFetchingExpense) "Fetching expense..." else "Submitting expense..."
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                modifier = Modifier.size(25.dp),
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = "Go Back"
                            )
                        }

                        Text(
                            if (mode == "EDIT") "Edit Expense" else "Add Expense",
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 0.5.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(20)
                            )
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (groupId != null) groupName else "Personal",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        DateField(
                            dateText = dateText,
                            onFocus = {
                                showDatePickerModal = true
                                focusManager.clearFocus()
                            }
                        )
                    }

                    item {
                        AmountField(
                            amount = amount,
                            displayAmount = displayAmount,
                            onAmountChange = { newAmount -> amount = newAmount },
                            onFocusChanged = { isFocused ->
                                if (isFocused) {
                                    displayAmount = null
                                } else if (amount.isNotBlank()) {
                                    amount.toDoubleOrNull()?.let {
                                        displayAmount = formatCurrency(it)
                                    }
                                }
                            }
                        )
                    }

                    item {
                        DescriptionField(
                            description = expenseDescription,
                            onDescriptionChange = { newDescription ->
                                expenseDescription = newDescription
                            }
                        )
                    }

                    item {
                        CategoryField(
                            category = category,
                            onFocus = {
                                showCategoryPickerModal = true
                                focusManager.clearFocus()
                            }
                        )
                    }

                    item {
                        PaymentMethodField(
                            isGroupExpense = groupId != null,
                            paidFrom = paidFrom,
                            paidBy = paidBy,
                            onFocus = {
                                showPaidFromModal = true
                                focusManager.clearFocus()
                            }
                        )
                    }

                    if (groupId != null) {
                        item {
                            SplitModeField(
                                groupData = groupData,
                                selectedSplitMode = splitMode,
                                splits = splits,
                                onSplitsChange = { newSplits, selectedSplitMode ->
                                    splits = newSplits
                                    splitMode = selectedSplitMode
                                },
                                amount = amount,
                                mode = mode
                            )
                        }
                    }

                    if (mode != "EDIT") {
                        item {
                            RecurringExpenseField(
                                isRecurring = isRecurring,
                                toggleIsRecurring = { isRecurring = it },
                                frequency = frequency,
                                durationInMonths = durationInMonths,
                                onFrequencyChange = { frequency = it },
                                onDurationChange = { durationInMonths = it }
                            )
                        }
                    }

                    item {
                        Button(
                            modifier = Modifier.fillMaxWidth(0.95f),
                            onClick = { submitExpense() },
                            enabled = checkFieldsArePopulated()
                        ) {
                            Text(
                                "Submit Expense",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            if (showDatePickerModal) {
                DatePickerModal(
                    sheetState = datePickerSheetState,
                    datePickerState = datePickerState,
                    onDismissRequest = { showDatePickerModal = false }
                )
            }

            if (showCategoryPickerModal) {
                CategoryPickerModal(
                    sheetState = categoryPickerSheetState,
                    onDismissRequest = { showCategoryPickerModal = false },
                    onCategorySelected = { selectedCategory ->
                        category = selectedCategory
                    }
                )
            }

            if (showPaidFromModal) {
                if (groupId != null) {
                    GroupMemberPickerModal(
                        sheetState = paidFromSheetState,
                        onDismissRequest = { showPaidFromModal = false },
                        groupData = groupData,
                        paidBy = paidBy,
                        onMemberSelected = { members ->
                            paidBy = members
                        },
                        amount = amount.toDoubleOrNull() ?: 0.0
                    )
                } else {
                    PaymentMethodPickerModal(
                        sheetState = paidFromSheetState,
                        onDismissRequest = { showPaidFromModal = false },
                        previousPaymentMethods = previousPaymentMethods,
                        initialPaidFrom = paidFrom,
                        onPaymentMethodSelected = { paymentMethod ->
                            paidFrom = paymentMethod
                        }
                    )
                }
            }
        }

        // Celebratory success overlay – shown after a fresh expense was saved
        SuccessOverlay(
            visible = showSuccess,
            message = "Expense added!",
            onDismiss = {
                showSuccess = false
                onBack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class BlockFutureDates : SelectableDates {
    private val today = LocalDate.now()
    private val todayStartMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return utcTimeMillis <= todayStartMillis
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= today.year
    }
}
