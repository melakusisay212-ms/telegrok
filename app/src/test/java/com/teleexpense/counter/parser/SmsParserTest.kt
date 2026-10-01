package com.teleexpense.counter.parser

import com.google.common.truth.Truth.assertThat
import com.teleexpense.counter.domain.grouping.TransactionGrouper
import com.teleexpense.counter.domain.model.Category
import com.teleexpense.counter.domain.model.Classification
import com.teleexpense.counter.domain.model.Direction
import com.teleexpense.counter.domain.parser.SmsParser
import org.junit.Test

/**
 * Automated tests for every SMS example in the product spec.
 * Critical assertions:
 * - Three ETB 5 eBirr SMS → exactly ONE expense
 * - Two ETB 35 packages → exactly TWO expenses
 * - P2P transfer → ZERO
 * - Birthday free package → ZERO
 * - Package without price → ZERO until matched
 */
class SmsParserTest {

    private val baseTime = 1726550000000L // ~2024-ish; tests use explicit dates in body

    @Test
    fun exampleA_airtimeSent_isCount() {
        val body = "[-EBIRR-KAAFI-] Transfer ID: 802520883630, You have successfuly sent airtime top-up of ETB5 to 251981801919"
        val r = SmsParser.parse("1", "EBIRR", body, baseTime)
        assertThat(r.classification).isEqualTo(Classification.COUNT)
        assertThat(r.category).isEqualTo(Category.AIRTIME)
        assertThat(r.amount).isEqualTo(5.0)
        assertThat(r.direction).isEqualTo(Direction.OUTGOING)
        assertThat(r.transferId).isEqualTo("802520883630")
    }

    @Test
    fun exampleB_airtimeReceived_isIgnore() {
        val body = "[-EBIRR-KAAFI-] Transfer ID: 802520883630, You have received airtime top-up of ETB5 from 251981801919"
        val r = SmsParser.parse("2", "EBIRR", body, baseTime)
        assertThat(r.classification).isEqualTo(Classification.IGNORE)
        assertThat(r.direction).isEqualTo(Direction.INCOMING)
    }

    @Test
    fun exampleC_rechargeConfirmation_isReference() {
        val body = "[-EBIRR-KAAFI-] You have recharged ETB5 to 251981801919, your balance is ETB3.6"
        val r = SmsParser.parse("3", "EBIRR", body, baseTime)
        assertThat(r.classification).isEqualTo(Classification.REFERENCE)
        assertThat(r.amount).isEqualTo(5.0)
        // Balance 3.6 must NOT be the amount
        assertThat(r.amount).isNotEqualTo(3.6)
    }

    @Test
    fun threeEtb5Messages_groupToOneExpense() {
        val a = SmsParser.parse("1", "EBIRR",
            "[-EBIRR-KAAFI-] Transfer ID: 802520883630, You have successfuly sent airtime top-up of ETB5 to 251981801919",
            baseTime)
        val b = SmsParser.parse("2", "EBIRR",
            "[-EBIRR-KAAFI-] Transfer ID: 802520883630, You have received airtime top-up of ETB5 from 251981801919",
            baseTime + 1000)
        val c = SmsParser.parse("3", "EBIRR",
            "[-EBIRR-KAAFI-] You have recharged ETB5 to 251981801919, your balance is ETB3.6",
            baseTime + 2000)
        val txs = TransactionGrouper.group(listOf(a, b, c))
        assertThat(txs).hasSize(1)
        assertThat(txs[0].amount).isEqualTo(5.0)
        assertThat(txs[0].sourceEventIds).hasSize(3)
    }

    @Test
    fun telebirrDataPackage_isCount() {
        val body = """Dear MELAKU
You have paid ETB 35.00 for package Monthly student pack 1.2GB + 120SMS plus 1.2GB night bonus purchase made for 981801919 on 11/09/2026 15:52:15.
Your transaction number is DIB1N45Z9Z."""
        val r = SmsParser.parse("10", "telebirr", body, baseTime)
        assertThat(r.classification).isEqualTo(Classification.COUNT)
        assertThat(r.category).isEqualTo(Category.MIXED_PACKAGE)
        assertThat(r.amount).isEqualTo(35.0)
        assertThat(r.transactionId).isEqualTo("DIB1N45Z9Z")
        assertThat(r.packageName).contains("Monthly student pack")
        assertThat(r.gregorianDateTimeMillis).isNotNull()
    }

