package com.settle.tracker.components.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.settle.tracker.components.expenses.ExpenseTable
import com.settle.tracker.scheme.ExpenseScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsExpenseModal(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    expenses: List<ExpenseScheme>,
    expenseIds: List<String>,
    onEditExpense: (String, String?) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val partialHeight = screenHeight * 0.45f

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp)
            ) {
                Text(
                    "${expenses.size}/50",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (expenses.isNotEmpty()) {
                ExpenseTable(
                    expenses = expenses,
                    smsExpenseIds = expenseIds,
                    toggleSmsModal = onDismissRequest,
                    onEditExpense = onEditExpense,
                    onDeleteExpense = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = partialHeight)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = partialHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sms,
                            contentDescription = "Empty SMS"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "No SMS expenses found",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}
