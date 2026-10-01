package com.teleexpense.counter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.teleexpense.counter.data.dao.AppMetaDao
import com.teleexpense.counter.data.dao.ProcessedSmsDao
import com.teleexpense.counter.data.dao.TransactionDao
import com.teleexpense.counter.data.entity.AppMetaEntity
import com.teleexpense.counter.data.entity.Converters
import com.teleexpense.counter.data.entity.ProcessedSmsEntity
import com.teleexpense.counter.data.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        ProcessedSmsEntity::class,
        AppMetaEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun processedSmsDao(): ProcessedSmsDao
    abstract fun appMetaDao(): AppMetaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tele_expense.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
