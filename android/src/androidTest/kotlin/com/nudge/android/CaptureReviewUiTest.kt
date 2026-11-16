package com.nudge.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nudge.android.data.*
import com.nudge.android.ui.CaptureHealthDialog
import com.nudge.android.ui.NeedsReviewSwipeScreen
import com.nudge.android.ui.theme.NudgeTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureReviewUiTest {
    @get:Rule val compose = createComposeRule()
    private val txn = TransactionEntity("new", 10000, "debit", merchantRaw = "Cafe", merchantNormalized = "Cafe", categoryId = "food", accountId = "bank", source = "sms", timestampEpoch = 1700000000000)
    private val accounts = listOf(AccountEntity("bank", "Everyday Bank", "Demo Bank", "upi", "1234", null))
    private val categories = listOf(CategoryEntity("food", "Food & Dining", type = "expense"))
    private fun source(duplicate: String? = null) = SavedSourceMessageEntity("source", "new", "sms", "BANK", null, null, null, null, txn.timestampEpoch, txn.timestampEpoch, .4f,
        assessmentJson = CaptureAssessment(.4f, .99f, .98f, .25f, .98f, duplicate).toJson())

    @Test fun uncertainDetailsCanBeCorrectedBeforeConfirmation() {
        var corrected: TransactionEntity? = null
        compose.setContent { NudgeTheme(isDark = true) {
            NeedsReviewSwipeScreen(listOf(txn), categories, accounts, listOf(source()), { _, _ -> }, { _, _, _, _, _ -> }, { null }, {}, { corrected = it }, {}, listOf(txn), {})
        } }
        compose.onNodeWithText("Check the amount").assertIsDisplayed()
        compose.onNodeWithText("Confirm the account").assertIsDisplayed()
        compose.onNodeWithText("Edit details").performClick()
        compose.onNodeWithText("100.00").performTextReplacement("250.50")
        compose.onNodeWithText("Save & confirm").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(25050L, corrected?.amountCents); assertTrue(corrected?.isReviewed == true) }
    }
    @Test fun duplicateRemovalDoesNotInvokeMerchantRejection() {
        var removed: String? = null
        var rejected = false
        compose.setContent { NudgeTheme(isDark = true) {
            NeedsReviewSwipeScreen(listOf(txn), categories, accounts, listOf(source("old")), { _, _ -> }, { _, _, _, _, _ -> }, { null }, { rejected = true }, {}, { removed = it }, listOf(txn, txn.copy(id = "old")), {})
        } }
        compose.onNodeWithText("Compare payments").performClick()
        compose.onNodeWithText("Remove duplicate").performClick()
        compose.runOnIdle { assertEquals("new", removed); assertFalse(rejected) }
    }
    @Test fun captureHealthShowsPermissionAndEmptyActivityState() {
        compose.setContent { NudgeTheme(isDark = true) { CaptureHealthDialog(false, false, false) {} } }
        compose.onNodeWithText("Automatic capture: Paused").assertIsDisplayed()
        compose.onNodeWithText("SMS access: Not granted").assertIsDisplayed()
        compose.onNodeWithText("Notifications: Access not granted").assertIsDisplayed()
    }
}
