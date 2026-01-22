package com.settle.tracker.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.settle.tracker.components.analytics.personal.PersonalAnalytics

enum class SideBarItems {
    PERSONAL,
    GROUPS,
    TRENDS;

    fun getDisplayName() = this.name.lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
fun AnalyticsScreen() {
    var selectedSection by remember { mutableStateOf(SideBarItems.PERSONAL) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
//            NavigationRail {
//                SideBarItems.entries.forEach {
//                    NavigationRailItem(
//                        modifier = Modifier.padding(vertical = 20.dp),
//                        selected = selectedSection == it,
//                        onClick = { selectedSection = it },
//                        icon = {
//                            Icon(
//                                imageVector = Icons.Filled.Person,
//                                contentDescription = it.getDisplayName()
//                            )
//                        },
//                        label = {
//                            Text(
//                                text = it.getDisplayName(),
//                                style = MaterialTheme.typography.labelLarge
//                            )
//                        }
//                    )
//                }
//            }

            when (selectedSection) {
                SideBarItems.PERSONAL -> PersonalAnalytics()
                SideBarItems.GROUPS -> {}
                SideBarItems.TRENDS -> {}
            }
        }
    }
}
