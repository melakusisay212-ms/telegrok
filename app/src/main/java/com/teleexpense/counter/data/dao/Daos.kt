package com.teleexpense.counter.data.dao

import androidx.room.*
import com.teleexpense.counter.data.entity.AppMetaEntity
import com.teleexpense.counter.data.entity.ProcessedSmsEntity
import com.teleexpense.counter.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isExcluded = 0 ORDER BY gregorianDateTimeMillis DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isExcluded = 0 ORDER BY gregorianDateTimeMillis DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("""
        SELECT * FROM transactions 
        WHERE isExcluded = 0 
          AND ethiopianYear = :year AND ethiopianMonth = :month
        ORDER BY ethiopianDay ASC, gregorianDateTimeMillis ASC
    """)
    suspend fun getByEthiopianMonth(year: Int, month: Int): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions 
        WHERE isExcluded = 0 
          AND ethiopianYear = :year AND ethiopianMonth = :month AND ethiopianDay = :day
        ORDER BY gregorianDateTimeMillis ASC
    """)
    suspend fun getByEthiopianDay(year: Int, month: Int, day: Int): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions 
        WHERE isExcluded = 0 
          AND gregorianDateTimeMillis >= :fromMillis AND gregorianDateTimeMillis < :toMillis
        ORDER BY gregorianDateTimeMillis DESC
    """)
    suspend fun getByGregorianRange(fromMillis: Long, toMillis: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TransactionEntity>)

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT SUM(amount) FROM transactions WHERE isExcluded = 0 AND ethiopianYear = :year AND ethiopianMonth = :month")
    suspend fun sumForEthiopianMonth(year: Int, month: Int): Double?

    @Query("SELECT COUNT(*) FROM transactions WHERE isExcluded = 0")
    suspend fun count(): Int
}

@Dao
interface ProcessedSmsDao {
    @Query("SELECT smsId FROM processed_sms")
    suspend fun getAllIds(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM processed_sms WHERE smsId = :smsId)")
    suspend fun isProcessed(smsId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ProcessedSmsEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<ProcessedSmsEntity>)
}

@Dao
interface AppMetaDao {
    @Query("SELECT value FROM app_meta WHERE key = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: AppMetaEntity)

    @Query("DELETE FROM app_meta WHERE key = :key")
    suspend fun delete(key: String)
}
