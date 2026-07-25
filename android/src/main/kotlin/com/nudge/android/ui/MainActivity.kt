package com.nudge.android.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nudge.android.service.NudgeNotificationListener
import com.nudge.android.ui.components.BottomDock
import com.nudge.android.ui.components.BNavItem
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.NudgeColors
import com.nudge.android.ui.theme.NudgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            var isDark by remember { mutableStateOf(applicationContext
                .getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE)
                .getBoolean("dark_mode", false)) }

            NudgeTheme(isDark = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val onboardingDone = applicationContext
                        .getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE)
                        .getBoolean("onboarding_complete", false)

                    if (!onboardingDone) {
                        OnboardingScreen(onDone = {
                            applicationContext.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE)
                                .edit().putBoolean("onboarding_complete", true).apply()
                            // Force recomposition — setContent will re-render with onboardingDone=true
                            recreate()
                        })
                    } else {
                        MainNavHost(viewModel, isDark) { new ->
                            isDark = new
                            applicationContext.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE)
                                .edit().putBoolean("dark_mode", new).apply()
                        }
                    }
                }
            }
        }
    }
}

enum class NavScreen { Home, Review, More, Achievements, Challenges, Goals, Subscriptions, Charts, Budget, Envelope, Backup, Sync, Permissions, Accounts }

