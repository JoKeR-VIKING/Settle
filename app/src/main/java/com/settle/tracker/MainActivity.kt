package com.settle.tracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import com.settle.tracker.components.BottomBar
import com.settle.tracker.components.BottomBarScreen
import com.settle.tracker.screens.AccountScreen
import com.settle.tracker.screens.AddEditExpenseScreen
import com.settle.tracker.screens.ExpensesScreen
import com.settle.tracker.screens.GroupExpensesScreen
import com.settle.tracker.screens.GroupsScreen
import com.settle.tracker.screens.LoginScreen
import com.settle.tracker.screens.PhoneVerificationScreen
import com.settle.tracker.ui.theme.SettleTheme
import com.settle.tracker.utils.Permissions
import com.settle.tracker.utils.createSmsNotificationChannel
import com.settle.tracker.utils.saveTokenToFirestore
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object PhoneVerification : Screen("phone_verification")
    object Expenses : Screen("expenses")
    object Account : Screen("account")
    object AddEditExpense :
        Screen("add_edit_expense?mode={mode}&expenseId={expenseId}&groupId={groupId}") {
        const val ARG_MODE = "mode"
        const val ARG_EXPENSE_ID = "expenseId"
        const val ARG_GROUP_ID = "groupId"

        fun createRoute(
            mode: String,
            expenseId: String? = null,
            groupId: String? = null
        ) =
            "add_edit_expense?mode=$mode&expenseId=$expenseId&groupId=$groupId"
    }

    object Groups : Screen("groups")
    object GroupExpenses : Screen("group_expenses?groupId={groupId}") {
        const val ARG_GROUP_ID = "groupId"

        fun createRoute(
            groupId: String
        ) = "group_expenses?groupId=$groupId"
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var permissions: Permissions

    private var destination by mutableStateOf<String?>(null)
    private var mode by mutableStateOf<String?>(null)
    private var smsExpenseId by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent ?: return

        setIntent(intent)
        updateIntent(intent)
    }

    private fun updateIntent(intent: Intent) {
        destination = intent.getStringExtra("destination")
        mode = intent.getStringExtra("mode")
        smsExpenseId = intent.getStringExtra("smsExpenseId")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        createSmsNotificationChannel(this)

        permissions = Permissions(this)
        lifecycle.addObserver(permissions)

        updateIntent(intent)

        setContent {
            AppContent(
                activity = this,
                destination = destination,
                mode = mode,
                smsExpenseId = smsExpenseId
            )
        }
    }
}

@Composable
private fun AppContent(
    activity: MainActivity,
    destination: String?,
    mode: String?,
    smsExpenseId: String?
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val scope = rememberCoroutineScope()

    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in BottomBarScreen.routes

    val googleAuthClient = remember { GoogleAuthClient(activity) }
    var currentUser by remember { mutableStateOf(googleAuthClient.getSignedInUser()) }
    val startDestination = remember {
        if (currentUser == null) {
            Screen.Login.route
        } else if (
            currentUser?.phoneNumber == null ||
            currentUser?.phoneNumber?.isBlank() == true
        ) {
            Screen.PhoneVerification.route
        } else {
            Screen.Expenses.route
        }
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

            val userData = mutableMapOf<String, Any>(
                "id" to signedInUser.uid,
                "name" to (signedInUser.displayName ?: ""),
                "email" to (signedInUser.email ?: ""),
                "phoneNumber" to (signedInUser.phoneNumber ?: ""),
                "photoUrl" to (signedInUser.photoUrl ?: "")
            )

            userRef
                .get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        userData["upiId"] = ""
                    }

                    userRef
                        .set(userData, SetOptions.merge())
                        .addOnSuccessListener {
                            currentUser = signedInUser

                            FirebaseMessaging
                                .getInstance()
                                .token
                                .addOnSuccessListener { token ->
                                    saveTokenToFirestore(token)
                                }

                            if (signedInUser.phoneNumber == null) {
                                navController.navigate(Screen.PhoneVerification.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            } else {
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
                }
                .addOnFailureListener { e ->
                    handleFailure(e)
                }
        } catch (e: Exception) {
            handleFailure(e)
        }
    }

    LaunchedEffect(
        destination,
        smsExpenseId,
        mode,
        currentUser
    ) {
        if (currentUser == null) return@LaunchedEffect

        when (destination) {
            "add_edit_expense" -> {
                if (smsExpenseId != null && mode != null) {
                    navController.navigate(
                        Screen.AddEditExpense.createRoute(
                            mode = mode,
                            expenseId = smsExpenseId
                        )
                    ) {
                        launchSingleTop = true
                    }
                }
            }
            "groups" -> {
                navController.navigate(Screen.Groups.route) {
                    launchSingleTop = true
                }
            }
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
                    composable(Screen.PhoneVerification.route) {
                        PhoneVerificationScreen(
                            onPhoneVerified = {
                                navController.navigate(Screen.Expenses.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(Screen.Expenses.route) {
                        ExpensesScreen(
                            onAddExpense = {
                                navController.navigate(
                                    Screen.AddEditExpense.createRoute(
                                        mode = "ADD"
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            },
                            onEditExpense = { mode, expenseId ->
                                navController.navigate(
                                    Screen.AddEditExpense.createRoute(
                                        mode = mode,
                                        expenseId = expenseId
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            },
                            currentUser = currentUser!!
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
                    composable(
                        Screen.AddEditExpense.route,
                        arguments = listOf(
                            navArgument(name = Screen.AddEditExpense.ARG_MODE) {
                                type = NavType.StringType
                                nullable = false
                                defaultValue = "ADD"
                            },
                            navArgument(name = Screen.AddEditExpense.ARG_EXPENSE_ID) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument(name = Screen.AddEditExpense.ARG_GROUP_ID) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { navBackStackEntry ->
                        val mode = navBackStackEntry
                            .arguments
                            ?.getString(Screen.AddEditExpense.ARG_MODE)
                            ?: "ADD"
                        val expenseId = navBackStackEntry
                            .arguments
                            ?.getString(Screen.AddEditExpense.ARG_EXPENSE_ID)
                        val groupId = navBackStackEntry
                            .arguments
                            ?.getString(Screen.AddEditExpense.ARG_GROUP_ID)

                        AddEditExpenseScreen(
                            onBack = {
                                navController.popBackStack()
                            },
                            currentUser = currentUser!!,
                            mode = mode,
                            expenseId = expenseId,
                            groupId = groupId
                        )
                    }
                    composable(Screen.Groups.route) {
                        GroupsScreen(
                            currentUser = currentUser!!,
                            onOpenGroup = { groupId ->
                                navController.navigate(
                                    Screen.GroupExpenses.createRoute(
                                        groupId
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(
                        Screen.GroupExpenses.route,
                        arguments = listOf(
                            navArgument(name = Screen.GroupExpenses.ARG_GROUP_ID) {
                                type = NavType.StringType
                                nullable = false
                                defaultValue = ""
                            }
                        )
                    ) { navBackStackEntry ->
                        val groupId = navBackStackEntry
                            .arguments
                            ?.getString(Screen.GroupExpenses.ARG_GROUP_ID)

                        GroupExpensesScreen(
                            groupId = groupId!!,
                            onBack = {
                                navController.popBackStack()
                            },
                            onAddExpense = {
                                navController.navigate(
                                    Screen.AddEditExpense.createRoute(
                                        mode = "ADD",
                                        groupId = groupId
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            },
                            onEditExpense = { mode, expenseId ->
                                navController.navigate(
                                    Screen.AddEditExpense.createRoute(
                                        mode = mode,
                                        expenseId = expenseId,
                                        groupId = groupId
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
