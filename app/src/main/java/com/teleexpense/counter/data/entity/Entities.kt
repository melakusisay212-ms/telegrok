package com.teleexpense.counter.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.teleexpense.counter.domain.model.*

@Entity(tableName = "transactions")
@TypeConverters(Converters::class)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val provider: String,
    val category: String,
    val amount: Double,
    val currency: String,
    val direction: String,
    val status: String,
    val gregorianDateTimeMillis: Long,
    val ethiopianYear: Int,
    val ethiopianMonth: Int,
    val ethiopianDay: Int,
    val packageName: String?,
    val recipient: String?,
    val transactionId: String?,
    val transferId: String?,
    val sourceEventIdsJson: String,   // JSON array of SMS IDs
    val classification: String,
    val confidence: Float,
    val createdAtMillis: Long,
    val isManuallyEdited: Boolean = false,
    val isExcluded: Boolean = false
)

@Entity(tableName = "processed_sms")
data class ProcessedSmsEntity(
    @PrimaryKey val smsId: String,
    val processedAtMillis: Long,
    val classification: String
)

@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val key: String,
    val value: String
)

class Converters {
    @TypeConverter
    fun fromStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun toStringList(list: List<String>): String = list.joinToString(",")
}

fun TransactionEntity.toDomain(): TelecomTransaction {
    val ids = if (sourceEventIdsJson.isBlank()) emptyList()
    else sourceEventIdsJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return TelecomTransaction(
        id = id,
        provider = runCatching { Provider.valueOf(provider) }.getOrDefault(Provider.UNKNOWN),
        category = runCatching { Category.valueOf(category) }.getOrDefault(Category.OTHER),
        amount = amount,
        currency = currency,
        direction = runCatching { Direction.valueOf(direction) }.getOrDefault(Direction.UNKNOWN),
        status = runCatching { TransactionStatus.valueOf(status) }.getOrDefault(TransactionStatus.UNKNOWN),
        gregorianDateTimeMillis = gregorianDateTimeMillis,
        ethiopianYear = ethiopianYear,
        ethiopianMonth = ethiopianMonth,
        ethiopianDay = ethiopianDay,
        packageName = packageName,
        recipient = recipient,
        transactionId = transactionId,
        transferId = transferId,
        sourceEventIds = ids,
        classification = runCatching { Classification.valueOf(classification) }.getOrDefault(Classification.COUNT),
        confidence = confidence,
        createdAtMillis = createdAtMillis,
        isManuallyEdited = isManuallyEdited,
        isExcluded = isExcluded
    )
}

fun TelecomTransaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        provider = provider.name,
        category = category.name,
        amount = amount,
        currency = currency,
        direction = direction.name,
        status = status.name,
        gregorianDateTimeMillis = gregorianDateTimeMillis,
        ethiopianYear = ethiopianYear,
        ethiopianMonth = ethiopianMonth,
        ethiopianDay = ethiopianDay,
        packageName = packageName,
        recipient = recipient,
        transactionId = transactionId,
        transferId = transferId,
        sourceEventIdsJson = sourceEventIds.joinToString(","),
        classification = classification.name,
        confidence = confidence,
        createdAtMillis = createdAtMillis,
        isManuallyEdited = isManuallyEdited,
        isExcluded = isExcluded
    )
}
