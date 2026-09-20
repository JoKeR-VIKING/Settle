package com.settle.tracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.settle.tracker.db.ExpenseDraftDao
import com.settle.tracker.db.ExpenseEntity
import com.settle.tracker.db.PatternCandidateEntity
import com.settle.tracker.db.SpendPatternDao
import com.settle.tracker.db.SpendPatternEntity
import com.settle.tracker.db.SpendPatternNegativeEntity

@Database(
    entities = [
        ExpenseEntity::class,
        SpendPatternEntity::class,
        SpendPatternNegativeEntity::class,
        PatternCandidateEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDraftDao(): ExpenseDraftDao
    abstract fun spendPatternDao(): SpendPatternDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expense_drafts ADD COLUMN patternId TEXT")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS spend_patterns (
                        id TEXT NOT NULL PRIMARY KEY,
                        label TEXT NOT NULL,
                        category TEXT NOT NULL,
                        state TEXT NOT NULL,
                        amountMin REAL NOT NULL,
                        amountMax REAL NOT NULL,
                        timeStartMinutes INTEGER NOT NULL,
                        timeEndMinutes INTEGER NOT NULL,
                        confirmStreak INTEGER NOT NULL,
                        occurrenceCount INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastMatchedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS spend_pattern_negatives (
                        rowId INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        patternId TEXT NOT NULL,
                        amount REAL NOT NULL,
                        timeMinutes INTEGER NOT NULL,
                        excludedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pattern_candidates (
                        rowId INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        label TEXT NOT NULL,
                        category TEXT NOT NULL,
                        amount REAL NOT NULL,
                        transactionDate TEXT NOT NULL,
                        timestampMillis INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "settle_db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