    @Test
    fun personToPerson_isIgnore_zeroExpense() {
        val body = """You have transferred ETB 10.00 to RUTA TAKELE (2519****3230) on 11/09/2026 21:21:46.
The service fee is ETB 0.87 and 15% VAT on the service fee is ETB 0.13."""
        val r = SmsParser.parse("20", "telebirr", body, baseTime)
        assertThat(r.classification).isEqualTo(Classification.IGNORE)
        assertThat(r.category).isEqualTo(Category.PERSON_TO_PERSON_TRANSFER)
        val txs = TransactionGrouper.group(listOf(r))
        assertThat(txs).isEmpty()
    }

    @Test
    fun secondDataPackage_isSeparate() {
        val body1 = """You have paid ETB 35.00 for package Monthly student pack 1.2GB + 120SMS plus 1.2GB night bonus purchase made for 981801919 on 11/09/2026 15:52:15.
Your transaction number is DIB1N45Z9Z."""
        val body2 = """You have paid ETB 35.00 for package Monthly student pack 1.2GB + 120SMS plus 1.2GB night bonus purchase made for 981801919 on 17/09/2026 08:38:04.
Your transaction number is DIH5SLOKQ1."""
        val r1 = SmsParser.parse("11", "telebirr", body1, baseTime)
        val r2 = SmsParser.parse("12", "telebirr", body2, baseTime + 6 * 24 * 3600_000L)
        val txs = TransactionGrouper.group(listOf(r1, r2))
        assertThat(txs).hasSize(2)
        assertThat(txs.map { it.amount }).containsExactly(35.0, 35.0)
    }

    @Test
    fun packageActivation_isReference() {
        val body = """As per your request the new service offer Monthly student pack 1.2GB + 120SMS plus 1.2GB night bonus from telebirr to expire after 30 days is added to your service number 0981801919.
The service offer is effective as of Sep 17, 2026 8:38:04 AM."""
        val r = SmsParser.parse("13", "telebirr", body, baseTime)
        assertThat(r.classification).isEqualTo(Classification.REFERENCE)
        assertThat(r.category).isEqualTo(Category.PACKAGE_ACTIVATION)
        val txs = TransactionGrouper.group(listOf(r))
        assertThat(txs).isEmpty()
    }

    @Test
    fun prepaidRecharge_100_isCount() {
        val body = """Your prepaid account has been recharged successfully.
Your Recharged balance is 100.00 Birr.
Your balance is 100.00 Birr."""
        val r = SmsParser.parse("30", "ethio", body, baseTime)
        assertThat(r.classification).isEqualTo(Classification.COUNT)
        assertThat(r.category).isEqualTo(Category.AIRTIME_RECHARGE)
        assertThat(r.amount).isEqualTo(100.0)
    }

    @Test
    fun prepaidRecharge_5_isCount() {
        val body = """Your prepaid account has been recharged successfully.
Your Recharged balance is 5.00 Birr.
Your balance is 5.00 Birr."""
        val r = SmsParser.parse("31", "ethio", body, baseTime)
        assertThat(r.classification).isEqualTo(Classification.COUNT)
        assertThat(r.amount).isEqualTo(5.0)
    }

    @Test
    fun freeBirthdayPackage_isIgnore() {
        val body = """Ethio telecom wishes you a Happy birthday!
Please enjoy 1GB internet, 20 Min and 20 SMS package Free gift, valid for 24-hours."""
        val r = SmsParser.parse("40", "ethio", body, baseTime)
        assertThat(r.classification).isEqualTo(Classification.IGNORE)
        assertThat(r.category).isEqualTo(Category.FREE_PACKAGE)
        val txs = TransactionGrouper.group(listOf(r))
        assertThat(txs).isEmpty()
    }

    @Test
    fun packageSentWithoutPrice_isReference_zeroExpense() {
        val body = "You have successfully sent Daily internet Package 300 MB to 0943178701."
        val r = SmsParser.parse("50", "ethio", body, baseTime)
        assertThat(r.classification).isEqualTo(Classification.REFERENCE)
        assertThat(r.amount).isNull()
        val txs = TransactionGrouper.group(listOf(r))
        assertThat(txs).isEmpty()
    }

    @Test
    fun packageWithoutPrice_matchedWithPaid_createsOneExpense() {
        val ref = SmsParser.parse("50", "ethio",
            "You have successfully sent Daily internet Package 300 MB to 0943178701.",
            baseTime)
        val paid = SmsParser.parse("51", "ethio",
            "You have paid ETB 10.00 for Daily internet Package 300 MB to 0943178701.",
            baseTime + 30_000)
        val txs = TransactionGrouper.group(listOf(ref, paid))
        assertThat(txs).hasSize(1)
        assertThat(txs[0].amount).isEqualTo(10.0)
    }
}
