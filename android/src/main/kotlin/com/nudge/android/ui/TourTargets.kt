package com.nudge.android.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

enum class TourTarget {
    Profile,
    MonthSummary,
    TransactionFilters,
    TransactionsTab,
    AddButton,
    AnalyticsTab,
    AnalyticsHero,
}

class TourTargetRegistry {
    val bounds = mutableStateMapOf<TourTarget, Rect>()
}

val LocalTourTargetRegistry = compositionLocalOf<TourTargetRegistry?> { null }

fun Modifier.tourTarget(target: TourTarget): Modifier = composed {
    val registry = LocalTourTargetRegistry.current
    if (registry == null) this else onGloballyPositioned { coordinates ->
        registry.bounds[target] = coordinates.boundsInWindow()
    }
}
