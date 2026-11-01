package com.nudge.android

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nudge.android.data.*
import com.nudge.android.ui.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Calendar

/** Opt-in real-app captures. Run only with -PcaptureScreenshots and -e readmeScreenshots true. */
@RunWith(AndroidJUnit4::class)
class ReadmeScreenshotsTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun captureDarkModeScreens() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue(InstrumentationRegistry.getArguments().getString("readmeScreenshots") == "true")
        check(context.packageName.endsWith(".screenshots")) { "Use the isolated screenshot build" }
        context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("dark_mode", true)
            .putBoolean("onboarding_complete", true)
            .putBoolean("first_run_permissions_v1_complete", true)
            .putBoolean("product_tour_v2_complete", true)
            .putString("display_name", "Alex").commit()
        runBlocking {
            val db = NudgeDatabase.getInstance(context)
            // The package is isolated; clearing only makes repeat captures deterministic.
            db.clearAllTables()
            DefaultsSeeder.seedIfEmpty(db)
            db.accountDao().getAllOnce().forEach { db.accountDao().delete(it) }
            val categories = db.categoryDao().getAllOnce().associateBy { it.name }
            val accounts = listOf(
                AccountEntity("demo-bank", "Everyday Bank", "Demo Bank", "upi", "4821", "#3A6B58", isDefault = true),
                AccountEntity("demo-card", "Everyday Card", "Demo Bank", "credit_card", "9016", "#536788"),
                AccountEntity("demo-cash", "Cash", null, "cash", null, "#A68C55"),
            )
            db.accountDao().insertAll(accounts)
            val month = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
            data class Entry(val day: Int, val name: String, val rupees: Long, val category: String, val type: String = "debit", val account: String = "demo-bank")
            val entries = listOf(
                Entry(1, "Acme Studio · Salary", 62000, "Salary", "credit"),
                Entry(3, "Maple Apartments", 14500, "Rent"),
                Entry(5, "Fresh Market", 1840, "Groceries"),
                Entry(8, "City Metro", 450, "Transport"),
                Entry(10, "Sunday Bookshop", 890, "Shopping", account = "demo-card"),
                Entry(12, "Home Internet", 799, "Utilities"),
                Entry(15, "Design Project", 8500, "Freelance", "credit"),
                Entry(18, "Corner Cafe", 340, "Food & Dining", account = "demo-cash"),
                Entry(20, "Fresh Market", 1260, "Groceries"),
                Entry(22, "Cinema Club", 680, "Entertainment", account = "demo-card"),
                Entry(25, "Metro Pass", 1200, "Transport"),
                Entry(26, "Bookshop Refund", 290, "Refunds", "refund", "demo-card"),
                Entry(27, "Olive Kitchen", 1250, "Food & Dining"),
                Entry(28, "Morning Coffee", 180, "Food & Dining", account = "demo-cash"),
                Entry(28, "Green Basket", 940, "Groceries"),
            )
            db.transactionDao().insertAll(entries.mapIndexed { index, entry ->
                val date = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, entry.day); set(Calendar.HOUR_OF_DAY, 9 + index % 10); set(Calendar.MINUTE, 20); set(Calendar.SECOND, 0) }
                TransactionEntity(id = "readme-$index", amountCents = entry.rupees * 100, type = entry.type,
                    merchantRaw = entry.name, merchantNormalized = entry.name, categoryId = categories[entry.category]?.id,
                    accountId = entry.account, source = "manual", isReviewed = true, timestampEpoch = date.timeInMillis)
            })
            val thisMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 0)
            }.timeInMillis
            db.transactionDao().insertAll(listOf(
                TransactionEntity("readme-bank-current", 3545000, "credit", merchantRaw = "Monthly pay", merchantNormalized = "Monthly pay", categoryId = categories["Salary"]?.id, accountId = "demo-bank", source = "manual", isReviewed = true, timestampEpoch = thisMonth),
                TransactionEntity("readme-cash-current", 290000, "credit", merchantRaw = "Cash allowance", merchantNormalized = "Cash allowance", categoryId = categories["Other Income"]?.id, accountId = "demo-cash", source = "manual", isReviewed = true, timestampEpoch = thisMonth),
                TransactionEntity("readme-card-current", 180000, "debit", merchantRaw = "Weekend supplies", merchantNormalized = "Weekend supplies", categoryId = categories["Shopping"]?.id, accountId = "demo-card", source = "manual", isReviewed = true, timestampEpoch = thisMonth),
            ))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Previous month").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Previous month").performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithText("Green Basket").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Green Basket").assertIsDisplayed()
            capture(context, "transactions-dark.png")
            compose.onNodeWithText("ANALYTICS", ignoreCase = true).performClick()
            compose.onNodeWithContentDescription("Previous month").performClick()
            compose.onNodeWithText("Daily average").assertIsDisplayed()
            capture(context, "analytics-dark.png")
            // Also verify the redesigned details remain reachable below the fold.
            compose.onNodeWithText("Spending rhythm").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Transactions").performClick()
            compose.onNodeWithText("A", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Accounts", substring = false).performScrollTo().performClick()
            compose.onNodeWithText("TOTAL ACROSS ACCOUNTS").assertIsDisplayed()
            capture(context, "accounts-dark.png")
        }
    }

    private fun capture(context: Context, name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // Let platform ripples and the final display frame settle before capturing pixels.
        Thread.sleep(600)
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val folder = File(context.getExternalFilesDir(null), "readme-screenshots").apply { mkdirs() }
        File(folder, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
