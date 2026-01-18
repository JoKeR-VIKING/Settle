package com.settle.tracker.components.expenses

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.settle.tracker.scheme.ExpenseCategory
import com.settle.tracker.scheme.PaidBy

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
    paidBy: PaidBy?,
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
        value = if (isGroupExpense) paidBy?.name ?: "" else paidFrom,
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
