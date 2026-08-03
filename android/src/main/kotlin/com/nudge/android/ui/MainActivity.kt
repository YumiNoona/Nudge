package com.nudge.android.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nudge.android.BuildConfig
import com.nudge.android.ui.components.BottomDock
import com.nudge.android.ui.components.DockItem
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.NudgeTheme
import com.nudge.android.update.GitHubRelease
import com.nudge.android.update.GitHubUpdateChecker
import com.nudge.android.update.UpdateCheckResult
import com.nudge.android.update.InAppUpdateInstaller

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
            var tourDone by remember { mutableStateOf(prefs.getBoolean("product_tour_v2_complete", false)) }
            var availableUpdate by remember { mutableStateOf<GitHubRelease?>(null) }
            var updateStatus by remember { mutableStateOf<String?>(null) }
            var checkingUpdates by remember { mutableStateOf(false) }
            var updateProgress by remember { mutableStateOf<Float?>(null) }
            var pendingInstallPermission by remember { mutableStateOf<GitHubRelease?>(null) }
            var queuedUpdate by remember { mutableStateOf<GitHubRelease?>(null) }
            val scope = rememberCoroutineScope()
            val widgetAction by pendingAction.collectAsState()
            val installPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val release = pendingInstallPermission
                pendingInstallPermission = null
                if (release != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls())) {
                    queuedUpdate = release
                } else if (release != null) {
                    Toast.makeText(this@MainActivity, "Allow Nudge to install updates, then try again", Toast.LENGTH_LONG).show()
                }
            }

            fun checkForUpdates(manual: Boolean) {
                if (checkingUpdates) return
                checkingUpdates = true
                updateStatus = "Checking GitHub…"
                scope.launch {
                    when (val result = GitHubUpdateChecker.check(BuildConfig.VERSION_NAME)) {
                        is UpdateCheckResult.Available -> {
                            availableUpdate = result.release
                            updateStatus = "Version ${result.release.version} available"
                        }
                        is UpdateCheckResult.Current -> {
                            updateStatus = "Version ${BuildConfig.VERSION_NAME} · up to date"
                            if (manual) Toast.makeText(this@MainActivity, "Nudge is up to date", Toast.LENGTH_SHORT).show()
                        }
                        is UpdateCheckResult.Failed -> {
                            updateStatus = "Version ${BuildConfig.VERSION_NAME}"
                            if (manual) Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                    checkingUpdates = false
                }
            }

            LaunchedEffect(onboardingDone, tourDone) {
                if (onboardingDone && tourDone) {
                    val lastCheck = prefs.getLong("last_update_check_epoch", 0L)
                    if (System.currentTimeMillis() - lastCheck >= 24L * 60L * 60L * 1_000L) {
                        prefs.edit().putLong("last_update_check_epoch", System.currentTimeMillis()).apply()
                        checkForUpdates(manual = false)
                    }
                }
            }
            LaunchedEffect(queuedUpdate) {
                val release = queuedUpdate ?: return@LaunchedEffect
                updateProgress = 0f
                val result = InAppUpdateInstaller.downloadAndVerify(this@MainActivity, release) { progress ->
                    withContext(Dispatchers.Main.immediate) { updateProgress = progress }
                }
                updateProgress = null
                queuedUpdate = null
                result.onSuccess { apk ->
                    runCatching { InAppUpdateInstaller.install(this@MainActivity, apk) }
                        .onFailure { Toast.makeText(this@MainActivity, it.message ?: "Unable to start installation", Toast.LENGTH_LONG).show() }
                }.onFailure { error ->
                    Toast.makeText(this@MainActivity, error.message ?: "Update download failed", Toast.LENGTH_LONG).show()
                }
            }
            NudgeTheme(isDark = dark) {
                Box(Modifier.fillMaxSize()) {
                    Surface(Modifier.fillMaxSize()) {
                        when {
                            !onboardingDone -> OnboardingScreen(onDone = {
                                prefs.edit().putBoolean("onboarding_complete", true).apply()
                                onboardingDone = true
                            })
                            else -> ExpenseNavHost(
                                viewModel = vm,
                                isDark = dark,
                                requestedAction = widgetAction,
                                onActionConsumed = { pendingAction.value = WidgetAction.NONE },
                                onToggleTheme = {
                                    dark = !dark
                                    prefs.edit().putBoolean("dark_mode", dark).apply()
                                },
                                tourActive = !tourDone,
                                onTourComplete = {
                                    prefs.edit().putBoolean("product_tour_v2_complete", true).apply()
                                    tourDone = true
                                },
                                onCheckUpdates = { checkForUpdates(manual = true) },
                                updateStatus = updateStatus,
                            )
                        }
                    }
                    availableUpdate?.let { release ->
                        UpdateAvailableDialog(
                            release = release,
                            onDismiss = { availableUpdate = null },
                            onOpen = {
                                if (release.apkUrl == null) {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl)))
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
                                    pendingInstallPermission = release
                                    installPermissionLauncher.launch(
                                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")),
                                    )
                                } else {
                                    queuedUpdate = release
                                }
                                availableUpdate = null
                            },
                        )
                    }
                    updateProgress?.let { progress -> UpdateDownloadDialog(progress) }
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
        const val EXTRA_OPEN_TRANSACTIONS = "com.nudge.android.OPEN_TRANSACTIONS"
    }
}

private enum class WidgetAction { NONE, ADD, REVIEW, TRANSACTIONS }
private fun Intent?.toWidgetAction(): WidgetAction = when {
    this?.getBooleanExtra(MainActivity.EXTRA_OPEN_ADD, false) == true -> WidgetAction.ADD
    this?.getBooleanExtra(MainActivity.EXTRA_OPEN_REVIEW, false) == true -> WidgetAction.REVIEW
    this?.getBooleanExtra(MainActivity.EXTRA_OPEN_TRANSACTIONS, false) == true -> WidgetAction.TRANSACTIONS
    else -> WidgetAction.NONE
}

