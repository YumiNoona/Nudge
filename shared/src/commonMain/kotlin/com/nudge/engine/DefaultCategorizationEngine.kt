package com.nudge.engine

import com.nudge.model.*

/**
 * Default implementation of CategorizationEngine.
 *
 * 1. Starts with built-in merchant → category rules (common knowledge)
 * 2. Learns from user corrections over time (per-user, on-device only)
 * 3. Falls back to heuristic matching if no rule matches
 *
 * Every manual correction feeds back into the local rule table instantly.
 * "Teach it once, it remembers forever."
 */
class DefaultCategorizationEngine : CategorizationEngine {

    // Built-in merchant → category rules (shipped with the app)
    private val builtInRules = mapOf(
        // Food & Dining
        "Swiggy" to "food",
        "Zomato" to "food",
        "Uber Eats" to "food",
        "DoorDash" to "food",
        "McDonald's" to "food",
        "Starbucks" to "food",
        "Chipotle" to "food",
        "Domino's" to "food",
        "Pizza Hut" to "food",
        "Subway" to "food",
        "Burger King" to "food",
        "KFC" to "food",
        "Dunzo" to "food",
        "EatSure" to "food",
        "Oven Story" to "food",
        "Faasos" to "food",
        "Behrouz" to "food",

        // Transport
        "Uber" to "transport",
        "Ola" to "transport",
        "Lyft" to "transport",
        "Rapido" to "transport",
        "IRCTC" to "transport",
        "MakeMyTrip" to "transport",
        "Goibibo" to "transport",
        "RedBus" to "transport",
        "Transport for London" to "transport",
        "Oyster Card" to "transport",
        "Metro" to "transport",

        // Groceries
        "BigBasket" to "groceries",
        "Blinkit" to "groceries",
        "Zepto" to "groceries",
        "DMart" to "groceries",
        "JioMart" to "groceries",
        "Instamart" to "groceries",
        "Tesco" to "groceries",
        "Sainsbury's" to "groceries",
        "Asda" to "groceries",
        "Walmart" to "groceries",
        "Costco" to "groceries",

        // Shopping
        "Amazon" to "shopping",
        "Flipkart" to "shopping",
        "Myntra" to "shopping",
        "Meesho" to "shopping",
        "Nykaa" to "shopping",
        "Tata Cliq" to "shopping",
        "Ajio" to "shopping",
        "Target" to "shopping",

        // Entertainment
        "Netflix" to "entertainment",
        "Hotstar" to "entertainment",
        "Amazon Prime" to "entertainment",
        "YouTube" to "entertainment",
        "Spotify" to "entertainment",
        "BookMyShow" to "entertainment",
        "PVR" to "entertainment",
        "INOX" to "entertainment",
        "Steam" to "entertainment",
        "PlayStation" to "entertainment",
        "Xbox" to "entertainment",

        // Utilities
        "Electricity" to "utilities",
        "Water" to "utilities",
        "Gas" to "utilities",
        "Airtel" to "utilities",
        "Jio" to "utilities",
        "Vodafone" to "utilities",
        "Vodafone Idea" to "utilities",
        "BSNL" to "utilities",
        "Vi" to "utilities",
        "Tata Sky" to "utilities",
        "Dish TV" to "utilities",
        "DTH" to "utilities",
        "BSES" to "utilities",
        "Tata Power" to "utilities",
        "Adani Electricity" to "utilities",
        "Mahanagar Gas" to "utilities",

        // Rent
        "Rent" to "rent",
        "Housing" to "rent",
        "Nestaway" to "rent",
        "NoBroker" to "rent",
        "MagicBricks" to "rent",

        // Healthcare
        "Apollo" to "healthcare",
        "Practo" to "healthcare",
        "PharmEasy" to "healthcare",
        "1mg" to "healthcare",
        "Netmeds" to "healthcare",
        "MediBuddy" to "healthcare",
        "Walgreens" to "healthcare",
        "CVS" to "healthcare",
        "Boots" to "healthcare",
        "Hospital" to "healthcare",
        "Pharmacy" to "healthcare",
        "Clinic" to "healthcare",

        // Subscriptions
        "Google" to "subscriptions",
        "Google Play" to "subscriptions",
        "Microsoft" to "subscriptions",
        "Apple" to "subscriptions",
        "Adobe" to "subscriptions",
        "Dropbox" to "subscriptions",
        "Github" to "subscriptions",
        "Notion" to "subscriptions",
        "Canva" to "subscriptions",
        "Figma" to "subscriptions",
        "Medium" to "subscriptions",
        "LinkedIn" to "subscriptions",

        // Insurance
        "LIC" to "investments",
        "HDFC Life" to "investments",
        "ICICI Prudential" to "investments",
        "Policy" to "investments",
        "Insurance" to "investments",

        // Cash
        "ATM Withdrawal" to "other",
        "Cash Withdrawal" to "other",
        "Cash" to "other",
        "UPI Transfer" to "other",
        "NEFT Transfer" to "other",
        "IMPS Transfer" to "other",
        "RTGS Transfer" to "other",
        "Transfer" to "other",
    )

