package com.nudge.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nudge.android.ui.theme.NudgeColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            var isDark by remember { mutableStateOf(false) }
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
            var onboarded by remember {
                mutableStateOf(getSharedPreferences("nudge_ui", MODE_PRIVATE).getBoolean("onboarded", false))
            }

            val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!onboarded) {
                        OnboardingScreen(
                            isDark = isDark,
                            onComplete = {
                                getSharedPreferences("nudge_ui", MODE_PRIVATE).edit().putBoolean("onboarded", true).apply()
                                onboarded = true
                            }
                        )
                    } else AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            (fadeIn(tween(280)) + slideInHorizontally { it / 12 }) togetherWith
                                (fadeOut(tween(180)) + slideOutHorizontally { -it / 14 })
                        }
                    ) { screen ->
                        when (screen) {
                            Screen.Home -> HomeScreen(
                                viewModel = viewModel,
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark },
                                onNavigateToReview = { currentScreen = Screen.Review }
                            )
                            Screen.Review -> NeedsReviewSwipeScreen(
                                transactions = viewModel.transactions.value.filter { !it.isReviewed },
                                categories = viewModel.categories.value,
                                onCategorize = { id, catId -> viewModel.reviewTransaction(id, catId) },
                                onDismiss = { id -> /* mark as reviewed but uncategorized */ viewModel.reviewTransaction(id, "") },
                                onBack = { currentScreen = Screen.Home }
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class Screen {
    Home, Review
}