private enum class NavScreen {
    Transactions,
    Charts,
    Settings,
    Review,
    Accounts,
    Categories,
    Backup,
    SavedMessages,
    Donate,
}

@Composable
private fun ExpenseNavHost(
    viewModel: MainViewModel,
    isDark: Boolean,
    requestedAction: WidgetAction,
    onActionConsumed: () -> Unit,
    onToggleTheme: () -> Unit,
    tourActive: Boolean,
    onTourComplete: () -> Unit,
    onCheckUpdates: () -> Unit,
    updateStatus: String?,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE) }
    val stack = remember { mutableStateListOf(NavScreen.Transactions) }
    val current = stack.last()
    var showAdd by remember { mutableStateOf(false) }
    var tourStep by remember { mutableIntStateOf(0) }
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
            WidgetAction.TRANSACTIONS -> root(NavScreen.Transactions)
            WidgetAction.NONE -> Unit
        }
        if (requestedAction != WidgetAction.NONE) onActionConsumed()
    }

    LaunchedEffect(tourActive) {
        if (tourActive) {
            showAdd = false
            tourStep = 0
            root(NavScreen.Transactions)
        }
    }

    BackHandler(enabled = tourActive || showAdd || stack.size > 1 || current != NavScreen.Transactions) {
        when {
            tourActive && tourStep > 0 -> {
                tourStep--
                if (tourStep < 6) root(NavScreen.Transactions)
            }
            tourActive -> onTourComplete()
            showAdd -> showAdd = false
            else -> back()
        }
    }

    val smsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        smsGranted = grants.values.all { it }
        prefs.edit().putBoolean("sms_granted", smsGranted).apply()
    }

    val tourTargetRegistry = remember { TourTargetRegistry() }
    CompositionLocalProvider(LocalTourTargetRegistry provides tourTargetRegistry) {
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
                NavScreen.Transactions -> HistoryScreen(
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
                NavScreen.Charts -> ChartsScreen(transactions, categories)
                NavScreen.Settings -> ExpenseSettingsScreen(
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
                    onSavedMessages = { push(NavScreen.SavedMessages) },
                    onDonate = { push(NavScreen.Donate) },
                    onCheckUpdates = onCheckUpdates,
                    updateStatus = updateStatus,
                )
                NavScreen.Review -> NeedsReviewSwipeScreen(
                    transactions.filter { !it.isReviewed }, categories, accounts, sources,
                    onCategorize = viewModel::reviewTransaction,
                    onCreateCategory = viewModel::createCategoryForTransaction,
                    decryptSource = viewModel::decryptSourceBody,
                    onDismiss = viewModel::rejectTransaction,
                    onBack = ::back
                )
                NavScreen.Accounts -> ManageAccountsScreen(
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
                NavScreen.Donate -> DonateScreen(onBack = ::back)
            }
        }

        if (current == NavScreen.Charts || current == NavScreen.Transactions) {
            BottomDock(
                items = listOf(
                    DockItem("transactions", { Lucide.ListTodo(size = 23.dp, color = it) }, "Transactions", transactions.count { !it.isReviewed }),
                    DockItem("add", { Lucide.Plus(size = 24.dp, color = it) }, "Add"),
                    DockItem("charts", { Lucide.ChartBar(size = 23.dp, color = it) }, "Analytics")
                ),
                activeId = if (current == NavScreen.Charts) "charts" else "transactions",
                onSelect = { if (it == "charts") root(NavScreen.Charts) else root(NavScreen.Transactions) },
                onFabClick = { showAdd = true },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
            )
        }

        if (tourActive) {
            AppTourOverlay(
                step = tourStep,
                targetRegistry = tourTargetRegistry,
                onBack = {
                    if (tourStep > 0) {
                        tourStep--
                        if (tourStep < 6) root(NavScreen.Transactions)
                    }
                },
                onNext = {
                    if (tourStep == 6) {
                        onTourComplete()
                    } else {
                        tourStep++
                        if (tourStep == 6) root(NavScreen.Charts)
                    }
                },
                onSkip = onTourComplete,
            )
        }
    }
    }

    if (showAdd) AddTransactionSheet(categories, accounts, onDismiss = { showAdd = false }) { amount, type, merchant, account, category, note ->
        viewModel.addTransaction(amount, type, merchant, account, category, note)
        showAdd = false
    }
}

@Composable
private fun UpdateAvailableDialog(
    release: GitHubRelease,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Lucide.Download(size = 25.dp) },
        title = { androidx.compose.material3.Text("Update ${release.version} available") },
        text = {
            androidx.compose.material3.Text(
                release.notes.take(500).ifBlank { "A newer version of Nudge is ready on GitHub Releases." },
                maxLines = 9,
            )
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = onOpen) {
                androidx.compose.material3.Text(if (release.apkUrl != null) "Download & install" else "View release")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { androidx.compose.material3.Text("Later") }
        },
    )
}

@Composable
private fun UpdateDownloadDialog(progress: Float) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { },
        icon = { Lucide.Download(size = 25.dp) },
        title = { androidx.compose.material3.Text("Preparing update") },
        text = {
            Column {
                androidx.compose.material3.Text("Downloading and verifying the signed Nudge APK…")
                Spacer(Modifier.height(14.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(7.dp))
                androidx.compose.material3.Text("${(progress * 100).toInt()}%", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = { },
    )
}
