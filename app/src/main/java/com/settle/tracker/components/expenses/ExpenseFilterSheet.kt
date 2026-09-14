package com.settle.tracker.components.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.settle.tracker.scheme.ExpenseCategory

data class ExpenseFilters(
    val categories: Set<String> = emptySet(),
    val minAmount: Double? = null,
    val maxAmount: Double? = null
) {
    val isActive get() = categories.isNotEmpty() || minAmount != null || maxAmount != null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFilterSheet(
    sheetState: SheetState,
    filters: ExpenseFilters,
    onFiltersChanged: (ExpenseFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategories by remember(filters.categories) { mutableStateOf(filters.categories) }
    var minAmountText by remember(filters.minAmount) {
        mutableStateOf(filters.minAmount?.let { "%.0f".format(it) } ?: "")
    }
    var maxAmountText by remember(filters.maxAmount) {
        mutableStateOf(filters.maxAmount?.let { "%.0f".format(it) } ?: "")
    }

    val hasChanges = selectedCategories.isNotEmpty() || minAmountText.isNotBlank() || maxAmountText.isNotBlank()

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter Expenses", style = MaterialTheme.typography.titleMedium)
                if (hasChanges) {
                    TextButton(
                        onClick = {
                            selectedCategories = emptySet()
                            minAmountText = ""
                            maxAmountText = ""
                        }
                    ) { Text("Clear all") }
                }
            }

            Text(
                "Category",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExpenseCategory.entries.filter { it != ExpenseCategory.SETTLEMENT }) { cat ->
                    val selected = cat.name in selectedCategories
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedCategories = if (selected) {
                                selectedCategories - cat.name
                            } else {
                                selectedCategories + cat.name
                            }
                        },
                        label = {
                            Text(cat.getDisplayName(), style = MaterialTheme.typography.labelSmall)
                        },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Text(
                "Amount Range",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = minAmountText,
                    onValueChange = {
                        if (it.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) minAmountText = it
                    },
                    label = { Text("Min ₹") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = maxAmountText,
                    onValueChange = {
                        if (it.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) maxAmountText = it
                    },
                    label = { Text("Max ₹") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onFiltersChanged(
                        ExpenseFilters(
                            categories = selectedCategories,
                            minAmount = minAmountText.toDoubleOrNull(),
                            maxAmount = maxAmountText.toDoubleOrNull()
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Apply", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
