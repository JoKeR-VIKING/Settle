package com.settle.tracker.components.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person4
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.settle.tracker.scheme.ExpenseCategory
import com.settle.tracker.scheme.GroupScheme
import com.settle.tracker.scheme.SplitMode
import com.settle.tracker.scheme.SplitParticipant
import com.settle.tracker.scheme.UserScheme
import com.settle.tracker.utils.fetchGroupMembersChunked
import com.settle.tracker.utils.recalculateEqualSplit
import kotlinx.coroutines.delay

const val MAX_DESCRIPTION_CHARS = 30

@Composable
fun DateField(
    modifier: Modifier = Modifier,
    dateText: String,
    onFocus: () -> Unit
) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocus()
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
}

@Composable
fun AmountField(
    modifier: Modifier = Modifier,
    amount: String,
    displayAmount: String?,
    onAmountChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .onFocusChanged { focusState ->
                onFocusChanged(focusState.isFocused)
            },
        textStyle = MaterialTheme.typography.labelLarge,
        shape = RoundedCornerShape(15),
        label = { Text("Amount", style = MaterialTheme.typography.labelLarge) },
        value = displayAmount ?: amount,
        onValueChange = { newAmount ->
            val isValid = newAmount.matches(Regex("^\\d*(\\.\\d{0,2})?$"))
            if (isValid) onAmountChange(newAmount)
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
}

@Composable
fun DescriptionField(
    modifier: Modifier = Modifier,
    description: TextFieldValue,
    onDescriptionChange: (TextFieldValue) -> Unit
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(0.95f),
        textStyle = MaterialTheme.typography.labelLarge,
        shape = RoundedCornerShape(15),
        label = {
            Text(
                "Description",
                style = MaterialTheme.typography.labelLarge
            )
        },
        value = description,
        onValueChange = {
            if (it.text.length <= MAX_DESCRIPTION_CHARS) {
                onDescriptionChange(it)
            }
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
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${description.text.length} / $MAX_DESCRIPTION_CHARS",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
    )
}

@Composable
fun CategoryField(
    modifier: Modifier = Modifier,
    category: ExpenseCategory,
    onFocus: () -> Unit
) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocus()
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
                contentDescription = "Select Category"
            )
        }
    )
}

@Composable
fun PaymentMethodField(
    modifier: Modifier = Modifier,
    isGroupExpense: Boolean,
    paidFrom: String,
    paidBy: List<SplitParticipant>,
    onFocus: () -> Unit
) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocus()
                }
            },
        textStyle = MaterialTheme.typography.labelLarge,
        shape = RoundedCornerShape(15),
        label = {
            Text(
                text = if (isGroupExpense) "Paid By" else "Payment Method",
                style = MaterialTheme.typography.labelLarge
            )
        },
        value = (
            if (isGroupExpense) {
                if (paidBy.isEmpty()) ""
                else if (paidBy.size == 1) paidBy.first().name
                else "Multiple"
            } else {
                paidFrom
            }
        ),
        onValueChange = {},
        readOnly = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        ),
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = if (isGroupExpense) Icons.Filled.Person4 else Icons.Filled.Paid,
                contentDescription = if (isGroupExpense) "Paid By" else "Paid From"
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = if (isGroupExpense) "Select Paid By" else "Select Paid From"
            )
        }
    )
}

@Composable
fun SplitModeField(
    modifier: Modifier = Modifier,
    groupData: GroupScheme,
    selectedSplitMode: SplitMode,
    splits: List<SplitParticipant>,
    onSplitsChange: (List<SplitParticipant>, SplitMode) -> Unit,
    amount: String,
    mode: String
) {
    var totalAmount by remember { mutableDoubleStateOf(0.0) }
    var prevSplitMode by remember { mutableStateOf(selectedSplitMode) }
    var splitMode by remember { mutableStateOf(selectedSplitMode) }
    var selectedUserIds by remember { mutableStateOf(emptySet<String>()) }
    var groupMembers by remember { mutableStateOf(emptyList<UserScheme>()) }
    var isSplitHydrated by remember { mutableStateOf(false) }

    val db = Firebase.firestore

    val onTabSelected: (SplitMode) -> Unit = {
        splitMode = it
    }

    LaunchedEffect(selectedSplitMode) {
        splitMode = selectedSplitMode
        prevSplitMode = selectedSplitMode
    }

    LaunchedEffect(groupData.members) {
        fetchGroupMembersChunked(
            memberIds = groupData.members,
            updateLoadingStatus = {},
            db = db,
            updateGroupMembers = {
                groupMembers = it
            }
        )
    }

    LaunchedEffect(
        groupData.members,
        splits,
        mode
    ) {
        if (isSplitHydrated) return@LaunchedEffect

        when {
            mode == "EDIT" && splits.isNotEmpty() -> {
                selectedUserIds = splits.map { it.id }.toSet()
                isSplitHydrated = true
            }

            mode != "EDIT" && groupData.members.isNotEmpty() -> {
                selectedUserIds = groupData.members.toSet()
                isSplitHydrated = true
            }
        }
    }

    LaunchedEffect(amount) {
        if (amount.isBlank()) return@LaunchedEffect

        delay(400)
        amount.toDoubleOrNull()?.let {
            totalAmount = it
        }
    }

    LaunchedEffect(totalAmount) {
        if (splitMode == SplitMode.EQUAL) {
            onSplitsChange(
                recalculateEqualSplit(
                    groupMembers = groupMembers,
                    selectedUserIds = selectedUserIds,
                    totalAmount = totalAmount
                ),
                splitMode
            )
        }
    }

    LaunchedEffect(splitMode) {
        if (
            splitMode != prevSplitMode &&
            prevSplitMode == SplitMode.UNEQUAL
        ) {
            selectedUserIds = groupData.members.toSet()
            onSplitsChange(
                recalculateEqualSplit(
                    groupMembers = groupMembers,
                    selectedUserIds = selectedUserIds,
                    totalAmount = totalAmount
                ),
                splitMode
            )
        }

        prevSplitMode = splitMode
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SplitModeTabRow(
            splitMode = splitMode,
            onTabSelected = onTabSelected
        )

        if (splitMode == SplitMode.EQUAL) {
            EqualSplitMemberList(
                groupMembers = groupMembers,
                selectedUserIds = selectedUserIds,
                splits = splits,
                onMemberSelectionChange = { memberId ->
                    selectedUserIds = if (memberId in selectedUserIds) {
                        if (selectedUserIds.size > 1) {
                            selectedUserIds - memberId
                        } else {
                            selectedUserIds
                        }
                    } else {
                        selectedUserIds + memberId
                    }

                    onSplitsChange(
                        recalculateEqualSplit(
                            groupMembers = groupMembers,
                            selectedUserIds = selectedUserIds,
                            totalAmount = totalAmount
                        ),
                        splitMode
                    )
                }
            )
        } else {
            UnequalSplitMemberList(
                groupMembers = groupMembers,
                splits = splits,
                onSplitsChange = { updatedSplits ->
                    onSplitsChange(
                        updatedSplits,
                        splitMode
                    )
                }
            )
        }
    }
}
