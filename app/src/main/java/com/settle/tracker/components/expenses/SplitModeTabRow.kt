package com.settle.tracker.components.expenses

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.settle.tracker.scheme.SplitMode

@Composable
fun SplitModeTabRow(
    modifier: Modifier = Modifier,
    splitMode: SplitMode,
    onTabSelected: (SplitMode) -> Unit
) {
    SecondaryTabRow(
        modifier = modifier,
        selectedTabIndex = splitMode.ordinal,
    ) {
        Tab(
            selected = splitMode == SplitMode.EQUAL,
            onClick = { onTabSelected(SplitMode.EQUAL) },
            text = {
                Text(
                    "Split Equally",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        Tab(
            selected = splitMode == SplitMode.UNEQUAL,
            onClick = { onTabSelected(SplitMode.UNEQUAL) },
            text = {
                Text(
                    "Split Unequally",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}
