package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FixedExpenseDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.UserSettingsDao
import com.example.data.entity.FixedExpenseEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserSettingsEntity
import java.math.BigDecimal
import java.math.RoundingMode

@Database(
    entities = [
        UserSettingsEntity::class,
        FixedExpenseEntity::class,
        TransactionEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(BigDecimalConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun fixedExpenseDao(): FixedExpenseDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from Database Version 1 to Version 2.
         *
         * Version 1:
         * - Stored monetary amounts (monthlyIncome, savingsTarget, amount) as numeric (REAL/Double).
         *
         * Version 2:
         * - Stores monetary amounts deterministically as TEXT via BigDecimalConverter
         *   with 3-decimal exact millime precision (1 TND = 1 000 millimes).
         *
         * This migration preserves:
         * - monthly income
         * - savings target
         * - all fixed expenses with their exact IDs, titles, and categories
         * - all transactions with their exact IDs, titles, categories, and timestamps
         * - languageCode, currency, and onboarding completed status
         *
         * Amounts are converted directly from SQLite string representations to BigDecimal
         * without ever passing through binary floating-point types (Float/Double).
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                fun toExactMoneyString(raw: String?, default: String): String {
                    if (raw.isNullOrBlank()) return default
                    return try {
                        BigDecimal(raw.trim()).setScale(3, RoundingMode.HALF_EVEN).toPlainString()
                    } catch (e: Exception) {
                        default
                    }
                }

                fun tableExists(tableName: String): Boolean {
                    val cursor = db.query(
                        "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
                        arrayOf(tableName)
                    )
                    return cursor.use { it.moveToFirst() }
                }

                // 1. Migrate user_settings
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_settings_v2_temp` (
                        `id` INTEGER NOT NULL,
                        `monthlyIncome` TEXT NOT NULL,
                        `savingsTarget` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `languageCode` TEXT NOT NULL,
                        `isOnboardingCompleted` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                if (tableExists("user_settings")) {
                    val cursor = db.query("SELECT id, monthlyIncome, savingsTarget, currency, languageCode, isOnboardingCompleted FROM `user_settings`")
                    cursor.use { c ->
                        while (c.moveToNext()) {
                            val id = c.getLong(0)
                            val incomeRaw = c.getString(1)
                            val savingsRaw = c.getString(2)
                            val currency = c.getString(3) ?: "DT"
                            val languageCode = c.getString(4) ?: "en"
                            val completed = c.getLong(5)

                            val income = toExactMoneyString(incomeRaw, "2000.000")
                            val savings = toExactMoneyString(savingsRaw, "300.000")

                            val stmt = db.compileStatement(
                                "INSERT INTO `user_settings_v2_temp` (`id`, `monthlyIncome`, `savingsTarget`, `currency`, `languageCode`, `isOnboardingCompleted`) VALUES (?, ?, ?, ?, ?, ?)"
                            )
                            try {
                                stmt.bindLong(1, id)
                                stmt.bindString(2, income)
                                stmt.bindString(3, savings)
                                stmt.bindString(4, currency)
                                stmt.bindString(5, languageCode)
                                stmt.bindLong(6, completed)
                                stmt.executeInsert()
                            } finally {
                                stmt.close()
                            }
                        }
                    }
                    db.execSQL("DROP TABLE `user_settings`")
                }
                db.execSQL("ALTER TABLE `user_settings_v2_temp` RENAME TO `user_settings`")

                // 2. Migrate fixed_expenses
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fixed_expenses_v2_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `category` TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                if (tableExists("fixed_expenses")) {
                    val cursor = db.query("SELECT id, title, amount, category FROM `fixed_expenses`")
                    cursor.use { c ->
                        while (c.moveToNext()) {
                            val id = c.getLong(0)
                            val title = c.getString(1) ?: ""
                            val amountRaw = c.getString(2)
                            val category = c.getString(3) ?: "HOUSING"

                            val amount = toExactMoneyString(amountRaw, "0.000")

                            val stmt = db.compileStatement(
                                "INSERT INTO `fixed_expenses_v2_temp` (`id`, `title`, `amount`, `category`) VALUES (?, ?, ?, ?)"
                            )
                            try {
                                stmt.bindLong(1, id)
                                stmt.bindString(2, title)
                                stmt.bindString(3, amount)
                                stmt.bindString(4, category)
                                stmt.executeInsert()
                            } finally {
                                stmt.close()
                            }
                        }
                    }
                    db.execSQL("DROP TABLE `fixed_expenses`")
                }
                db.execSQL("ALTER TABLE `fixed_expenses_v2_temp` RENAME TO `fixed_expenses`")

                // 3. Migrate transactions
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions_v2_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                if (tableExists("transactions")) {
                    val cursor = db.query("SELECT id, title, amount, category, timestamp FROM `transactions`")
                    cursor.use { c ->
                        while (c.moveToNext()) {
                            val id = c.getLong(0)
                            val title = c.getString(1) ?: ""
                            val amountRaw = c.getString(2)
                            val category = c.getString(3) ?: "OTHER"
                            val timestamp = c.getLong(4)

                            val amount = toExactMoneyString(amountRaw, "0.000")

                            val stmt = db.compileStatement(
                                "INSERT INTO `transactions_v2_temp` (`id`, `title`, `amount`, `category`, `timestamp`) VALUES (?, ?, ?, ?, ?)"
                            )
                            try {
                                stmt.bindLong(1, id)
                                stmt.bindString(2, title)
                                stmt.bindString(3, amount)
                                stmt.bindString(4, category)
                                stmt.bindLong(5, timestamp)
                                stmt.executeInsert()
                            } finally {
                                stmt.close()
                            }
                        }
                    }
                    db.execSQL("DROP TABLE `transactions`")
                }
                db.execSQL("ALTER TABLE `transactions_v2_temp` RENAME TO `transactions`")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_finance.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
