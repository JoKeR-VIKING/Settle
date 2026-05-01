package com.settle.tracker.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.settle.tracker.AppDatabase
import com.settle.tracker.components.expenses.SmsExpenseModal
import com.settle.tracker.scheme.ExpenseScheme
import com.settle.tracker.ui.animations.bounceClickable
import com.settle.tracker.ui.theme.BrandBlue
import com.settle.tracker.ui.theme.BrandTeal
import com.settle.tracker.utils.SettlePermission
import com.settle.tracker.utils.rememberPermissionRequester

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FabMenu(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (String, String?) -> Unit
) {
    var showSmsModal by remember { mutableStateOf(false) }

    // On-demand SMS permissions: ask only when user taps "Add From SMS"
    val readSmsReq = rememberPermissionRequester(SettlePermission.ReadSms) { granted ->
        if (granted) {
            showSmsModal = true
            onToggleExpanded()
        }
    }

    val smsModalSheetstate = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = LocalContext.current
    val expenseDao = AppDatabase.getInstance(context).expenseDraftDao()
    val drafts by expenseDao.getAll().collectAsState(initial = emptyList())
    val expenses = drafts.map {
        ExpenseScheme(
            id = "",
            timestamp = it.timestamp,
            details = it.details,
            amount = it.amount,
            category = it.category,
            paidFrom = it.paidFrom
        )
    }
    val expenseIds = drafts.map { it.id }

    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "Fab Rotation"
    )
    val scale by animateFloatAsState(
        targetValue = if (expanded) 1.08f else 1f,
        animationSpec = tween(280),
        label = "Fab Scale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = slideInHorizontally(tween(240)) { it } + expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                exit = slideOutHorizontally(tween(200)) { it } + shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
            ) {
                SmallFab(
                    label = "Add Expense",
                    icon = Icons.Filled.Receipt,
                    onClick = onAddExpense
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = slideInHorizontally(tween(300)) { it } + expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                exit = slideOutHorizontally(tween(220)) { it } + shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
            ) {
                SmallFab(
                    label = "Add From SMS",
                    icon = Icons.Filled.Sms,
                    onClick = { readSmsReq.request() }
                )
            }

            // Main brand-gradient FAB
            Box(
                modifier = Modifier
                    .size((56 * scale).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(BrandTeal, BrandBlue))
                    )
                    .bounceClickable {
                        onToggleExpanded()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Expense Floating Button",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(26.dp)
                        .rotate(rotation)
                )
            }
        }

        if (showSmsModal) {
            SmsExpenseModal(
                sheetState = smsModalSheetstate,
                onDismissRequest = { showSmsModal = false },
                expenses = expenses,
                expenseIds = expenseIds,
                onEditExpense = onEditExpense
            )
        }
    }
}

@Composable
private fun SmallFab(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .bounceClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
