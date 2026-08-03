package com.nudge.engine

import com.nudge.model.*

/**
 * Bundled, versioned library of regex templates per bank.
 * Ship 50+ common bank formats out of the box.
 * Supports remote-updatable rule packs (rules only, never raw data uploaded).
 *
 * Version: 1.0.0
 */
object BundledRulePack {
    const val VERSION = "1.0.0"

    fun getRules(): List<ParserRule> {
        val rules = mutableListOf<ParserRule>()

        // ==========================================
        // INDIA — Bank SMS formats
        // ==========================================

        // HDFC Bank
        rules.add(
            ParserRule(
                id = "in-hdfc-debit-1",
                bankName = "HDFC Bank",
                regexPattern = """Rs\.?(\d[\d,]*)\.?\d*\s*(?:debited|spent|paid|withdrawn)""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 0),
                isVerified = true
            )
        )
        rules.add(
            ParserRule(
                id = "in-hdfc-upi-1",
                bankName = "HDFC Bank",
                regexPattern = """Rs\.?(\d[\d,]*)\.?\d*\s*debited from a/c [*X\d]+ on (\d{2}-\d{2}-\d{2,4})""",
                fieldMapping = FieldMapping(amountGroup = 1, dateGroup = 2),
                isVerified = true
            )
        )

        // ICICI Bank
        rules.add(
            ParserRule(
                id = "in-icici-debit-1",
                bankName = "ICICI Bank",
                regexPattern = """Rs\s*(\d[\d,]*)\.?\d*\s*debited from(?: your)? a/c""",
                fieldMapping = FieldMapping(amountGroup = 1),
                isVerified = true
            )
        )
        rules.add(
            ParserRule(
                id = "in-icici-credit-1",
                bankName = "ICICI Bank",
                regexPattern = """Rs\s*(\d[\d,]*)\.?\d*\s*credited to(?: your)? a/c""",
                fieldMapping = FieldMapping(amountGroup = 1, transactionTypeHint = TransactionType.CREDIT),
                isVerified = true
            )
        )

        // SBI
        rules.add(
            ParserRule(
                id = "in-sbi-debit-1",
                bankName = "SBI",
                regexPattern = """debited\s*(?:by|with)?\s*(?:Rs\.?|INR)?\s*([\d,]+\.?\d*)""",
                fieldMapping = FieldMapping(amountGroup = 1),
                isVerified = true
            )
        )
        rules.add(
            ParserRule(
                id = "in-sbi-upi-1",
                bankName = "SBI",
                regexPattern = """UPI/(\w+)/\d+\s*Rs\.?(\d[\d,]*)\.?\d*\s*(?:debited|paid|sent)""",
                fieldMapping = FieldMapping(amountGroup = 2, merchantGroup = 1),
                isVerified = true
            )
        )

        // Axis Bank
        rules.add(
            ParserRule(
                id = "in-axis-debit-1",
                bankName = "Axis Bank",
                regexPattern = """INR\s*([\d,]+\.?\d*)\s*debited from a/c""",
                fieldMapping = FieldMapping(amountGroup = 1),
                isVerified = true
            )
        )

        // Kotak Bank
        rules.add(
            ParserRule(
                id = "in-kotak-debit-1",
                bankName = "Kotak Mahindra",
                regexPattern = """Rs\.\s*([\d,]+\.?\d*)\s*(?:debited|spent|transferred)""",
                fieldMapping = FieldMapping(amountGroup = 1),
                isVerified = true
            )
        )

        // Punjab National Bank
        rules.add(
            ParserRule(
                id = "in-pnb-debit-1",
                bankName = "PNB",
                regexPattern = """(?:Rs\.|INR)\s*([\d,]+\.?\d*)\s*(?:debited|paid|withdrawn)""",
                fieldMapping = FieldMapping(amountGroup = 1),
                isVerified = true
            )
        )

        // ==========================================
        // INDIA — UPI / Wallet formats
        // ==========================================

        // PhonePe
        rules.add(
            ParserRule(
                id = "in-phonepe-1",
                bankName = "PhonePe",
                regexPattern = """(?:₹|Rs\.?)\s*([\d,]+\.?\d*)\s*(?:paid|sent|debited)\s*(?:to|at)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // Google Pay
        rules.add(
            ParserRule(
                id = "in-gpay-1",
                bankName = "Google Pay",
                regexPattern = """(?:₹|Rs\.?)\s*([\d,]+\.?\d*)\s*(?:sent|paid|debited)\s*(?:to|at)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )
        rules.add(
            ParserRule(
                id = "in-gpay-receive-1",
                bankName = "Google Pay",
                regexPattern = """(?:₹|Rs\.?)\s*([\d,]+\.?\d*)\s*(?:received|credited)\s*(?:from|by)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2, transactionTypeHint = TransactionType.CREDIT),
                isVerified = true
            )
        )

        // Paytm
        rules.add(
            ParserRule(
                id = "in-paytm-1",
                bankName = "Paytm",
                regexPattern = """Paid\s*(?:Rs\.?|₹)\s*([\d,]+\.?\d*)\s*(?:to|for)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )
        rules.add(
            ParserRule(
                id = "in-paytm-wallet-1",
                bankName = "Paytm",
                regexPattern = """(?:Rs\.?|₹)\s*([\d,]+\.?\d*)\s*(?:added to|credited to) your (?:Paytm|wallet)""",
                fieldMapping = FieldMapping(amountGroup = 1, transactionTypeHint = TransactionType.CREDIT),
                isVerified = true
            )
        )

        // Generic UPI
        rules.add(
            ParserRule(
                id = "in-generic-upi-dr",
                bankName = "Generic UPI",
                regexPattern = """(?:₹|Rs\.?|INR)\s*([\d,]+\.?\d*)\s*(?:debited|spent|paid|sent|transferred)\s*(?:from|via)?\s*(?:a/c\s*[*\dxX]+)?(?:\s*(?:to|at))?\s*(\S+(?:\s+\S+){0,2})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // ==========================================
        // INDIA — Credit Card formats
        // ==========================================

        rules.add(
            ParserRule(
                id = "in-cc-generic-1",
                bankName = "Generic Credit Card",
                regexPattern = """(?:Rs\.?|INR)\s*([\d,]+\.?\d*)\s*(?:spent|charged|paid)\s*(?:on|at|to)\s*(?:your)?\s*(?:.*?card)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )
        rules.add(
            ParserRule(
                id = "in-cc-generic-2",
                bankName = "Generic Credit Card",
                regexPattern = """(?:purchase|payment)\s*(?:of|at)?\s*(?:Rs\.?|INR|₹)?\s*([\d,]+\.?\d*)\s*(?:at|on)\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // ==========================================
        // USA — Bank SMS formats
        // ==========================================

        // Chase
        rules.add(
            ParserRule(
                id = "us-chase-debit-1",
                bankName = "Chase",
                regexPattern = """\$\s*([\d,]+\.\d{2})\s*(?:purchase|payment|withdrawal)\s*(?:at|from)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // Bank of America
        rules.add(
            ParserRule(
                id = "us-boa-debit-1",
                bankName = "Bank of America",
                regexPattern = """Debit\s*\$\s*([\d,]+\.\d{2})\s*(?:from|at)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // Wells Fargo
        rules.add(
            ParserRule(
                id = "us-wf-debit-1",
                bankName = "Wells Fargo",
                regexPattern = """(?:purchase|debit|payment)\s*\$\s*([\d,]+\.\d{2})\s*(?:at|from|to)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // Citi
        rules.add(
            ParserRule(
                id = "us-citi-debit-1",
                bankName = "Citibank",
                regexPattern = """\$([\d,]+\.\d{2})\s*(?:spent|charged|paid)\s*(?:at|to|on)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // Amex
        rules.add(
            ParserRule(
                id = "us-amex-1",
                bankName = "American Express",
                regexPattern = """\$([\d,]+\.\d{2})\s*(?:charge|purchase|payment)\s*(?:at|from)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // Capital One
        rules.add(
            ParserRule(
                id = "us-capitalone-1",
                bankName = "Capital One",
                regexPattern = """\$([\d,]+\.\d{2})\s*(?:purchase|payment)\s*(?:at|from)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // Discover
        rules.add(
            ParserRule(
                id = "us-discover-1",
                bankName = "Discover",
                regexPattern = """(?:purchase|payment)\s*(?:of)?\s*\$([\d,]+\.\d{2})\s*(?:at|from)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // US Bank
        rules.add(
            ParserRule(
                id = "us-usbank-1",
                bankName = "US Bank",
                regexPattern = """Debit\s*\$([\d,]+\.\d{2})\s*(?:at|to)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // Generic US
        rules.add(
            ParserRule(
                id = "us-generic-debit",
                bankName = "Generic US",
                regexPattern = """\$([\d,]+\.\d{2})\s*(?:has been|was)?\s*(?:debited|withdrawn|charged|spent)""",
                fieldMapping = FieldMapping(amountGroup = 1),
                isVerified = true
            )
        )
        rules.add(
            ParserRule(
                id = "us-generic-credit",
                bankName = "Generic US",
                regexPattern = """\$([\d,]+\.\d{2})\s*(?:has been|was)?\s*(?:credited|deposited|received|added)""",
                fieldMapping = FieldMapping(amountGroup = 1, transactionTypeHint = TransactionType.CREDIT),
                isVerified = true
            )
        )

        // ==========================================
        // UK — Bank SMS formats
        // ==========================================

        // Barclays
        rules.add(
            ParserRule(
                id = "uk-barclays-1",
                bankName = "Barclays",
                regexPattern = """£?([\d,]+\.\d{2})\s*(?:spent|paid|debited|withdrawn)\s*(?:at|to|from)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // HSBC UK
        rules.add(
            ParserRule(
                id = "uk-hsbc-1",
                bankName = "HSBC UK",
                regexPattern = """(?:purchase|payment|debit)\s*(?:of)?\s*GBP\s*([\d,]+\.\d{2})\s*(?:at|to)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // NatWest
        rules.add(
            ParserRule(
                id = "uk-natwest-1",
                bankName = "NatWest",
                regexPattern = """(?:a payment of|spent|debited)\s*£?([\d,]+\.\d{2})\s*(?:at|to|from)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // Lloyds
        rules.add(
            ParserRule(
                id = "uk-lloyds-1",
                bankName = "Lloyds",
                regexPattern = """£([\d,]+\.\d{2})\s*(?:has been|was)?\s*(?:debited|paid|spent|withdrawn)""",
                fieldMapping = FieldMapping(amountGroup = 1),
                isVerified = true
            )
        )

        // Santander UK
        rules.add(
            ParserRule(
                id = "uk-santander-1",
                bankName = "Santander UK",
                regexPattern = """(?:purchase|payment)\s*(?:of)?\s*£([\d,]+\.\d{2})\s*(?:at|to)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // Monzo
        rules.add(
            ParserRule(
                id = "uk-monzo-1",
                bankName = "Monzo",
                regexPattern = """£([\d,]+\.\d{2})\s*(?:spent|paid)\s*(?:at|to)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // Revolut
        rules.add(
            ParserRule(
                id = "uk-revolut-1",
                bankName = "Revolut",
                regexPattern = """(?:spent|paid|sent)\s*£([\d,]+\.\d{2})\s*(?:at|to)?\s*(\S+(?:\s+\S+){0,3})""",
                fieldMapping = FieldMapping(amountGroup = 1, merchantGroup = 2),
                isVerified = true
            )
        )

        // Generic UK
        rules.add(
            ParserRule(
                id = "uk-generic-debit",
                bankName = "Generic UK",
                regexPattern = """£?([\d,]+\.\d{2})\s*(?:debited|spent|paid|withdrawn|left your account)""",
                fieldMapping = FieldMapping(amountGroup = 1),
                isVerified = true
            )
        )
        rules.add(
            ParserRule(
                id = "uk-generic-credit",
                bankName = "Generic UK",
                regexPattern = """£?([\d,]+\.\d{2})\s*(?:credited|received|paid in|deposited|added to)""",
                fieldMapping = FieldMapping(amountGroup = 1, transactionTypeHint = TransactionType.CREDIT),
                isVerified = true
            )
        )

        // ==========================================
        // FALLBACK — Heuristic patterns
        // ==========================================

        rules.add(
            ParserRule(
                id = "fallback-amount-any",
                bankName = "Generic Fallback",
                regexPattern = """(?:₹|Rs\.?|INR|\$|£|EUR|€)\s*([\d,]+\.?\d{0,2})""",
                fieldMapping = FieldMapping(amountGroup = 1),
                isVerified = true
            )
        )

        return rules
    }

    /**
     * Get a flat list of known bank sender IDs for the whitelist filter
     */
    fun getSenderWhitelist(): List<SenderWhitelist> {
        return listOf(
            // India — Banks
            SenderWhitelist("wl-in-hdfc", "HDFCBK", "HDFC Bank", "IN"),
            SenderWhitelist("wl-in-icici", "ICICIB", "ICICI Bank", "IN"),
            SenderWhitelist("wl-in-icici-cards", "ICICIT", "ICICI Bank", "IN"),
            SenderWhitelist("wl-in-sbi", "SBIINB", "SBI", "IN"),
            SenderWhitelist("wl-in-axis", "AXISBK", "Axis Bank", "IN"),
            SenderWhitelist("wl-in-kotak", "KOTAKB", "Kotak Mahindra", "IN"),
            SenderWhitelist("wl-in-pnb", "PNBANK", "PNB", "IN"),
            SenderWhitelist("wl-in-bob", "BOBINB", "Bank of Baroda", "IN"),
            SenderWhitelist("wl-in-union", "UBIINB", "Union Bank", "IN"),
            SenderWhitelist("wl-in-canara", "CNRBNK", "Canara Bank", "IN"),
            SenderWhitelist("wl-in-yes", "YESBNK", "Yes Bank", "IN"),
            SenderWhitelist("wl-in-idfc", "IDFCBK", "IDFC First Bank", "IN"),
            SenderWhitelist("wl-in-indus", "INDUS", "IndusInd Bank", "IN"),
            SenderWhitelist("wl-in-federal", "FEDBNK", "Federal Bank", "IN"),
            SenderWhitelist("wl-in-bank-india", "BANKBK", "Bank of India", "IN"),
            SenderWhitelist("wl-in-citi", "CITIIN", "Citibank", "IN"),
            SenderWhitelist("wl-in-amex", "AMEXIN", "American Express", "IN"),
            SenderWhitelist("wl-in-hsbc", "HSBCIN", "HSBC", "IN"),
            SenderWhitelist("wl-in-sc", "SCBLIN", "Standard Chartered", "IN"),
            SenderWhitelist("wl-in-dbs", "DBSSG", "DBS Bank", "IN"),

            // India — UPI/Wallet vendors (notification package names)
            SenderWhitelist("wl-upi-gpay", "com.google.android.apps.nbu.paisa.user", "Google Pay", "IN"),
            SenderWhitelist("wl-upi-phonepe", "com.phonepe.app", "PhonePe", "IN"),
            SenderWhitelist("wl-upi-paytm", "net.one97.paytm", "Paytm", "IN"),
            SenderWhitelist("wl-upi-bhim", "in.org.npci.upiapp", "BHIM", "IN"),
            SenderWhitelist("wl-upi-amazon", "com.amazon.mShop.android.shopping", "Amazon Pay", "IN"),

            // India — Bank apps
            SenderWhitelist("wl-app-hdfc", "com.hdfc.retail.netbanking", "HDFC Mobile", "IN"),
            SenderWhitelist("wl-app-icici", "com.icici.bank.icicico", "iMobile", "IN"),
            SenderWhitelist("wl-app-sbi", "com.sbi.lotus", "YONO SBI", "IN"),
            SenderWhitelist("wl-app-kotak", "com.kotak.neo", "Kotak Mobile", "IN"),
            SenderWhitelist("wl-app-axis", "com.axis.mobile", "Axis Mobile", "IN"),
            SenderWhitelist("wl-app-yes", "com.yesbank.nomad", "YES Bank", "IN"),
            SenderWhitelist("wl-app-idfc", "com.idfcfirstbank.optimus", "IDFC First", "IN"),

            // US — Common bank SMS shortcodes
            SenderWhitelist("wl-us-chase", "CHASE", "Chase", "US"),
            SenderWhitelist("wl-us-boa", "BANKAM", "Bank of America", "US"),
            SenderWhitelist("wl-us-wf", "WELLS", "Wells Fargo", "US"),
            SenderWhitelist("wl-us-citi", "CITI", "Citibank", "US"),
            SenderWhitelist("wl-us-amex", "AMEX", "American Express", "US"),
            SenderWhitelist("wl-us-capone", "CAPONE", "Capital One", "US"),
            SenderWhitelist("wl-us-disc", "DISCOVER", "Discover", "US"),
            SenderWhitelist("wl-us-usbank", "USBANK", "US Bank", "US"),
            SenderWhitelist("wl-us-pnc", "PNCBANK", "PNC Bank", "US"),
            SenderWhitelist("wl-us-td", "TDBANK", "TD Bank", "US"),

            // UK — Common bank SMS shortcodes
            SenderWhitelist("wl-uk-barclays", "BARCLAYS", "Barclays", "UK"),
            SenderWhitelist("wl-uk-hsbc", "HSBCUK", "HSBC UK", "UK"),
            SenderWhitelist("wl-uk-natwest", "NATWEST", "NatWest", "UK"),
            SenderWhitelist("wl-uk-lloyds", "LLOYDS", "Lloyds", "UK"),
            SenderWhitelist("wl-uk-santander", "SANTAN", "Santander UK", "UK"),
            SenderWhitelist("wl-uk-monzo", "MONZO", "Monzo", "UK"),
            SenderWhitelist("wl-uk-revolut", "REVOLUT", "Revolut", "UK"),
            SenderWhitelist("wl-uk-nationwide", "NATION", "Nationwide", "UK"),
            SenderWhitelist("wl-uk-halifax", "HALIFAX", "Halifax", "UK"),
        )
    }
}
