package com.settle.tracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.settle.tracker.components.ForceUpdateOverlay
import com.settle.tracker.components.UpdateBanner
import com.settle.tracker.utils.UpdateStatus
import com.settle.tracker.utils.checkForUpdate
import com.settle.tracker.screens.AccountScreen
import com.settle.tracker.screens.AddEditExpenseScreen
import com.settle.tracker.screens.AnalyticsScreen
import com.settle.tracker.screens.ExpensesScreen
import com.settle.tracker.screens.GroupExpensesScreen
import com.settle.tracker.screens.GroupsScreen
import com.settle.tracker.screens.LoginScreen
import com.settle.tracker.screens.PhoneVerificationScreen
import com.settle.tracker.ui.theme.SettleTheme
import com.settle.tracker.utils.LocalThemeState
import com.settle.tracker.utils.SettlePermission
import com.settle.tracker.utils.SettlePrefs
import com.settle.tracker.utils.createSmsNotificationChannel
import com.settle.tracker.utils.rememberPermissionRequester
import com.settle.tracker.utils.rememberThemeState
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

    object Analytics : Screen("analytics")
}

class MainActivity : ComponentActivity() {
    private var destination by mutableStateOf<String?>(null)
    private var mode by mutableStateOf<String?>(null)
    private var smsExpenseId by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
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

        // NOTE: Permissions are now requested on-demand at the exact moment
        // the user needs them (SMS when tapping "Add From SMS", Contacts when
        // adding members, Notifications right after first login).

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
    val themeState = rememberThemeState(activity)
    var currentUser by remember { mutableStateOf(googleAuthClient.getSignedInUser()) }
    val startDestination = remember {
        when {
            currentUser == null -> Screen.Login.route
            currentUser?.phoneNumber.isNullOrBlank() -> Screen.PhoneVerification.route
            else -> Screen.Expenses.route
        }
    }

