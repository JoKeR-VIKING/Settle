package com.settle.tracker.components

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

import androidx.navigation.NavHostController

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Person

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.unit.dp

import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomBarScreen(val route: String, val title: String, val icon: ImageVector) {
    object Expenses : BottomBarScreen("expenses", "Expenses", Icons.Filled.PieChart)
    object Account : BottomBarScreen("account", "Account", Icons.Filled.Person)

    companion object {
        val routes = listOf(
            Expenses.route,
            Account.route
        )
    }
}

@Composable
fun BottomBar(
    navController: NavHostController,
) {
    val screens = listOf(
        BottomBarScreen.Expenses,
        BottomBarScreen.Account,
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = Modifier
            .height(85.dp)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()

                drawLine(
                    color = Color(0xFF666666),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = strokeWidth,
                )
            },
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        screens.forEach { screen ->
            NavigationBarItem(
                label = {
                    Text(
                        screen.title,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                icon = {
                    Icon(
                        screen.icon,
                        contentDescription = screen.title,
                    )
                },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = screen.route != "account"
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    }
}
