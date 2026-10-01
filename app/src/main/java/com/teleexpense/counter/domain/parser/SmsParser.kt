package com.teleexpense.counter.domain.parser

import com.teleexpense.counter.domain.calendar.EthiopianCalendarConverter
import com.teleexpense.counter.domain.model.*
import java.util.UUID
import java.util.regex.Pattern

/**
 * Rule-based SMS parser.
 * Pipeline: Normalize → Provider → Classification → Amount → Date → IDs → Package → Result
 *
 * Never creates a final expense by itself — only produces SmsParseResult candidates
 * that later go through TransactionGrouper.
 */
object SmsParser {

    private val TRANSFER_ID_PATTERN = Pattern.compile(
        """Transfer ID:\s*(\d+)""",
        Pattern.CASE_INSENSITIVE
    )
    private val TRANSACTION_ID_PATTERN = Pattern.compile(
        """(?:transaction number is|transaction id[:\s]|Txn[:\s]|Trx[:\s])\s*([A-Z0-9]+)""",
        Pattern.CASE_INSENSITIVE
    )
    private val RECIPIENT_PATTERN = Pattern.compile(
        """(?:to|for)\s+(?:251)?(\d{9,12})""",
        Pattern.CASE_INSENSITIVE
    )
    private val PACKAGE_NAME_PATTERNS = listOf(
        Pattern.compile(
            """(?:for package|package)\s+([A-Za-z0-9\s.+\-]+?)(?:\s+purchase|\s+from|\s+to expire|\s+is added|\.|$)""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            """sent\s+([A-Za-z0-9\s.+\-]*?(?:Package|pack|MB|GB)[A-Za-z0-9\s.+\-]*)\s+to""",
            Pattern.CASE_INSENSITIVE
        )
    )

    /**
     * Parse a single SMS into a structured candidate.
     *
     * @param smsId stable local identifier (e.g. content://sms/_id or generated)
     * @param sender address / short code
     * @param body SMS body (will not be stored permanently in production)
     * @param receivedAtMillis device timestamp of SMS
     */
    fun parse(
        smsId: String,
        sender: String,
        body: String,
        receivedAtMillis: Long
    ): SmsParseResult {
        val normalized = body.trim().replace(Regex("\\s+"), " ")

        val provider = ProviderDetector.detect(sender, normalized)
        val classResult = CategoryClassifier.classify(sender, normalized)

        val amount = when (classResult.classification) {
            Classification.COUNT, Classification.REFERENCE ->
                AmountExtractor.extractExpenseAmount(normalized)
            Classification.IGNORE ->
                if (classResult.category == Category.FREE_PACKAGE) 0.0
                else AmountExtractor.extractExpenseAmount(normalized)
        }

        // Date: prefer explicit transaction date, else SMS received time
        val explicitDate = DateExtractor.extract(normalized)
        val effectiveMillis = explicitDate ?: receivedAtMillis
        val eth = EthiopianCalendarConverter.toEthiopian(effectiveMillis)

        val transferId = extractFirst(TRANSFER_ID_PATTERN, normalized)
        val transactionId = extractFirst(TRANSACTION_ID_PATTERN, normalized)
        val recipient = extractRecipient(normalized)
        val packageName = extractPackageName(normalized)

        val confidence = classResult.confidence *
                (if (amount != null || classResult.classification != Classification.COUNT) 1f else 0.7f)

        return SmsParseResult(
            sourceSmsId = smsId,
            rawSender = sender,
            smsReceivedAtMillis = receivedAtMillis,
            classification = classResult.classification,
            confidence = confidence.coerceIn(0f, 1f),
            provider = provider,
            category = classResult.category,
            amount = amount,
            currency = "ETB",
            direction = classResult.direction,
            status = TransactionStatus.SUCCESSFUL,
            packageName = packageName,
            recipient = recipient,
            transactionId = transactionId,
            transferId = transferId,
            gregorianDateTimeMillis = effectiveMillis,
            ethiopianYear = eth.year,
            ethiopianMonth = eth.month,
            ethiopianDay = eth.day
        )
    }

    private fun extractFirst(pattern: Pattern, text: String): String? {
        val m = pattern.matcher(text)
        return if (m.find()) m.group(1)?.trim() else null
    }

    private fun extractRecipient(text: String): String? {
        val m = RECIPIENT_PATTERN.matcher(text)
        return if (m.find()) {
            val num = m.group(1) ?: return null
            if (num.length == 9) "251$num" else num
        } else null
    }

    private fun extractPackageName(text: String): String? {
        for (p in PACKAGE_NAME_PATTERNS) {
            val m = p.matcher(text)
            if (m.find()) {
                val name = m.group(1)?.trim()?.take(120)
                if (!name.isNullOrBlank() && name.length > 3) return name
            }
        }
        return null
    }
}
