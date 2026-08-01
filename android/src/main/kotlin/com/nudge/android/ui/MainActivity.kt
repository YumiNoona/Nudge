package com.nudge.android.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import com.nudge.android.ui.components.BottomDock
import com.nudge.android.ui.components.DockItem
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.NudgeTheme

class MainActivity : ComponentActivity() {
    private val pendingAction = MutableStateFlow(WidgetAction.NONE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingAction.value = intent.toWidgetAction()
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val prefs = remember { getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE) }
            var dark by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
            var onboardingDone by remember { mutableStateOf(prefs.getBoolean("onboarding_complete", false)) }
            val widgetAction by pendingAction.collectAsState()
            NudgeTheme(isDark = dark) {
                Surface(Modifier.fillMaxSize()) {
                    if (!onboardingDone) {
                        OnboardingScreen(onDone = {
                            prefs.edit().putBoolean("onboarding_complete", true).apply()
                            onboardingDone = true
                        })
                    } else {
                        ExpenseNavHost(vm, dark, requestedAction = widgetAction, onActionConsumed = { pendingAction.value = WidgetAction.NONE }, onToggleTheme = {
                            dark = !dark
                            prefs.edit().putBoolean("dark_mode", dark).apply()
                        })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAction.value = intent.toWidgetAction()
    }

    companion object {
        const val EXTRA_OPEN_ADD = "com.nudge.android.OPEN_ADD"
        const val EXTRA_OPEN_REVIEW = "com.nudge.android.OPEN_REVIEW"
    }
}

private enum class WidgetAction { NONE, ADD, REVIEW }
private fun Intent?.toWidgetAction(): WidgetAction = when {
    this?.getBooleanExtra(MainActivity.EXTRA_OPEN_ADD, false) == true -> WidgetAction.ADD
    this?.getBooleanExtra(MainActivity.EXTRA_OPEN_REVIEW, false) == true -> WidgetAction.REVIEW
    else -> WidgetAction.NONE
}

// Legacy values remain temporarily so old, disconnected screens still compile.
enum class NavScreen { History, Charts, Settings, Review, Accounts, Categories, Backup, SavedMessages, Permissions, Home, More, Transactions, Wallet, Achievements, Challenges, Goals, Subscriptions, Budget, Envelope, Sync }

@Composable
private fun ExpenseNavHost(
    viewModel: MainViewModel,
    isDark: Boolean,
    requestedAction: WidgetAction,
    onActionConsumed: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE) }
    val stack = remember { mutableStateListOf(NavScreen.Transactions) }
    val current = stack.last()
    var showAdd by remember { mutableStateOf(false) }
    var captureEnabled by remember { mutableStateOf(prefs.getBoolean("auto_capture_enabled", true)) }
    var notificationEnabled by remember(current) { mutableStateOf(NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) }
    var smsGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val sources by viewModel.sourceMessages.collectAsState()
    val scanState by viewModel.captureScanState.collectAsState()

