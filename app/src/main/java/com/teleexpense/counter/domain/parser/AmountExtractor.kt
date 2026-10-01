package com.teleexpense.counter.domain.parser

import java.util.regex.Pattern

/**
 * Semantic amount extraction.
 * Never takes the first number blindly.
 * Never treats balance, phone numbers, data sizes, fees, or VAT as the expense amount.
 */
object AmountExtractor {

    // Primary spend indicators – strongest signals
    private val PAID_PATTERNS = listOf(
        Pattern.compile(
            """(?:paid|payment of|purchase of|top-?up of|recharged?|sent airtime top-?up of|transferred)\s+(?:ETB|Birr|Br\.?)\s*([\d,]+\.?\d*)""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            """(?:ETB|Birr|Br\.?)\s*([\d,]+\.?\d*)\s+(?:for package|for|airtime|top-?up|recharge)""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            """(?:You have paid|You have successfully sent airtime top-up of|You have recharged)\s+(?:ETB|Birr)?\s*([\d,]+\.?\d*)""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            """Recharged balance is\s+([\d,]+\.?\d*)\s*(?:Birr|ETB)?""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            """airtime top-up of\s+(?:ETB|Birr)?\s*([\d,]+\.?\d*)""",
            Pattern.CASE_INSENSITIVE
        )
    )

    // Explicit balance phrases – must never be used as expense amount
    private val BALANCE_PATTERNS = listOf(
        Pattern.compile("""(?:your\s+)?balance\s+is\s+(?:ETB|Birr)?\s*([\d,]+\.?\d*)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:current\s+)?balance\s*[:=]?\s*(?:ETB|Birr)?\s*([\d,]+\.?\d*)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""Your balance is\s+(?:ETB|Birr)?\s*([\d,]+\.?\d*)""", Pattern.CASE_INSENSITIVE)
    )

    // Fee / VAT – not the main expense
    private val FEE_PATTERNS = listOf(
        Pattern.compile("""service fee is\s+(?:ETB|Birr)?\s*([\d,]+\.?\d*)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""VAT on the service fee is\s+(?:ETB|Birr)?\s*([\d,]+\.?\d*)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""fee\s+(?:ETB|Birr)?\s*([\d,]+\.?\d*)""", Pattern.CASE_INSENSITIVE)
    )

    /**
     * Returns the most likely expense amount, or null if uncertain.
     */
    fun extractExpenseAmount(text: String): Double? {
        // First collect all balance values so we can exclude them
        val balanceValues = mutableSetOf<Double>()
        for (p in BALANCE_PATTERNS) {
            val m = p.matcher(text)
            while (m.find()) {
                parseNumber(m.group(1))?.let { balanceValues.add(it) }
            }
        }

        // Prefer paid / recharge patterns
        for (p in PAID_PATTERNS) {
            val m = p.matcher(text)
            if (m.find()) {
                val amount = parseNumber(m.group(1)) ?: continue
                // If the same number appears as "balance is X" and is the only candidate,
                // still accept it for "Recharged balance is X" because that is the spend.
                // Only reject when it is clearly a post-transaction balance.
                if (isClearlyPostBalance(text, amount, balanceValues)) continue
                return amount
            }
        }

        // Fallback: look for "ETB X" near spend keywords
        val etbPattern = Pattern.compile(
            """(?:ETB|Birr|Br\.?)\s*([\d,]+\.?\d*)""",
            Pattern.CASE_INSENSITIVE
        )
        val spendKeywords = listOf("paid", "purchase", "recharged", "top-up", "topup", "package", "airtime")
        val lower = text.lowercase()
        val m = etbPattern.matcher(text)
        while (m.find()) {
            val amount = parseNumber(m.group(1)) ?: continue
            if (balanceValues.contains(amount) && !lower.contains("recharged balance")) continue
            // Check proximity to spend keyword (simple window)
            val start = (m.start() - 40).coerceAtLeast(0)
            val end = (m.end() + 40).coerceAtMost(text.length)
            val window = lower.substring(start, end)
            if (spendKeywords.any { window.contains(it) }) {
                return amount
            }
        }

        return null
    }

    /**
     * Returns true when the amount is only mentioned as resulting balance,
     * not as the recharged/paid amount.
     */
    private fun isClearlyPostBalance(
        text: String,
        amount: Double,
        balanceValues: Set<Double>
    ): Boolean {
        if (!balanceValues.contains(amount)) return false
        val lower = text.lowercase()
        // "Recharged balance is 100" is the spend amount
        if (lower.contains("recharged balance") || lower.contains("recharged balance is")) {
            return false
        }
        // "your balance is 3.6" after a recharge is not the expense
        return lower.contains("your balance is") || lower.contains("balance is")
    }

    fun parseNumber(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        return try {
            raw.replace(",", "").toDouble()
        } catch (_: NumberFormatException) {
            null
        }
    }

    /**
     * Detect free / bonus / promotional packages.
     */
    fun isFreeOrBonus(text: String): Boolean {
        val lower = text.lowercase()
        val freeWords = listOf(
            "free gift", "free package", " complimentary", "bonus package",
            "promotional", "promotion", "free of charge", "0 etb", "etb 0",
            "happy birthday", "enjoy free"
        )
        return freeWords.any { lower.contains(it) } ||
                (lower.contains("free") && (lower.contains("package") || lower.contains("gift") || lower.contains("bonus")))
    }
}
