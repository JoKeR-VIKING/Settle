package com.settle.tracker.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class SideBarItems {
    PERSONAL,
    GROUPS;

    fun getDisplayName() = this.name.lowercase().replaceFirstChar { it.uppercase() }
    fun getDisplayIcon() = when (this) {
        PERSONAL -> Icons.Filled.Person
        GROUPS -> Icons.Filled.Groups
    }
}

@Composable
fun AnalyticsDrawer(
    selectedSection: SideBarItems,
    onItemClick: (SideBarItems) -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.6f)
    ) {
        Column {
            SideBarItems.entries.forEach { item ->
                NavigationDrawerItem(
                    label = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.getDisplayIcon(),
                                contentDescription = item.getDisplayName()
                            )

                            Text(
                                text = item.getDisplayName(),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    },
                    selected = selectedSection == item,
                    onClick = {
                        onItemClick(item)
                    }
                )
            }
        }
    }
}
