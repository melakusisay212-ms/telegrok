package com.teleexpense.counter.domain.parser

import com.teleexpense.counter.domain.model.Category
import com.teleexpense.counter.domain.model.Classification
import com.teleexpense.counter.domain.model.Direction

/**
 * Rule-based classification of SMS into COUNT / IGNORE / REFERENCE
 * and category assignment.
 */
object CategoryClassifier {

    data class Result(
        val classification: Classification,
        val category: Category,
        val direction: Direction,
        val confidence: Float
    )

    fun classify(sender: String, body: String): Result {
        val lower = body.lowercase()

        // ---------- FREE / BONUS / PROMO → IGNORE ----------
        if (AmountExtractor.isFreeOrBonus(body)) {
            return Result(Classification.IGNORE, Category.FREE_PACKAGE, Direction.INCOMING, 0.95f)
        }

        // ---------- PERSON-TO-PERSON TRANSFER → IGNORE ----------
        if (isPersonToPerson(lower)) {
            return Result(Classification.IGNORE, Category.PERSON_TO_PERSON_TRANSFER, Direction.OUTGOING, 0.95f)
        }

        // ---------- INCOMING AIRTIME → IGNORE ----------
        if (isIncomingAirtime(lower)) {
            return Result(Classification.IGNORE, Category.AIRTIME, Direction.INCOMING, 0.95f)
        }

        // ---------- OUTGOING AIRTIME SENT → COUNT ----------
        if (isOutgoingAirtimeSent(lower)) {
            return Result(Classification.COUNT, Category.AIRTIME, Direction.OUTGOING, 0.92f)
        }

        // ---------- PREPAID RECHARGE → COUNT ----------
        if (isPrepaidRecharge(lower)) {
            return Result(Classification.COUNT, Category.AIRTIME_RECHARGE, Direction.OUTGOING, 0.93f)
        }

        // ---------- PAID DATA / SMS / VOICE PACKAGE → COUNT ----------
        if (isPaidPackage(lower)) {
            val cat = when {
                lower.contains("sms") && (lower.contains("gb") || lower.contains("mb") || lower.contains("data") || lower.contains("internet")) ->
                    Category.MIXED_PACKAGE
                lower.contains("sms package") || (lower.contains("sms") && !lower.contains("gb") && !lower.contains("mb")) ->
                    Category.SMS_PACKAGE
                lower.contains("voice") || lower.contains("min") -> Category.VOICE_PACKAGE
                else -> Category.DATA_PACKAGE
            }
            return Result(Classification.COUNT, cat, Direction.OUTGOING, 0.94f)
        }

        // ---------- PACKAGE ACTIVATION (no price) → REFERENCE ----------
        if (isPackageActivation(lower)) {
            return Result(Classification.REFERENCE, Category.PACKAGE_ACTIVATION, Direction.OUTGOING, 0.85f)
        }

        // ---------- SENT PACKAGE WITHOUT PRICE → REFERENCE ----------
        if (isPackageSentNoPrice(lower)) {
            return Result(Classification.REFERENCE, Category.DATA_PACKAGE, Direction.OUTGOING, 0.80f)
        }

        // ---------- AIRTIME RECHARGE CONFIRMATION (balance mentioned) → REFERENCE ----------
        if (isAirtimeRechargeConfirmation(lower)) {
            return Result(Classification.REFERENCE, Category.AIRTIME_RECHARGE, Direction.OUTGOING, 0.85f)
        }

        // ---------- BANK TRANSFER (generic) → IGNORE for now ----------
        if (lower.contains("transferred") && (lower.contains("bank") || lower.contains("account"))) {
            return Result(Classification.IGNORE, Category.BANK_TRANSFER, Direction.OUTGOING, 0.70f)
        }

        // Default: unknown → IGNORE (conservative)
        return Result(Classification.IGNORE, Category.OTHER, Direction.UNKNOWN, 0.30f)
    }

    private fun isPersonToPerson(lower: String): Boolean {
        return (lower.contains("transferred etb") || lower.contains("transferred birr") ||
                lower.contains("you have transferred")) &&
                (lower.contains("to ") || lower.contains("(251")) &&
                !lower.contains("airtime") && !lower.contains("package")
    }

    private fun isIncomingAirtime(lower: String): Boolean {
        return lower.contains("received airtime") ||
                lower.contains("you have received airtime top-up") ||
                (lower.contains("received") && lower.contains("airtime") && lower.contains("from"))
    }

    private fun isOutgoingAirtimeSent(lower: String): Boolean {
        return lower.contains("sent airtime top-up") ||
                lower.contains("successfully sent airtime") ||
                (lower.contains("sent") && lower.contains("airtime") && lower.contains("to"))
    }

    private fun isPrepaidRecharge(lower: String): Boolean {
        return lower.contains("prepaid account has been recharged") ||
                (lower.contains("recharged successfully") && lower.contains("recharged balance"))
    }

    private fun isPaidPackage(lower: String): Boolean {
        return (lower.contains("you have paid") || lower.contains("paid etb") || lower.contains("paid birr")) &&
                (lower.contains("package") || lower.contains("for package"))
    }

    private fun isPackageActivation(lower: String): Boolean {
        return (lower.contains("new service offer") || lower.contains("is added to your service") ||
                lower.contains("service offer is effective") || lower.contains("as per your request")) &&
                lower.contains("package") || lower.contains("service offer")
    }

    private fun isPackageSentNoPrice(lower: String): Boolean {
        return (lower.contains("successfully sent") && lower.contains("package") &&
                !lower.contains("paid") && !lower.contains("etb") && !lower.contains("birr"))
    }

    private fun isAirtimeRechargeConfirmation(lower: String): Boolean {
        return lower.contains("you have recharged") && lower.contains("your balance is")
    }
}
