package com.settle.tracker.screens

import android.annotation.SuppressLint
import android.util.Log
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneOffset
import java.lang.Exception
import java.util.UUID

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberDatePickerState

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

import com.settle.tracker.utils.formatCurrency
import com.settle.tracker.utils.formatTimestamp
import com.settle.tracker.utils.getExpenseCategoryIcon
import com.settle.tracker.utils.getExpenseCategoryColor
import com.settle.tracker.scheme.ExpenseCategory
import com.settle.tracker.scheme.ExpenseDraft
import com.settle.tracker.AppDatabase
import com.settle.tracker.components.LoadingScreenWrapper
import com.settle.tracker.components.FourDigitTextField
import kotlinx.coroutines.launch

data class PaymentOption(val name: String, val icon: ImageVector)

enum class ExpenseMode {
    ADD,
    EDIT,
    SMS_ADD
}
const val MAX_DESCRIPTION_CHARS = 30

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    onBack: () -> Unit,
    currentUser: FirebaseUser,
    expense: ExpenseDraft? = null
) {
    val focusManager = LocalFocusManager.current

    val db = Firebase.firestore
    val scope = rememberCoroutineScope()

    val datePickerSheetState = rememberModalBottomSheetState()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        selectableDates = BlockFutureDates()
    )
    val categoryPickerSheetState = rememberModalBottomSheetState()
    val paidFromSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    val expenseMode by remember {
        mutableStateOf(when {
            expense == null -> ExpenseMode.ADD
            expense.id?.isNotBlank() ?: false -> ExpenseMode.EDIT
            else -> ExpenseMode.SMS_ADD
        })
    }
    var id by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var smsExpenseId by remember { mutableStateOf(expense?.smsExpenseId) }
    var expenseDescription by remember { mutableStateOf(TextFieldValue("")) }
    var amount by remember { mutableStateOf("") }
    var displayAmount by remember { mutableStateOf<String?>(null) }
    var dateText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.MISC) }
    var paidFrom by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var lastFourDigits by remember { mutableStateOf("") }

    var showDatePickerModal by remember { mutableStateOf(false) }
    var showCategoryPickerModal by remember { mutableStateOf(false) }
    var showPaidFromModal by remember { mutableStateOf(false) }
    var isSubmittingExpense by remember { mutableStateOf(false) }

    val options = listOf(
        PaymentOption("Card", Icons.Filled.CreditCard),
        PaymentOption("Bank A/C", Icons.Filled.AccountBalance),
        PaymentOption("Wallet", Icons.Filled.Wallet)
    )
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val partialHeight = screenHeight * 0.25f

    val context = LocalContext.current
    val expenseDao = AppDatabase
        .getInstance(context)
        .expenseDraftDao()

    fun checkFieldsArePopulated(): Boolean {
        return (expenseDescription.text.isNotBlank() &&
            amount.isNotBlank() &&
            dateText.isNotBlank())
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

            if (expenseMode != ExpenseMode.EDIT) {
                expenseMap["createdAt"] = System.currentTimeMillis()
            }

            db
                .collection("users")
                .document(currentUser.uid)
                .collection("expenses")
                .document(id)
                .set(expenseMap, SetOptions.merge())
                .addOnSuccessListener {
                    smsExpenseId?.let { id ->
                        scope.launch {
                            expenseDao.delete(id)
                        }
                    }

                    isSubmittingExpense = false
                    onBack()
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

    LaunchedEffect(expense) {
        if (expense == null) return@LaunchedEffect

        id = expense.id?.takeIf { it.isNotBlank() } ?: id
        expenseDescription = TextFieldValue(expense.details)
        amount = expense.amount.toString()
        category = ExpenseCategory.valueOf(expense.category)
        paidFrom = expense.paidFrom
        datePickerState.selectedDateMillis = expense.timestamp

        when {
            expense.paidFrom.contains("Card") -> {
                selectedIndex = 0
                lastFourDigits = expense.paidFrom.takeLast(4)
            }
            expense.paidFrom.contains("Bank A/C") -> {
                selectedIndex = 1
                lastFourDigits = expense.paidFrom.takeLast(4)
            }
            expense.paidFrom.contains("Wallet") -> {
                selectedIndex = 2
                lastFourDigits = ""
            }
            else -> selectedIndex = -1
        }
    }

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { millis ->
            dateText = formatTimestamp(millis, "dd MMM YYYY")

            delay(200)
            showDatePickerModal = false
        }
    }

    LoadingScreenWrapper(
        isSubmittingExpense,
        "Submitting expense..."
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
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            modifier = Modifier.size(30.dp),
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = "Go Back"
                        )
                    }

                    Text(
                        if (expenseMode == ExpenseMode.EDIT) "Edit Expense" else "Add Expense",
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 18.sp,
                        letterSpacing = 0.5.sp,
                    )

                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Personal",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    showDatePickerModal = true
                                    focusManager.clearFocus()
                                }
                            },
                        textStyle = MaterialTheme.typography.labelLarge,
                        shape = RoundedCornerShape(15),
                        label = { Text("Date", style = MaterialTheme.typography.labelLarge) },
                        value = dateText,
                        onValueChange = {},
                        readOnly = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = "Date"
                            )
                        },
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    displayAmount = null
                                } else if (amount.isNotBlank()) {
                                    amount.toDoubleOrNull()?.let {
                                        displayAmount = formatCurrency(it)
                                    }
                                }
                            },
                        textStyle = MaterialTheme.typography.labelLarge,
                        shape = RoundedCornerShape(15),
                        label = { Text("Amount", style = MaterialTheme.typography.labelLarge) },
                        value = displayAmount ?: amount,
                        onValueChange = { newAmount ->
                            val isValid = newAmount.matches(Regex("^\\d*(\\.\\d{0,2})?$"))

                            if (isValid) amount = newAmount
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Money,
                                contentDescription = "Amount"
                            )
                        },
                    )

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(0.95f),
                        textStyle = MaterialTheme.typography.labelLarge,
                        shape = RoundedCornerShape(15),
                        label = {
                            Text(
                                "Description",
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        value = expenseDescription,
                        onValueChange = {
                            if (it.text.length <= MAX_DESCRIPTION_CHARS) expenseDescription = it
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = "Description"
                            )
                        },
                        supportingText = {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "${expenseDescription.text.length} / $MAX_DESCRIPTION_CHARS",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    showCategoryPickerModal = true
                                    focusManager.clearFocus()
                                }
                            },
                        textStyle = MaterialTheme.typography.labelLarge,
                        shape = RoundedCornerShape(15),
                        label = { Text("Category", style = MaterialTheme.typography.labelLarge) },
                        value = category.getDisplayName(),
                        onValueChange = {},
                        readOnly = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Category,
                                contentDescription = "Category"
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Select Cateogory"
                            )
                        }
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    showPaidFromModal = true
                                    focusManager.clearFocus()
                                }
                            },
                        textStyle = MaterialTheme.typography.labelLarge,
                        shape = RoundedCornerShape(15),
                        label = {
                            Text(
                                "Payment Method",
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        value = paidFrom,
                        onValueChange = {},
                        readOnly = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Paid,
                                contentDescription = "Paid From"
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Select Paid From"
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

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

                if (showDatePickerModal) {
                    ModalBottomSheet(
                        sheetState = datePickerSheetState,
                        onDismissRequest = { showDatePickerModal = false },
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        DatePicker(
                            state = datePickerState,
                            showModeToggle = false,
                            title = null,
                            headline = null,
                            modifier = Modifier.graphicsLayer(
                                scaleX = 0.9f,
                                scaleY = 0.9f,
                                transformOrigin = TransformOrigin(0.5f, 0f)
                            )
                        )
                    }
                } else if (showCategoryPickerModal) {
                    ModalBottomSheet(
                        sheetState = categoryPickerSheetState,
                        onDismissRequest = {
                            showCategoryPickerModal = false
                            selectedIndex = -1
                            lastFourDigits = ""
                        },
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(
                                items = ExpenseCategory.entries,
                                key = { it.name }
                            ) { expense ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(15))
                                        .clickable {
                                            category = expense
                                            showCategoryPickerModal = false
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .background(
                                                getExpenseCategoryColor(expense.name),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            modifier = Modifier.size(25.dp),
                                            imageVector = getExpenseCategoryIcon(expense.name),
                                            contentDescription = "Expense Icon",
                                            tint = MaterialTheme.colorScheme.onSecondary
                                        )
                                    }

                                    Text(
                                        text = expense.getDisplayName(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else if (showPaidFromModal) {
                    ModalBottomSheet(
                        sheetState = paidFromSheetState,
                        onDismissRequest = {
                            showPaidFromModal = false

                            when {
                                paidFrom.contains("Card") -> {
                                    selectedIndex = 0
                                    lastFourDigits = paidFrom.takeLast(4)
                                }
                                paidFrom.contains("Bank A/C") -> {
                                    selectedIndex = 1
                                    lastFourDigits = paidFrom.takeLast(4)
                                }
                                paidFrom.contains("Wallet") -> {
                                    selectedIndex = 2
                                    lastFourDigits = ""
                                }
                                else -> {
                                    selectedIndex = -1
                                    lastFourDigits = ""
                                }
                            }
                        },
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = partialHeight),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceAround
                        ) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                options.forEachIndexed { index, label ->
                                    SegmentedButton(
                                        label = {
                                            Text(
                                                text = label.name,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                            )
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = label.icon,
                                                contentDescription = label.name
                                            )
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = options.size
                                        ),
                                        selected = selectedIndex == index,
                                        onClick = {
                                            selectedIndex = index
                                            lastFourDigits = ""
                                        }
                                    )
                                }
                            }

                            FourDigitTextField(
                                lastFourDigits,
                                onValueChange = {
                                    lastFourDigits = it
                                },
                                isVisible = selectedIndex in 0..1,
                                helperText = if (selectedIndex in 0..1) "Enter the last 4 digits of your ${options[selectedIndex].name}" else ""
                            )

                            Button(
                                onClick = {
                                    paidFrom = "${options[selectedIndex].name} $lastFourDigits"
                                    showPaidFromModal = false
                                },
                                modifier = Modifier.fillMaxWidth(0.9f),
                                shape = RoundedCornerShape(25),
                                enabled = if (selectedIndex in 0..1) lastFourDigits.length == 4 else selectedIndex != -1
                            ) {
                                Text(
                                    "Confirm",
                                    style = MaterialTheme.typography.bodyLarge,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
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
