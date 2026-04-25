package com.settle.tracker.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.settle.tracker.components.analytics.groups.GroupAnalytics
import com.settle.tracker.components.analytics.personal.PersonalAnalytics
import com.settle.tracker.components.common.CoachMarkOverlay
import com.settle.tracker.components.common.CoachStep
import com.settle.tracker.utils.SettlePrefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val prefs = remember { SettlePrefs(context.applicationContext) }

    var selectedSection by remember { mutableStateOf(SideBarItems.PERSONAL) }
    var showCoach by remember { mutableStateOf(prefs.isFirstRun(SettlePrefs.TUTORIAL_ANALYTICS)) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AnalyticsDrawer(
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
                                if (drawerState.isClosed) drawerState.open()
                                else drawerState.close()
                            }
                        }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    windowInsets = WindowInsets(top = 0),
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
                    SideBarItems.GROUPS -> GroupAnalytics()
                }

                CoachMarkOverlay(
                    visible = showCoach,
                    title = "Your money, visualised",
                    steps = listOf(
                        CoachStep(
                            icon = Icons.Filled.Menu,
                            title = "Tap the menu icon",
                            body = "The ☰ menu at the top-left switches between your Personal and Group analytics."
                        ),
                        CoachStep(
                            icon = Icons.Filled.Person,
                            title = "Personal insights",
                            body = "See monthly trends, daily spend, category breakdowns, and unusual spikes — all on your own expenses."
                        ),
                        CoachStep(
                            icon = Icons.Filled.Groups,
                            title = "Group insights",
                            body = "Pick any group from the chips at the top to see who spent the most, categories, and monthly trends."
                        ),
                        CoachStep(
                            icon = Icons.Filled.FilterList,
                            title = "Filter the range",
                            body = "Use the \"Last 3 / 6 / 12 months\" chips above the charts to zoom in or zoom out."
                        ),
                        CoachStep(
                            icon = Icons.Filled.BarChart,
                            title = "Keep logging",
                            body = "The more expenses you add, the sharper these insights get. Add one right from the Expenses tab."
                        ),
                    ),
                    onDismiss = {
                        showCoach = false
                        prefs.markSeen(SettlePrefs.TUTORIAL_ANALYTICS)
                    }
                )
            }
        }
    }
}
