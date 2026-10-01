package com.teleexpense.counter.domain.grouping

import com.teleexpense.counter.domain.model.*
import java.util.UUID
import kotlin.math.abs

/**
 * Groups related SMS parse results into final TelecomTransaction expenses.
 *
 * Core rule: SMS ≠ transaction.
 * Multiple SMS (COUNT + REFERENCE + supporting) that belong to the same real-world
 * spend become ONE expense.
 *
 * Matching priority:
 * 1. Transfer ID / Transaction ID
 * 2. Amount + recipient + package + close timestamp
 * 3. Amount + provider + close timestamp + similar context
 */
object TransactionGrouper {

    private const val TIME_WINDOW_MS = 15 * 60 * 1000L // 15 minutes

    /**
     * Takes a list of parse results (typically from a scan) and returns
     * the final set of COUNT expenses after grouping/deduplication.
     * IGNORE results never become expenses.
     * REFERENCE results only attach to a matching COUNT or stay out.
     */
    fun group(results: List<SmsParseResult>): List<TelecomTransaction> {
        if (results.isEmpty()) return emptyList()

        val countCandidates = results.filter { it.classification == Classification.COUNT }
        val referenceCandidates = results.filter { it.classification == Classification.REFERENCE }
        // IGNORE are discarded for expense creation

        val usedReferenceIds = mutableSetOf<String>()
        val transactions = mutableListOf<TelecomTransaction>()

        // First pass: group COUNT messages that share IDs or strong signals
        val groups = mutableListOf<MutableList<SmsParseResult>>()

        for (candidate in countCandidates) {
            val existing = groups.find { group ->
                group.any { matches(it, candidate) }
            }
            if (existing != null) {
                existing.add(candidate)
            } else {
                groups.add(mutableListOf(candidate))
            }
        }

        // Attach REFERENCE messages to matching groups
        for (ref in referenceCandidates) {
            val matchingGroup = groups.find { group ->
                group.any { matches(it, ref) }
            }
            if (matchingGroup != null) {
                matchingGroup.add(ref)
                usedReferenceIds.add(ref.sourceSmsId)
            }
        }

        // Build final transactions from groups
        for (group in groups) {
            val primary = selectPrimary(group) ?: continue
            val amount = primary.amount ?: continue // COUNT without amount is invalid
            if (amount <= 0.0) continue

            val allIds = group.map { it.sourceSmsId }
            val ethYear = primary.ethiopianYear ?: continue
            val ethMonth = primary.ethiopianMonth ?: continue
            val ethDay = primary.ethiopianDay ?: continue
            val gMillis = primary.gregorianDateTimeMillis ?: primary.smsReceivedAtMillis

            transactions.add(
                TelecomTransaction(
                    id = UUID.randomUUID().toString(),
                    provider = primary.provider,
                    category = primary.category,
                    amount = amount,
                    currency = primary.currency,
                    direction = primary.direction,
                    status = primary.status,
                    gregorianDateTimeMillis = gMillis,
                    ethiopianYear = ethYear,
                    ethiopianMonth = ethMonth,
                    ethiopianDay = ethDay,
                    packageName = primary.packageName ?: group.mapNotNull { it.packageName }.firstOrNull(),
                    recipient = primary.recipient ?: group.mapNotNull { it.recipient }.firstOrNull(),
                    transactionId = primary.transactionId ?: group.mapNotNull { it.transactionId }.firstOrNull(),
                    transferId = primary.transferId ?: group.mapNotNull { it.transferId }.firstOrNull(),
                    sourceEventIds = allIds,
                    classification = Classification.COUNT,
                    confidence = group.map { it.confidence }.average().toFloat(),
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }

        return transactions
    }

    /**
     * Strong match signals.
     */
    private fun matches(a: SmsParseResult, b: SmsParseResult): Boolean {
        // Same transfer / transaction ID → same transaction
        if (!a.transferId.isNullOrBlank() && a.transferId == b.transferId) return true
        if (!a.transactionId.isNullOrBlank() && a.transactionId == b.transactionId) return true

        // Amount must match when both present
        val amountA = a.amount
        val amountB = b.amount
        if (amountA != null && amountB != null && amountA != amountB) return false

        // Time proximity
        val timeA = a.gregorianDateTimeMillis ?: a.smsReceivedAtMillis
        val timeB = b.gregorianDateTimeMillis ?: b.smsReceivedAtMillis
        if (abs(timeA - timeB) > TIME_WINDOW_MS) return false

        // Same recipient
        if (!a.recipient.isNullOrBlank() && a.recipient == b.recipient && amountA != null && amountA == amountB) {
            return true
        }

        // Same package name + amount
        if (!a.packageName.isNullOrBlank() && a.packageName.equals(b.packageName, ignoreCase = true) &&
            amountA != null && amountA == amountB
        ) {
            return true
        }

        // Same provider + amount + close time (weaker)
        if (a.provider == b.provider && amountA != null && amountA == amountB &&
            abs(timeA - timeB) <= 5 * 60 * 1000L
        ) {
            return true
        }

        return false
    }

    /**
     * Prefer the COUNT with highest confidence / most complete data as primary.
     */
    private fun selectPrimary(group: List<SmsParseResult>): SmsParseResult? {
        return group
            .filter { it.classification == Classification.COUNT }
            .maxByOrNull { it.confidence + (if (it.amount != null) 0.1f else 0f) + (if (it.transactionId != null) 0.1f else 0f) }
            ?: group.firstOrNull()
    }
}
