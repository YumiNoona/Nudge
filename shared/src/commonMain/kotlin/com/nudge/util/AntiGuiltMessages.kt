package com.nudge.util

/**
 * Anti-Dark-Pattern Messaging — §6.6 Guardrails.
 *
 * Principles:
 * - No fake urgency
 * - No shame-based messaging
 * - No push notifications designed purely to manipulate re-engagement
 * - All gamification notifications must carry genuine utility
 * - NEVER make overspending feel punished or shamed
 * - Frame everything as encouragement
 */
object AntiGuiltMessages {

    // --- Streak messages (anti-shame framing) ---

    const val STREAK_BROKEN = "Your streak paused — start a new one anytime, no pressure"
    const val STREAK_ACTIVE = "You're on a roll! Keep the momentum going"
    const val STREAK_MILESTONE_7 = "One week strong! Nice consistency"
    const val STREAK_MILESTONE_30 = "A full month! That's real dedication"
    const val STREAK_MILESTONE_100 = "100 days! You've built a solid habit"
    const val STREAK_MILESTONE_365 = "A whole year! Incredible commitment"
    const val STREAK_AT_RISK = "Quick check-in to keep your streak going — only if you have a moment"
    const val STREAK_FREEZE_EARNED = "You earned a streak freeze! One missed day won't reset your progress"
    const val STREAK_FREEZE_USED = "Your streak freeze kept you going — nice planning"

    // --- Budget messages (encouragement, not guilt) ---

    const val BUDGET_UNDER = "You're under budget — great job managing your spending!"
    const val BUDGET_APPROACHING = "Getting close to your budget limit. Here's what you've spent so far."
    const val BUDGET_EXCEEDED = "You've gone over budget. That's okay — it happens. Want to adjust your budget or review these expenses?"
    const val BUDGET_EMPTY = "This envelope is empty for now. Ready to refill next period?"

    // --- Overspend framing (never "you failed", always "let's adjust") ---

    const val OVERSPEND_TITLE = "Budget Adjustment Opportunity"
    const val OVERSPEND_BODY = "Your spending in %s was higher than planned. Would you like to adjust the budget or review what changed?"
    const val OVERSPEND_ACTION_REVIEW = "Review Transactions"
    const val OVERSPEND_ACTION_ADJUST = "Adjust Budget"
    const val OVERSPEND_ACTION_DISMISS = "I'll keep an eye on it"

    // --- Challenge messages (encouragement-focused) ---

    const val CHALLENGE_ASSIGNED = "Here's a new challenge to help you reach your goals"
    const val CHALLENGE_PROGRESS = "You're %d%% of the way — great progress!"
    const val CHALLENGE_COMPLETED = "Challenge complete! You earned %d XP. Want another?"
    const val CHALLENGE_MISSED = "Didn't quite hit this one — no worries, a fresh challenge starts tomorrow"
    const val CHALLENGE_CUSTOM_PROMPT = "Set your own goal and challenge yourself at your own pace"

    // --- Notification copy (must carry genuine utility) ---

    const val NOTIF_REVIEW_PENDING = "You have %d transaction(s) to review. Quick categorization helps keep things organized."
    const val NOTIF_BUDGET_ALERT = "Your %s budget is at %d%%. Want to take a look?"
    const val NOTIF_STREAK_AT_RISK = "Quick check-in to keep your streak going — only if you have a moment"
    const val NOTIF_BILL_REMINDER = "%s usually charges around the %dth — just a heads up"
    const val NOTIF_SAVINGS_MILESTONE = "You're %d%% toward your %s goal! Every bit adds up"
    const val NOTIF_WEEKLY_SUMMARY = "Your weekly spending summary is ready — you spent ₹%s this week"

    // --- Achievement messages (celebrate, never shame for not having) ---

    const val ACHIEVEMENT_UNLOCKED = "Achievement unlocked: %s!"
    const val ACHIEVEMENT_LOCKED_HINT = "Keep going — this one will unlock as you use the app more"
    const val ACHIEVEMENT_SECRET = "Some badges are hidden — discover them by exploring the app"

    // --- Level-up messages ---

    const val LEVEL_UP_TITLE = "Level Up!"
    const val LEVEL_UP_BODY = "You're now a %s! Your financial awareness is growing."

    // --- Empty states (never "you haven't done anything", always "let's get started") ---

    const val EMPTY_TRANSACTIONS = "Ready to start tracking? Your first transaction is just a tap away"
    const val EMPTY_BUDGETS = "Set a budget to get a clearer picture of your spending"
    const val EMPTY_GOALS = "What are you saving for? Add a goal and watch your progress grow"

    // --- Generic encouragement ---

    const val CHECKIN_GREETING_MORNING = "Good morning! Ready to review yesterday's spending?"
    const val CHECKIN_GREETING_AFTERNOON = "Taking a moment to check in on your finances — smart move"
    const val CHECKIN_GREETING_EVENING = "Winding down? Here's how your day went"
    const val CHECKIN_NO_SPENDS = "No transactions to review today — enjoy the peace of mind"

    // --- Never use these patterns ---
    // ❌ "You FAILED to stay under budget"
    // ❌ "You broke your streak! What happened??"  
    // ❌ Countdown timers creating fake urgency
    // ❌ "Only 2 hours left to complete this challenge!"
    // ❌ "Your spending is OUT OF CONTROL"
    // ❌ Comparing user to others ("You spend more than 80% of users")
}
