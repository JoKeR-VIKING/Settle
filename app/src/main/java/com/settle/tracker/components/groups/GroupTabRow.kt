package com.settle.tracker.components.groups

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import com.settle.tracker.screens.GroupTab

@Composable
fun GroupTabRow(
    selectedTab: GroupTab,
    onTabSelected: (GroupTab) -> Unit
) {
    SecondaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
    ) {
        Tab(
            selected = selectedTab == GroupTab.EXPENSES,
            onClick = { onTabSelected(GroupTab.EXPENSES) },
            text = {
                Text(
                    "Expenses",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        Tab(
            selected = selectedTab == GroupTab.RECURRING_EXPENSES,
            onClick = { onTabSelected(GroupTab.RECURRING_EXPENSES) },
            text = {
                Text(
                    "Recurring Expenses",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        )

        Tab(
            selected = selectedTab == GroupTab.BALANCES,
            onClick = { onTabSelected(GroupTab.BALANCES) },
            text = {
                Text(
                    "Balances",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}