    fun root(destination: NavScreen) { stack.clear(); stack.add(destination) }
    fun push(destination: NavScreen) { if (stack.lastOrNull() != destination) stack.add(destination) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) else if (current != NavScreen.Transactions) root(NavScreen.Transactions) }

    LaunchedEffect(requestedAction) {
        when (requestedAction) {
            WidgetAction.ADD -> showAdd = true
            WidgetAction.REVIEW -> push(NavScreen.Review)
            WidgetAction.NONE -> Unit
        }
        if (requestedAction != WidgetAction.NONE) onActionConsumed()
    }

    BackHandler(enabled = showAdd || stack.size > 1 || current != NavScreen.Transactions) {
        if (showAdd) showAdd = false else back()
    }

    val smsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        smsGranted = grants.values.all { it }
        prefs.edit().putBoolean("sms_granted", smsGranted).apply()
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (fadeIn(spring()) + slideInHorizontally { it / 10 }) togetherWith
                    (fadeOut(spring()) + slideOutHorizontally { -it / 12 })
            },
            label = "expenseDestination"
        ) { screen ->
            when (screen) {
                NavScreen.History, NavScreen.Home, NavScreen.Transactions -> HistoryScreen(
                    transactions = transactions,
                    categories = categories,
                    accounts = accounts,
                    sources = sources,
                    decryptSource = viewModel::decryptSourceBody,
                    captureEnabled = captureEnabled,
                    onSettings = { push(NavScreen.Settings) },
                    onReview = { push(NavScreen.Review) },
                    onAdd = { showAdd = true },
                    onUpdate = viewModel::updateTransaction,
                    onDelete = viewModel::deleteTransaction
                )
                NavScreen.Charts -> ChartsScreen(transactions, categories, onBack = { root(NavScreen.Transactions) })
                NavScreen.Settings, NavScreen.More -> ExpenseSettingsScreen(
                    isDark = isDark,
                    captureEnabled = captureEnabled,
                    notificationEnabled = notificationEnabled,
                    smsGranted = smsGranted,
                    scanState = scanState,
                    onBack = ::back,
                    onToggleTheme = onToggleTheme,
                    onCaptureChanged = {
                        captureEnabled = it
                        prefs.edit().putBoolean("auto_capture_enabled", it).apply()
                    },
                    onRequestSms = { smsPermission.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)) },
                    onNotificationSettings = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        notificationEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                    },
                    onScanSms = viewModel::scanHistoricalSms,
                    onAccounts = { push(NavScreen.Accounts) },
                    onCategories = { push(NavScreen.Categories) },
                    onBackup = { push(NavScreen.Backup) },
                    onSavedMessages = { push(NavScreen.SavedMessages) }
                )
                NavScreen.Review -> NeedsReviewSwipeScreen(
                    transactions.filter { !it.isReviewed }, categories, accounts, sources,
                    onCategorize = viewModel::reviewTransaction,
                    onCreateCategory = viewModel::createCategoryForTransaction,
                    decryptSource = viewModel::decryptSourceBody,
                    onDismiss = { viewModel.deleteTransaction(it) },
                    onBack = ::back
                )
                NavScreen.Accounts, NavScreen.Wallet -> ManageAccountsScreen(
                    accounts, transactions,
                    onAdd = viewModel::saveAccount,
                    onUpdate = viewModel::saveAccount,
                    onDelete = viewModel::deleteAccount,
                    onBack = ::back
                )
                NavScreen.Categories -> CategoryManagerScreen(categories, viewModel::addCategory, viewModel::updateCategory, viewModel::deleteCategory, ::back)
                NavScreen.Backup -> BackupScreen(::back, viewModel)
                NavScreen.SavedMessages -> SavedMessagesScreen(
                    sources = sources,
                    transactions = transactions,
                    accounts = accounts,
                    categories = categories,
                    decryptSource = viewModel::decryptSourceBody,
                    onDeleteBody = viewModel::deleteSavedSourceBody,
                    onClearAll = viewModel::clearAllSavedSourceBodies,
                    onRetentionChanged = viewModel::applySourceRetention,
                    onBack = ::back
                )
                else -> HistoryScreen(
                    transactions, categories, accounts, sources, viewModel::decryptSourceBody, captureEnabled,
                    { push(NavScreen.Settings) }, { push(NavScreen.Review) }, { showAdd = true },
                    viewModel::updateTransaction, viewModel::deleteTransaction
                )
            }
        }

        if (current == NavScreen.History || current == NavScreen.Charts || current == NavScreen.Home || current == NavScreen.Transactions) {
            BottomDock(
                items = listOf(
                    DockItem("transactions", { Lucide.ListTodo(size = 23.dp, color = it) }, "Transactions", transactions.count { !it.isReviewed }),
                    DockItem("add", { Lucide.Plus(size = 24.dp, color = it) }, "Add"),
                    DockItem("charts", { Lucide.ChartBar(size = 23.dp, color = it) }, "Charts")
                ),
                activeId = if (current == NavScreen.Charts) "charts" else "transactions",
                onSelect = { if (it == "charts") root(NavScreen.Charts) else root(NavScreen.Transactions) },
                onFabClick = { showAdd = true },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
            )
        }
    }

    if (showAdd) AddTransactionSheet(categories, accounts, onDismiss = { showAdd = false }) { amount, type, merchant, account, category, note ->
        viewModel.addTransaction(amount, type, merchant, account, category, note)
        showAdd = false
    }
}
