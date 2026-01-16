package com.settle.tracker

import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.launch

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.NotificationManagerCompat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState

import androidx.compose.material3.Scaffold

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.firestore

import com.settle.tracker.ui.theme.SettleTheme
import com.settle.tracker.utils.Permissions
import com.settle.tracker.utils.createSmsNotificationChannel
import com.settle.tracker.components.BottomBar
import com.settle.tracker.components.BottomBarScreen
import com.settle.tracker.scheme.UserScheme
import com.settle.tracker.scheme.ExpenseDraft
import com.settle.tracker.screens.LoginScreen
import com.settle.tracker.screens.ExpensesScreen
import com.settle.tracker.screens.AccountScreen
import com.settle.tracker.screens.AddEditExpenseScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Expenses : Screen("expenses")
    object Account : Screen("account")
    object AddEditExpense : Screen("add_edit_expense?expenseId={expenseId}") {
        fun createRoute(expenseId: String? = null) =
            if (expenseId == null) "add_edit_expense"
            else "add_edit_expense?expenseId=$expenseId"
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var permissions: Permissions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        createSmsNotificationChannel(this)

        permissions = Permissions(this)
        lifecycle.addObserver(permissions)

        val openSmsModal = intent.getBooleanExtra("openSmsModal", false)
        NotificationManagerCompat.from(this).cancelAll()

        setContent {
            AppContent(
                activity = this,
                openSmsModal = openSmsModal
            )
        }
    }
}

@Composable
private fun AppContent(
    activity: MainActivity,
    openSmsModal: Boolean
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val scope = rememberCoroutineScope()

    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in BottomBarScreen.routes

    val googleAuthClient = remember { GoogleAuthClient(activity) }
    var currentUser by remember { mutableStateOf(googleAuthClient.getSignedInUser()) }
    val startDestination = remember {
        if (currentUser != null) Screen.Expenses.route else Screen.Login.route
    }

    val db = Firebase.firestore

    val onLoginSuccess: (FirebaseUser) -> Unit = { signedInUser ->
        val handleFailure = { e: Exception ->
            Log.e("Firestore", "${e.message}")
            scope.launch {
                googleAuthClient.signOut()
                currentUser = null
            }
        }

        try {
            val userRef =
                db
                    .collection("users")
                    .document(signedInUser.uid)

            userRef
                .get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        userRef
                            .set(
                                UserScheme(
                                    upiId = "",
                                )
                            )
                            .addOnSuccessListener { _ ->
                                currentUser = signedInUser
                                navController.navigate(Screen.Expenses.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                            .addOnFailureListener { e ->
                                handleFailure(e)
                            }
                    } else {
                        currentUser = signedInUser
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                }
                .addOnFailureListener { e ->
                    handleFailure(e)
                }
        } catch (e: Exception) {
            handleFailure(e)
        }
    }

    SettleTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (currentUser != null && showBottomBar) {
                    BottomBar(navController)
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                ) {
                    composable(Screen.Login.route) {
                        LoginScreen(
                            googleAuthClient,
                            onLoginSuccess = onLoginSuccess
                        )
                    }
                    composable(Screen.Expenses.route) {
                        ExpensesScreen(
                            onAddExpense = {
                                navController.navigate(Screen.AddEditExpense.createRoute()) {
                                    launchSingleTop = true
                                }
                            },
                            onEditExpense = { expense ->
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(
                                        "expenseDraft",
                                        expense
                                    )

                                navController.navigate(Screen.AddEditExpense.route) {
                                    launchSingleTop = true
                                }
                            },
                            currentUser = currentUser!!,
                            openSmsModal = openSmsModal
                        )
                    }
                    composable(Screen.Account.route) {
                        AccountScreen(
                            googleAuthClient,
                            onLogoutSuccess = {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(Screen.AddEditExpense.route) {
                        val expenseDraft =
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.get<ExpenseDraft>("expenseDraft")

                        LaunchedEffect(Unit) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.remove<ExpenseDraft>("expenseDraft")
                        }

                        AddEditExpenseScreen(
                            onBack = {
                                navController.popBackStack()
                            },
                            currentUser = currentUser!!,
                            expense = expenseDraft
                        )
                    }
                }
            }
        }
    }
}
