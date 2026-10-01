package com.teleexpense.counter.domain.model

/**
 * Core domain models for Tele Expense Counter.
 * All processing is local; no network models.
 */

enum class Classification {
    COUNT,      // Sufficient evidence of telecom spend → create/contribute to expense
    IGNORE,     // Clearly not a telecom expense
    REFERENCE   // Telecom-related but insufficient alone; may link to a COUNT
}

enum class Direction {
    OUTGOING,
    INCOMING,
    UNKNOWN
}

enum class TransactionStatus {
    SUCCESSFUL,
    FAILED,
    PENDING,
    UNKNOWN
}

enum class Category {
    AIRTIME,
    AIRTIME_RECHARGE,
    DATA_PACKAGE,
    SMS_PACKAGE,
    VOICE_PACKAGE,
    MIXED_PACKAGE,
    FREE_PACKAGE,
    PACKAGE_ACTIVATION,
    PERSON_TO_PERSON_TRANSFER,
    BANK_TRANSFER,
    OTHER
}

enum class Provider {
    ETHIO_TELECOM,
    TELEBIRR,
    EBIRR,
    BANK,
    UNKNOWN
}

/**
 * Intermediate parse result from a single SMS.
 * Never becomes a final expense by itself — goes through grouping.
 */
data class SmsParseResult(
    val sourceSmsId: String,
    val rawSender: String,
    val smsReceivedAtMillis: Long,
    val classification: Classification,
    val confidence: Float,                 // 0.0 – 1.0
    val provider: Provider,
    val category: Category,
    val amount: Double?,                   // null when unknown
    val currency: String = "ETB",
    val direction: Direction,
    val status: TransactionStatus,
    val packageName: String?,
    val recipient: String?,
    val transactionId: String?,
    val transferId: String?,
    val gregorianDateTimeMillis: Long?,    // preferred transaction time
    val ethiopianYear: Int?,
    val ethiopianMonth: Int?,
    val ethiopianDay: Int?,
    val notes: String? = null
)

/**
 * Final deduplicated telecom expense transaction.
 * One real-world spend event — may be backed by multiple SMS.
 */
data class TelecomTransaction(
    val id: String,
    val provider: Provider,
    val category: Category,
    val amount: Double,
    val currency: String = "ETB",
    val direction: Direction,
    val status: TransactionStatus,
    val gregorianDateTimeMillis: Long,
    val ethiopianYear: Int,
    val ethiopianMonth: Int,
    val ethiopianDay: Int,
    val packageName: String?,
    val recipient: String?,
    val transactionId: String?,
    val transferId: String?,
    val sourceEventIds: List<String>,      // SMS IDs that contributed
    val classification: Classification,
    val confidence: Float,
    val createdAtMillis: Long,
    val isManuallyEdited: Boolean = false,
    val isExcluded: Boolean = false        // user marked "not an expense"
)

/**
 * Lightweight day summary for calendar view.
 */
data class DaySpending(
    val ethiopianYear: Int,
    val ethiopianMonth: Int,
    val ethiopianDay: Int,
    val totalAmount: Double,
    val transactionCount: Int
)

/**
 * Category breakdown for analytics.
 */
data class CategoryBreakdown(
    val category: Category,
    val amount: Double,
    val percentage: Float,
    val count: Int
)

/**
 * Period summary shown on dashboard / analytics.
 */
data class PeriodSummary(
    val label: String,
    val ethiopianLabel: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val averagePerDay: Double,
    val averagePerTransaction: Double,
    val breakdown: List<CategoryBreakdown>
)