    // User-learned rules (stored on-device, populated from DB on startup)
    private val userRules = mutableMapOf<String, String>()
    private val userConfidence = mutableMapOf<String, Int>() // count of times confirmed

    /**
     * Auto-categorize a transaction by merchant name.
     * Priority: user-learned > built-in > heuristic
     */
    override fun autoCategorize(merchantNormalized: String, amount: Long): CategorizationResult {
        val lowered = merchantNormalized.lowercase().trim()

        // 1. Check user-learned rules first (highest priority)
        for ((pattern, categoryId) in userRules) {
            if (lowered.contains(pattern.lowercase())) {
                return CategorizationResult(
                    categoryId = categoryId,
                    confidence = 0.95f,
                    source = CategorizationSource.USER_LEARNED
                )
            }
        }

        // 2. Check built-in rules
        for ((merchant, categoryType) in builtInRules) {
            if (lowered.contains(merchant.lowercase())) {
                return CategorizationResult(
                    categoryId = categoryType, // categoryType is used as a hint — platform maps to actual ID
                    confidence = 0.80f,
                    source = CategorizationSource.BUILT_IN_RULE
                )
            }
        }

        // 3. Heuristic fallback — try to match keywords
        val heuristicResult = heuristicMatch(lowered)
        if (heuristicResult != null) {
            return CategorizationResult(
                categoryId = heuristicResult,
                confidence = 0.50f,
                source = CategorizationSource.HEURISTIC_FALLBACK
            )
        }

        // 4. No match — needs manual categorization
        return CategorizationResult(
            categoryId = null,
            confidence = 0.0f,
            source = CategorizationSource.HEURISTIC_FALLBACK
        )
    }

    /**
     * Learn from a user correction. Permanently stores the mapping.
     */
    override fun learn(merchantNormalized: String, categoryId: String) {
        val key = merchantNormalized.lowercase().trim()
        userRules[key] = categoryId
        userConfidence[key] = (userConfidence[key] ?: 0) + 1

        // If user confirmed same mapping 5+ times, treat it as 100% confidence
        if ((userConfidence[key] ?: 0) >= 5) {
            userRules[key] = categoryId
        }
    }

    override fun getConfidence(merchantNormalized: String, categoryId: String): Float {
        val key = merchantNormalized.lowercase().trim()
        val count = userConfidence[key] ?: 0
        val isMatch = userRules[key] == categoryId
        return if (isMatch) (count / 5f).coerceIn(0.5f, 1.0f) else 0.0f
    }

    override fun getLearnedMappings(): Map<String, String> {
        return userRules.toMap()
    }

    override fun importMappings(mappings: Map<String, String>) {
        mappings.forEach { (key, value) ->
            userRules[key.lowercase().trim()] = value
        }
    }

    /**
     * Heuristic keyword-based matching for uncategorized merchants
     */
    private fun heuristicMatch(merchant: String): String? {
        val keywords = listOf(
            "food" to listOf("food", "restaurant", "cafe", "diner", "kitchen", "bistro", "pizza",
                "burger", "sushi", "taco", "grill", "curry", "biryani", "dhaba", "bar"),
            "transport" to listOf("uber", "ola", "lyft", "taxi", "cab", "auto", "metro", "bus",
                "train", "flight", "airlines", "petrol", "diesel", "fuel", "parking", "toll"),
            "groceries" to listOf("grocery", "supermarket", "mart", "store", "vegetable", "fruit",
                "kirana", "organic", "daily needs", "provision"),
            "shopping" to listOf("mall", "store", "shop", "retail", "fashion", "clothing",
                "apparel", "electronics", "gadget"),
            "entertainment" to listOf("movie", "cinema", "theatre", "game", "gaming", "arcade",
                "concert", "event", "ticket", "streaming"),
            "utilities" to listOf("electric", "water", "gas", "bill", "recharge", "mobile",
                "broadband", "wifi", "internet", "dth", "cable"),
            "healthcare" to listOf("doctor", "hospital", "clinic", "pharmacy", "medical", "health",
                "medicine", "dental", "lab", "diagnostic"),
            "education" to listOf("school", "college", "university", "course", "tuition",
                "learning", "class", "training", "books", "library"),
            "subscriptions" to listOf("subscription", "membership", "saas", "cloud", "hosting"),
            "investments" to listOf("mutual fund", "stock", "share", "sip", "investment", "trading",
                "demat", "broker", "nps", "ppf"),
            "insurance" to listOf("insurance", "premium", "cover", "policy", "life", "health",
                "term", "vehicle"),
            "rent" to listOf("rent", "lease", "pg", "hostel", "flat"),
            "travel" to listOf("hotel", "resort", "stay", "booking", "trip", "vacation", "tour"),
            "personal_care" to listOf("salon", "spa", "barber", "parlour", "beauty", "gym",
                "fitness", "yoga"),
        )

        val lowered = merchant.lowercase()
        for ((categoryType, terms) in keywords) {
            if (terms.any { lowered.contains(it) }) {
                return categoryType
            }
        }

        return null
    }
}
