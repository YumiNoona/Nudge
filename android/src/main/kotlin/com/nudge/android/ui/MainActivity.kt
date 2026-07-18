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

            val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
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
