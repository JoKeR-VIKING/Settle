package com.settle.tracker.components.expenses

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.settle.tracker.components.ConfirmAlertDialog
import com.settle.tracker.components.LoadingScreenWrapper
import com.settle.tracker.scheme.RecurrenceType
import com.settle.tracker.scheme.RecurringExpensesScheme
import com.settle.tracker.utils.calculateNextOccurrence
import com.settle.tracker.utils.formatCurrency
import com.settle.tracker.utils.formatTimestamp
import com.settle.tracker.utils.getExpenseCategoryColor
import com.settle.tracker.utils.getExpenseCategoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesList(
    ownerCollection: String,
    ownerId: String,
    modifier: Modifier = Modifier
) {
    val db = Firebase.firestore

    val editSheetState = rememberModalBottomSheetState()

    var recurringTemplates by remember { mutableStateOf(emptyList<RecurringExpensesScheme>()) }
    var deletionTemplatedId by remember { mutableStateOf("") }

    var editTemplate by remember { mutableStateOf(RecurringExpensesScheme()) }
    var editStatus by remember { mutableStateOf(false) }
    var editFrequency by remember { mutableStateOf(RecurrenceType.MONTHLY) }

    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

    fun editTemplate() {
        isLoading = true
        val updatedTemplate = mutableMapOf<String, Any>()

        updatedTemplate["paused"] = editStatus
        updatedTemplate["frequency"] = editFrequency.name

        if (editTemplate.frequency != editFrequency.name) {
            updatedTemplate["nextOccurrenceAt"] = calculateNextOccurrence(
                System.currentTimeMillis(),
                editFrequency
            )
        }

        db
            .collection(ownerCollection)
            .document(ownerId)
            .collection("recurring_expenses")
            .document(editTemplate.id)
            .update(updatedTemplate)
            .addOnSuccessListener {
                editStatus = false
                editFrequency = RecurrenceType.MONTHLY
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    fun deleteTemplate(templateId: String) {
        isLoading = true

        db
            .collection(ownerCollection)
            .document(ownerId)
            .collection("recurring_expenses")
            .document(templateId)
            .delete()
            .addOnSuccessListener {
                deletionTemplatedId = ""
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    LaunchedEffect(ownerCollection, ownerId) {
        db
            .collection(ownerCollection)
            .document(ownerId)
            .collection("recurring_expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "${error.message}")
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val data = snapshot.toObjects(RecurringExpensesScheme::class.java)
                recurringTemplates = data
                isLoading = false
            }
    }

    LoadingScreenWrapper(
        isLoading = isLoading,
        message = "Fetching recurring templates..."
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            recurringTemplates.forEach { template ->
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10)
                            )
                            .combinedClickable(
                                onClick = {
                                    editTemplate = template
                                    editFrequency = RecurrenceType.valueOf(template.frequency)
                                    editStatus = template.paused
                                    showEditSheet = true
                                },
                                onLongClick = {
                                    deletionTemplatedId = template.id
                                    showDeleteDialog = true
                                }
                            )
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            getExpenseCategoryColor(template.expenseData.category),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        modifier = Modifier.size(25.dp),
                                        imageVector = getExpenseCategoryIcon(template.expenseData.category),
                                        contentDescription = "Expense Icon",
                                        tint = MaterialTheme.colorScheme.onSecondary
                                    )
                                }

                                Text(
                                    text = template.expenseData.details,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            Text(
                                text = formatCurrency(template.expenseData.amount),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        if (template.expenseData.splits.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Split between",
                                    style = MaterialTheme.typography.labelLarge
                                )

                                template.expenseData.splits.forEach { splitParticipant ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = splitParticipant.name,
                                            style = MaterialTheme.typography.labelMedium
                                        )

                                        Text(
                                            text = "---",
                                            style = MaterialTheme.typography.labelMedium
                                        )

                                        Text(
                                            text = formatCurrency(splitParticipant.amount),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Next Trigger On: ${formatTimestamp(template.nextOccurrenceAt, "dd MMM YYYY")}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )

                        if (template.durationInMonths != null && template.endAt != null) {
                            Text(
                                text = "EMI of: ${template.durationInMonths} months",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )

                            Text(
                                text = "Ends On: ${formatTimestamp(template.endAt, "dd MMM YYYY")}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        Text(
                            text = "Runs: ${RecurrenceType.valueOf(template.frequency).getDisplayName()}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )

                        Text(
                            text = "Status: ${if (template.paused) "Paused" else "Active"}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (template.paused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceBright
                        )
                    }
                }
            }
        }

        if (showDeleteDialog) {
            ConfirmAlertDialog(
                title = "Delete Recurring Expense",
                text = "Are you sure you want to delete this recurring expense?",
                subText = "This action cannot be undone.",
                onConfirm = {
                    deleteTemplate(deletionTemplatedId)
                    showDeleteDialog = false
                },
                toggleAlert = { showDeleteDialog = false }
            )
        }

        if (showEditSheet) {
            ModalBottomSheet(
                sheetState = editSheetState,
                onDismissRequest = { showEditSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth(0.95f)
                    ) {
                        RecurrenceType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                label = {
                                    Text(
                                        text = type.getDisplayName(),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                },
                                icon = {},
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = RecurrenceType.entries.size
                                ),
                                selected = editFrequency == type,
                                onClick = { editFrequency = type }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(0.95f),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pause Recurrence?",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Switch(
                            checked = editStatus,
                            onCheckedChange = { editStatus = it }
                        )
                    }

                    Button(
                        onClick = {
                            showEditSheet = false
                            editTemplate()
                        }
                    ) {
                        Text(
                            text = "Submit",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
