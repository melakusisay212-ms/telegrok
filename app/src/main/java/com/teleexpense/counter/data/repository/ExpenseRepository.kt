package com.teleexpense.counter.data.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.teleexpense.counter.data.AppDatabase
import com.teleexpense.counter.data.entity.AppMetaEntity
import com.teleexpense.counter.data.entity.ProcessedSmsEntity
import com.teleexpense.counter.data.entity.toDomain
import com.teleexpense.counter.data.entity.toEntity
import com.teleexpense.counter.domain.calendar.EthiopianCalendarConverter
import com.teleexpense.counter.domain.grouping.TransactionGrouper
import com.teleexpense.counter.domain.model.*
import com.teleexpense.counter.domain.parser.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.GregorianCalendar

/**
 * Single source of truth for local telecom expenses.
 * All SMS reading & parsing happens here — never leaves the device.
 */
class ExpenseRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val txDao = db.transactionDao()
    private val smsDao = db.processedSmsDao()
    private val metaDao = db.appMetaDao()

    companion object {
        const val KEY_TRACKER_STARTED = "trackerStartedAt"
        const val KEY_LAST_PROCESSED = "lastProcessedAt"
        const val KEY_ONBOARDING_DONE = "onboardingDone"
    }

    fun observeTransactions(): Flow<List<TelecomTransaction>> =
        txDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAllTransactions(): List<TelecomTransaction> =
        txDao.getAll().map { it.toDomain() }

    suspend fun getTransaction(id: String): TelecomTransaction? =
        txDao.getById(id)?.toDomain()

    suspend fun getByEthiopianMonth(year: Int, month: Int): List<TelecomTransaction> =
        txDao.getByEthiopianMonth(year, month).map { it.toDomain() }

    suspend fun getByEthiopianDay(year: Int, month: Int, day: Int): List<TelecomTransaction> =
        txDao.getByEthiopianDay(year, month, day).map { it.toDomain() }

    suspend fun isOnboardingDone(): Boolean =
        metaDao.get(KEY_ONBOARDING_DONE) == "true"

    suspend fun setOnboardingDone() {
        metaDao.put(AppMetaEntity(KEY_ONBOARDING_DONE, "true"))
    }

    suspend fun getTrackerStartedAt(): Long? =
        metaDao.get(KEY_TRACKER_STARTED)?.toLongOrNull()

    suspend fun setTrackerStartedAt(millis: Long) {
        metaDao.put(AppMetaEntity(KEY_TRACKER_STARTED, millis.toString()))
        metaDao.put(AppMetaEntity(KEY_LAST_PROCESSED, millis.toString()))
    }

    suspend fun getLastProcessedAt(): Long? =
        metaDao.get(KEY_LAST_PROCESSED)?.toLongOrNull()

    suspend fun setLastProcessedAt(millis: Long) {
        metaDao.put(AppMetaEntity(KEY_LAST_PROCESSED, millis.toString()))
    }

    /**
     * Initial scan: only current Gregorian month (mapped via Ethiopian conversion for display).
     * Does NOT scan years of history.
     */
    suspend fun scanCurrentMonth(): ScanResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cal = GregorianCalendar().apply { timeInMillis = now }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis

        val messages = readSmsSince(monthStart)
        processMessages(messages, markStart = true)
    }

    /**
     * Scan only messages after last processed timestamp.
     */
    suspend fun scanNewMessages(): ScanResult = withContext(Dispatchers.IO) {
        val last = getLastProcessedAt() ?: getTrackerStartedAt() ?: System.currentTimeMillis()
        val messages = readSmsSince(last)
        processMessages(messages, markStart = false)
    }

    /**
     * Start from today — set tracker timestamp, no historical scan.
     */
    suspend fun startFromToday(): ScanResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        setTrackerStartedAt(now)
        setOnboardingDone()
        ScanResult(0, 0, 0, emptyList())
    }

    private suspend fun processMessages(
        messages: List<RawSms>,
        markStart: Boolean
    ): ScanResult {
        if (messages.isEmpty()) {
            if (markStart) {
                setTrackerStartedAt(System.currentTimeMillis())
                setOnboardingDone()
            }
            return ScanResult(0, 0, 0, emptyList())
        }

        val alreadyProcessed = smsDao.getAllIds().toSet()
        val toProcess = messages.filter { it.id !in alreadyProcessed }

        val parseResults = toProcess.map { sms ->
            SmsParser.parse(sms.id, sms.address, sms.body, sms.date)
        }

        // Mark processed (store only id + classification, never full body)
        smsDao.insertAll(parseResults.map {
            ProcessedSmsEntity(it.sourceSmsId, System.currentTimeMillis(), it.classification.name)
        })

        val newTransactions = TransactionGrouper.group(parseResults)
        // Avoid inserting duplicates against existing DB transactions (by transfer/transaction ID)
        val existing = txDao.getAll().map { it.toDomain() }
        val filtered = newTransactions.filter { candidate ->
            existing.none { ex ->
                (!candidate.transferId.isNullOrBlank() && candidate.transferId == ex.transferId) ||
                        (!candidate.transactionId.isNullOrBlank() && candidate.transactionId == ex.transactionId)
            }
        }

        txDao.insertAll(filtered.map { it.toEntity() })

        val maxDate = messages.maxOfOrNull { it.date } ?: System.currentTimeMillis()
        if (markStart) {
            setTrackerStartedAt(System.currentTimeMillis())
            setOnboardingDone()
        }
        setLastProcessedAt(maxDate)

        return ScanResult(
            relevantMessages = parseResults.size,
            transactionEvents = parseResults.count { it.classification != Classification.IGNORE },
            expensesCreated = filtered.size,
            transactions = filtered
        )
    }

    /**
     * Read SMS from device ContentResolver since a given timestamp.
     * Only metadata + body used in memory; body is not persisted.
     */
    private fun readSmsSince(sinceMillis: Long): List<RawSms> {
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(sinceMillis.toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC"

        val results = mutableListOf<RawSms>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            cursor?.let {
                val idIdx = it.getColumnIndex(Telephony.Sms._ID)
                val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                while (it.moveToNext()) {
                    val id = it.getString(idIdx) ?: continue
                    val addr = it.getString(addrIdx) ?: ""
                    val body = it.getString(bodyIdx) ?: continue
                    val date = it.getLong(dateIdx)
                    // Quick filter: only telecom-ish messages
                    if (looksTelecomRelated(addr, body)) {
                        results.add(RawSms(id, addr, body, date))
                    }
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted
        } finally {
            cursor?.close()
        }
        return results
    }

    private fun looksTelecomRelated(address: String, body: String): Boolean {
        val lower = body.lowercase()
        val addr = address.lowercase()
        val keywords = listOf(
            "etb", "birr", "airtime", "package", "telebirr", "ebirr", "recharged",
            "prepaid", "data", "sms", "top-up", "topup", "balance", "transaction",
            "ethio", "telecom", "transfer id", "paid"
        )
        return keywords.any { lower.contains(it) } ||
                addr.contains("telebirr") || addr.contains("ebirr") ||
                addr.contains("ethio") || addr.contains("telecom")
    }

    suspend fun updateTransaction(tx: TelecomTransaction) {
        txDao.update(tx.copy(isManuallyEdited = true).toEntity())
    }

    suspend fun deleteTransaction(id: String) {
        txDao.deleteById(id)
    }

    suspend fun excludeTransaction(id: String) {
        val entity = txDao.getById(id) ?: return
        txDao.update(entity.copy(isExcluded = true))
    }

    /**
     * Period summary for dashboard / analytics.
     */
    suspend fun getPeriodSummary(
        fromMillis: Long,
        toMillis: Long,
        ethiopianLabel: String
    ): PeriodSummary {
        val list = txDao.getByGregorianRange(fromMillis, toMillis)
            .map { it.toDomain() }
            .filter { !it.isExcluded }

        val total = list.sumOf { it.amount }
        val count = list.size
        val days = ((toMillis - fromMillis) / (24 * 60 * 60 * 1000L)).coerceAtLeast(1)
        val byCat = list.groupBy { it.category }
            .map { (cat, txs) ->
                val amt = txs.sumOf { it.amount }
                CategoryBreakdown(
                    category = cat,
                    amount = amt,
                    percentage = if (total > 0) ((amt / total) * 100).toFloat() else 0f,
                    count = txs.size
                )
            }
            .sortedByDescending { it.amount }

        return PeriodSummary(
            label = "Period",
            ethiopianLabel = ethiopianLabel,
            totalAmount = total,
            transactionCount = count,
            averagePerDay = total / days,
            averagePerTransaction = if (count > 0) total / count else 0.0,
            breakdown = byCat
        )
    }

    data class RawSms(val id: String, val address: String, val body: String, val date: Long)

    data class ScanResult(
        val relevantMessages: Int,
        val transactionEvents: Int,
        val expensesCreated: Int,
        val transactions: List<TelecomTransaction>
    )
}
