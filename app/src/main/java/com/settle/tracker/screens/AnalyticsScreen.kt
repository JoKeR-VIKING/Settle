package com.settle.tracker.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.settle.tracker.components.analytics.personal.PersonalAnalytics
import kotlinx.coroutines.launch

enum class SideBarItems {
    PERSONAL,
    GROUPS,
    TRENDS;

    fun getDisplayName() = this.name.lowercase().replaceFirstChar { it.uppercase() }
    fun getDisplayIcon() = when (this) {
        PERSONAL -> Icons.Filled.Person
        GROUPS -> Icons.Filled.Groups
        TRENDS -> Icons.AutoMirrored.Filled.ShowChart
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedSection by remember { mutableStateOf(SideBarItems.PERSONAL) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                selectedSection = selectedSection,
                onItemClick = { item ->
                    selectedSection = item
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) {
                                    drawerState.open()
                                } else {
                                    drawerState.close()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    windowInsets = WindowInsets(
                        top = 0,
                    ),
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedSection) {
                    SideBarItems.PERSONAL -> PersonalAnalytics()
                    SideBarItems.GROUPS -> {}
                    SideBarItems.TRENDS -> {}
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
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