    val db = Firebase.firestore
    val prefs = remember { SettlePrefs(activity.applicationContext) }

    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.UpToDate) }
    var showUpdateBanner by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        updateStatus = checkForUpdate(BuildConfig.VERSION_CODE)
    }

    // Initial permission sequence: Notifications -> Read SMS -> Receive SMS (polite ask)
    val receiveSmsPermission = rememberPermissionRequester(
        permission = SettlePermission.ReceiveSms,
        showSettingsOnDenial = false
    ) { }
    val readSmsPermission = rememberPermissionRequester(
        permission = SettlePermission.ReadSms,
        showSettingsOnDenial = false
    ) { receiveSmsPermission.request() }
    val initialPermissions = rememberPermissionRequester(
        permission = SettlePermission.Notifications,
        showSettingsOnDenial = false
    ) { readSmsPermission.request() }

    LaunchedEffect(currentUser) {
        if (currentUser != null && prefs.isFirstRun(SettlePrefs.PROMPT_INITIAL_PERMISSIONS)) {
            prefs.markSeen(SettlePrefs.PROMPT_INITIAL_PERMISSIONS)
            initialPermissions.request()
        }
    }

    val onLoginSuccess: (FirebaseUser) -> Unit = { signedInUser ->
        val handleFailure = { e: Exception ->
            Log.e("Firestore", "${e.message}")
            scope.launch {
                googleAuthClient.signOut()
                currentUser = null
            }
        }

        try {
            val userRef = db.collection("users").document(signedInUser.uid)
            val userData = mutableMapOf<String, Any>(
                "id" to signedInUser.uid,
                "name" to (signedInUser.displayName ?: ""),
                "email" to (signedInUser.email ?: ""),
                "phoneNumber" to (signedInUser.phoneNumber ?: ""),
                "photoUrl" to (signedInUser.photoUrl ?: "")
            )

            userRef.get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        userData["upiId"] = ""
                        userData["tourTaken"] = false
                    }
                    userRef.set(userData, SetOptions.merge())
                        .addOnSuccessListener {
                            currentUser = signedInUser
                            FirebaseMessaging.getInstance().token.addOnSuccessListener {
                                saveTokenToFirestore(it)
                            }
                            if (signedInUser.phoneNumber == null) {
                                navController.navigate(Screen.PhoneVerification.route) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate(Screen.Expenses.route) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                        .addOnFailureListener { e -> handleFailure(e) }
                }
                .addOnFailureListener { e -> handleFailure(e) }
        } catch (e: Exception) {
            handleFailure(e)
        }
    }

    LaunchedEffect(destination, smsExpenseId, mode, currentUser) {
        if (currentUser == null) return@LaunchedEffect
        when (destination) {
            "add_edit_expense" -> {
                if (smsExpenseId != null && mode != null) {
                    navController.navigate(
                        Screen.AddEditExpense.createRoute(mode = mode, expenseId = smsExpenseId)
                    ) { launchSingleTop = true }
                }
            }
            "groups" -> {
                navController.navigate(Screen.Groups.route) { launchSingleTop = true }
            }
        }
    }

    SettleTheme(themeMode = themeState.mode.value) {
        CompositionLocalProvider(
            LocalThemeState provides themeState
        ) {
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
                    enterTransition = {
                        fadeIn(tween(300)) + scaleIn(
                            initialScale = 0.98f,
                            animationSpec = tween(300)
                        )
                    },
                    exitTransition = {
                        fadeOut(tween(220))
                    },
                    popEnterTransition = {
                        fadeIn(tween(300)) + scaleIn(
                            initialScale = 1.02f,
                            animationSpec = tween(300)
                        )
                    },
                    popExitTransition = {
                        fadeOut(tween(220)) + scaleOut(
                            targetScale = 0.98f,
                            animationSpec = tween(220)
                        )
                    }
                ) {
                    composable(Screen.Login.route) {
                        LoginScreen(googleAuthClient, onLoginSuccess = onLoginSuccess)
                    }
                    composable(Screen.PhoneVerification.route) {
                        PhoneVerificationScreen(
                            onPhoneVerified = {
                                navController.navigate(Screen.Expenses.route) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(Screen.Expenses.route) {
                        ExpensesScreen(
                            onAddExpense = {
                                navController.navigate(Screen.AddEditExpense.createRoute(mode = "ADD")) {
                                    launchSingleTop = true
                                }
                            },
                            onEditExpense = { mode, expenseId ->
                                navController.navigate(
                                    Screen.AddEditExpense.createRoute(mode = mode, expenseId = expenseId)
                                ) { launchSingleTop = true }
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
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(
                        Screen.AddEditExpense.route,
                        arguments = listOf(
                            navArgument(Screen.AddEditExpense.ARG_MODE) {
                                type = NavType.StringType
                                nullable = false
                                defaultValue = "ADD"
                            },
                            navArgument(Screen.AddEditExpense.ARG_EXPENSE_ID) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument(Screen.AddEditExpense.ARG_GROUP_ID) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { navBackStackEntry ->
                        val m = navBackStackEntry.arguments?.getString(Screen.AddEditExpense.ARG_MODE) ?: "ADD"
                        val expenseId = navBackStackEntry.arguments?.getString(Screen.AddEditExpense.ARG_EXPENSE_ID)
                        val groupId = navBackStackEntry.arguments?.getString(Screen.AddEditExpense.ARG_GROUP_ID)

                        AddEditExpenseScreen(
                            onBack = { navController.popBackStack() },
                            currentUser = currentUser!!,
                            mode = m,
                            expenseId = expenseId,
                            groupId = groupId
                        )
                    }
                    composable(Screen.Groups.route) {
                        GroupsScreen(
                            currentUser = currentUser!!,
                            onOpenGroup = { groupId ->
                                navController.navigate(Screen.GroupExpenses.createRoute(groupId)) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(
                        Screen.GroupExpenses.route,
                        arguments = listOf(
                            navArgument(Screen.GroupExpenses.ARG_GROUP_ID) {
                                type = NavType.StringType
                                nullable = false
                                defaultValue = ""
                            }
                        )
                    ) { navBackStackEntry ->
                        val groupId = navBackStackEntry.arguments?.getString(Screen.GroupExpenses.ARG_GROUP_ID)
                        GroupExpensesScreen(
                            groupId = groupId!!,
                            onBack = { navController.popBackStack() },
                            onAddExpense = {
                                navController.navigate(
                                    Screen.AddEditExpense.createRoute(mode = "ADD", groupId = groupId)
                                ) { launchSingleTop = true }
                            },
                            onEditExpense = { mode, expenseId ->
                                navController.navigate(
                                    Screen.AddEditExpense.createRoute(
                                        mode = mode,
                                        expenseId = expenseId,
                                        groupId = groupId
                                    )
                                ) { launchSingleTop = true }
                            }
                        )
                    }
                    composable(Screen.Analytics.route) {
                        AnalyticsScreen(currentUser = currentUser!!)
                    }
                }

                UpdateBanner(
                    visible = updateStatus is UpdateStatus.OptionalUpdate && showUpdateBanner,
                    onDismiss = { showUpdateBanner = false },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        } // CompositionLocalProvider

        val forceStatus = updateStatus
        if (forceStatus is UpdateStatus.ForceUpdate) {
            ForceUpdateOverlay()
        }
    }
}
