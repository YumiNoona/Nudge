package com.nudge.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nudge.android.ui.theme.NudgeTheme
import com.nudge.android.ui.theme.NudgeColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            var isDark by remember { mutableStateOf(false) }

            NudgeTheme(isDark = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavHost(viewModel, isDark) { isDark = !isDark }
                }
            }
        }
    }
}

enum class NavScreen { Home, Review, More, Achievements, Challenges, Goals, Subscriptions, Charts, Budget, Envelope, Backup, Sync }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainNavHost(
    viewModel: MainViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    var current by remember { mutableStateOf(NavScreen.Home) }

    val springSpec = spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
    )

    // Tie AnimatedContent around the screen swap
    AnimatedContent(
        targetState = current,
        transitionSpec = {
            (fadeIn(springSpec) + scaleIn(
                initialScale = 0.96f,
                animationSpec = springSpec
            )) togetherWith fadeOut(springSpec)
        },
        label = "screen"
    ) { screen ->
        when (screen) {
            NavScreen.Home -> HomeScreen(
                viewModel = viewModel,
                isDark = isDark,
                onToggleTheme = onToggleTheme,
                onNavigateToReview = { current = NavScreen.Review },
                onNavigateToMore = { current = NavScreen.More }
            )
            NavScreen.Review -> NeedsReviewSwipeScreen(
                transactions = viewModel.transactions.value.filter { !it.isReviewed },
                categories = viewModel.categories.value,
                onCategorize = { id, catId -> viewModel.reviewTransaction(id, catId) },
                onDismiss = { id -> viewModel.reviewTransaction(id, "") },
                onBack = { current = NavScreen.Home }
            )
            NavScreen.More -> MoreScreen(
                onBack = { current = NavScreen.Home },
                onNavigate = { current = it }
            )
            NavScreen.Achievements -> AchievementsScreen(
                gamificationProfile = viewModel.gamificationProfile.value,
                onBack = { current = NavScreen.More }
            )
            NavScreen.Challenges -> ChallengesScreen(
                categories = viewModel.categories.value,
                transactions = viewModel.transactions.value,
                onBack = { current = NavScreen.More }
            )
            NavScreen.Goals -> SavingsGoalsScreen(
                transactions = viewModel.transactions.value,
                onBack = { current = NavScreen.More }
            )
            NavScreen.Subscriptions -> SubscriptionsScreen(
                transactions = viewModel.transactions.value,
                categories = viewModel.categories.value,
                recurringRules = emptyList(),
                onBack = { current = NavScreen.More }
            )
            NavScreen.Charts -> ChartsScreen(
                transactions = viewModel.transactions.value,
                categories = viewModel.categories.value,
                isDark = isDark,
                onBack = { current = NavScreen.Home }
            )
            NavScreen.Budget -> BudgetScreen(
                budgets = viewModel.budgets.collectAsState().value,
                categories = viewModel.categories.collectAsState().value,
                transactions = viewModel.transactions.collectAsState().value,
                isDark = isDark,
                onSave = { id, catId, amt, period, rollover, start ->
                    viewModel.saveBudget(id, catId, amt, period, rollover, start)
                },
                onDelete = { id -> viewModel.deleteBudget(id) },
                onBack = { current = NavScreen.Home }
            )
            else -> HomeScreen(
                viewModel = viewModel,
                isDark = isDark,
                onToggleTheme = onToggleTheme
            )
        }
    }
}

@Composable
fun MoreScreen(
    onBack: () -> Unit,
    onNavigate: (NavScreen) -> Unit
) {
    val items = listOf(
        Triple(NavScreen.Achievements, "🏆", "Achievements"),
        Triple(NavScreen.Challenges, "🎯", "Challenges"),
        Triple(NavScreen.Goals, "🐷", "Savings Goals"),
        Triple(NavScreen.Subscriptions, "📅", "Subscriptions"),
        Triple(NavScreen.Charts, "📊", "Analytics"),
        Triple(NavScreen.Budget, "💰", "Budgets"),
        Triple(NavScreen.Envelope, "✉️", "Envelope Budget"),
        Triple(NavScreen.Backup, "💾", "Backup & Data"),
        Triple(NavScreen.Sync, "☁️", "Sync Settings"),
    )
    // Simple grid of options
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back", color = NudgeColors.InkSoft) }
        Spacer(modifier = Modifier.height(16.dp))
        Text("More", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { (screen, icon, label) ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate(screen) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NudgeColors.Surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(icon, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NudgeColors.Ink)
                        }
                    }
                }
            }
        }
    }
}

private object NudgeRadius {
    const val SM = 8; const val MD = 14; const val LG = 20; const val XL = 28
}
