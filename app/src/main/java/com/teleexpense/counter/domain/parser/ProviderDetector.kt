package com.teleexpense.counter.domain.parser

import com.teleexpense.counter.domain.model.Provider

/**
 * Modular provider detection from sender address and message body.
 */
object ProviderDetector {

    private val ETHIO_KEYWORDS = listOf(
        "ethio telecom", "ethiotelecom", "ethio-telecom", "et telecom"
    )
    private val TELEBIRR_KEYWORDS = listOf(
        "telebirr", "tele birr", "tele-birr"
    )
    private val EBIRR_KEYWORDS = listOf(
        "ebirr", "e-birr", "-ebirr-", "kaafi"
    )
    private val BANK_KEYWORDS = listOf(
        "cbe", "commercial bank", "dashen", "awash", "abyssinia",
        "bank of abyssinia", "coop bank", "hibret", "wegagen"
    )

    fun detect(sender: String, body: String): Provider {
        val s = sender.lowercase()
        val b = body.lowercase()

        // Sender-based (strong)
        when {
            s.contains("telebirr") || s.contains("tele-birr") -> return Provider.TELEBIRR
            s.contains("ebirr") || s.contains("e-birr") || s.contains("kaafi") -> return Provider.EBIRR
            s.contains("ethio") || s.contains("telecom") -> return Provider.ETHIO_TELECOM
            BANK_KEYWORDS.any { s.contains(it) } -> return Provider.BANK
        }

        // Body-based
        when {
            TELEBIRR_KEYWORDS.any { b.contains(it) } -> return Provider.TELEBIRR
            EBIRR_KEYWORDS.any { b.contains(it) } -> return Provider.EBIRR
            ETHIO_KEYWORDS.any { b.contains(it) } -> return Provider.ETHIO_TELECOM
            BANK_KEYWORDS.any { b.contains(it) } && (b.contains("transferred") || b.contains("payment")) ->
                return Provider.BANK
        }

        // Generic telecom patterns
        if (b.contains("airtime") || b.contains("data package") || b.contains("prepaid account") ||
            b.contains("package") && (b.contains("mb") || b.contains("gb") || b.contains("sms"))
        ) {
            return Provider.ETHIO_TELECOM
        }

        return Provider.UNKNOWN
    }
}