@Composable
fun MainNavHost(
    viewModel: MainViewModel,
    isDark: Boolean,
    onToggleTheme: (Boolean) -> Unit
) {
    var current by remember { mutableStateOf(NavScreen.Home) }
    var showAddSheet by remember { mutableStateOf(false) }
    val needsReviewCount by viewModel.needsReviewCount.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val context = LocalContext.current

    // SMS permission launcher
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val prefs = context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("sms_granted", grants.values.all { it }).apply()
    }

    // Notification access check
    val notificationEnabled = remember {
        NotificationManagerCompat.getEnabledListenerPackages(context).any { it == context.packageName }
    }

    val springSpec = spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
    )

    val showBottomNav = current in listOf(NavScreen.Home, NavScreen.Review, NavScreen.More)

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (fadeIn(springSpec) + scaleIn(initialScale = 0.96f, animationSpec = springSpec))
                    .togetherWith(fadeOut(springSpec))
            },
            label = "screen",
            modifier = Modifier.fillMaxSize()
        ) { screen ->
            when (screen) {
                NavScreen.Home -> HomeScreen(
                    viewModel = viewModel, isDark = isDark,
                    onToggleTheme = { onToggleTheme(!isDark) },
                    onNavigateToReview = { current = NavScreen.Review },
                    onNavigateToMore = { current = NavScreen.More }
                )
                NavScreen.Review -> NeedsReviewSwipeScreen(
                    transactions = transactions.filter { !it.isReviewed },
                    categories = categories,
                    onCategorize = { id, catId -> viewModel.reviewTransaction(id, catId) },
                    onDismiss = { id -> viewModel.reviewTransaction(id, "") },
                    onBack = { current = NavScreen.Home }
                )
                NavScreen.More -> MoreScreen(
                    isDark = isDark,
                    onToggleTheme = { onToggleTheme(!isDark) },
                    notificationEnabled = notificationEnabled,
                    smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED,
                    onBack = { current = NavScreen.Home },
                    onNavigate = { current = it },
                    onNavigateToAccounts = { current = NavScreen.Accounts },
                    onRequestSms = {
                        smsPermissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
                    },
                    onOpenNotificationSettings = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
                NavScreen.Accounts -> ManageAccountsScreen(
                    accounts = accounts,
                    transactions = transactions,
                    onAdd = { viewModel.addAccount(it.name, com.nudge.model.AccountType.valueOf(it.accountType.uppercase()), it.bankName, it.last4Digits) },
                    onUpdate = { /* update logic */ },
                    onDelete = { /* delete logic */ },
                    onBack = { current = NavScreen.More }
                )
                NavScreen.Achievements -> AchievementsScreen(
                    gamificationProfile = viewModel.gamificationProfile.value,
                    onBack = { current = NavScreen.More }
                )
                NavScreen.Challenges -> ChallengesScreen(
                    categories = categories, transactions = transactions,
                    onBack = { current = NavScreen.More }
                )
                NavScreen.Goals -> SavingsGoalsScreen(
                    transactions = transactions,
                    onBack = { current = NavScreen.More }
                )
                NavScreen.Subscriptions -> SubscriptionsScreen(
                    transactions = transactions, categories = categories,
                    recurringRules = emptyList(),
                    onBack = { current = NavScreen.More }
                )
                NavScreen.Charts -> ChartsScreen(
                    transactions = transactions, categories = categories,
                    isDark = isDark,
                    onBack = { current = NavScreen.Home }
                )
                NavScreen.Budget -> BudgetScreen(
                    budgets = budgets, categories = categories, transactions = transactions,
                    isDark = isDark,
                    onSave = { id, catId, amt, period, roll, start ->
                        viewModel.saveBudget(id, catId, amt, period, roll, start)
                    },
                    onDelete = { viewModel.deleteBudget(it) },
                    onBack = { current = NavScreen.Home }
                )
                NavScreen.Envelope -> EnvelopeBudgetScreen(
                    budgets = budgets, categories = categories, transactions = transactions,
                    onAddBudget = {}, onEditBudget = {},
                    onBack = { current = NavScreen.More }
                )
                NavScreen.Backup -> BackupScreen(
                    onBack = { current = NavScreen.More },
                    viewModel = viewModel
                )
                NavScreen.Sync -> SyncSettingsScreen(
                    onBack = { current = NavScreen.More }
                )
                NavScreen.Permissions -> PermissionsScreen(
                    notificationEnabled = notificationEnabled,
                    smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED,
                    onBack = { current = NavScreen.More },
                    onRequestSms = {
                        smsPermissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
                    },
                    onOpenNotificationSettings = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
        }

        // Bottom nav dock
        if (showBottomNav) {
            BottomDock(
                items = listOf(
                    BNavItem("home", { c -> Lucide.Home(size = 22.dp, strokeWidth = 1.8.dp, color = c) }, "Home"),
                    BNavItem("add",  { c -> Lucide.Plus(size = 22.dp, strokeWidth = 2.5.dp, color = c) }, ""),
                    BNavItem("review", { c -> Lucide.ListTodo(size = 22.dp, strokeWidth = 1.8.dp, color = c) }, "Review", needsReviewCount),
                    BNavItem("more", { c -> Lucide.Menu(size = 22.dp, strokeWidth = 1.8.dp, color = c) }, "More"),
                ),
                activeId = when (current) {
                    NavScreen.Home -> "home"
                    NavScreen.Review -> "review"
                    NavScreen.More -> "more"
                    else -> "home"
                },
                onSelect = { id ->
                    when (id) {
                        "home" -> current = NavScreen.Home
                        "review" -> current = NavScreen.Review
                        "more" -> current = NavScreen.More
                    }
                },
                onFabClick = { showAddSheet = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Add transaction modal sheet — available from anywhere via bottom nav FAB
    AnimatedVisibility(
        visible = showAddSheet,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        AddTransactionSheet(
            categories = categories,
            accounts = accounts,
            onDismiss = { showAddSheet = false },
            onAdd = { amount, type, merchant, accountId, categoryId, note ->
                viewModel.addTransaction(amount, type, merchant, accountId, categoryId, note)
                showAddSheet = false
            }
        )
    }
}

// ── Permissions Screen ──

@Composable
fun PermissionsScreen(
    notificationEnabled: Boolean,
    smsGranted: Boolean,
    onBack: () -> Unit,
    onRequestSms: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← Back", color = NudgeColors.InkSoft) }
        Spacer(Modifier.height(8.dp))
        Text("Permissions", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
        Spacer(Modifier.height(16.dp))

        // SMS
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = NudgeColors.Surface)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SMS Access", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NudgeColors.Ink)
                    Text(if (smsGranted) "Granted" else "Not granted", fontSize = 12.sp, color = if (smsGranted) NudgeColors.Emerald else NudgeColors.Coral)
                    Text("Auto-detect UPI & bank transaction SMS", fontSize = 11.sp, color = NudgeColors.InkMute)
                }
                if (!smsGranted) Button(onClick = onRequestSms, colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.Emerald)) { Text("Grant") }
                else Text("✓", color = NudgeColors.Emerald, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Notification listener
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = NudgeColors.Surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Notification Access", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NudgeColors.Ink)
                        Text(if (notificationEnabled) "Enabled" else "Not enabled", fontSize = 12.sp, color = if (notificationEnabled) NudgeColors.Emerald else NudgeColors.Coral)
                        Text("Capture GPay, PhonePe, Paytm notifications", fontSize = 11.sp, color = NudgeColors.InkMute)
                    }
                    if (!notificationEnabled) Button(onClick = onOpenNotificationSettings, colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.Emerald)) { Text("Open Settings") }
                    else Text("✓", color = NudgeColors.Emerald, fontSize = 20.sp)
                }
                if (!notificationEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Text("Find \"Nudge\" in the list and toggle it on", fontSize = 11.sp, color = NudgeColors.Amber)
                }
            }
        }
    }
}
