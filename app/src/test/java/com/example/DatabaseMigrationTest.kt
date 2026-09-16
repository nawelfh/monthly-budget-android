package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.BigDecimalConverter
import com.example.data.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationTest {

    private lateinit var context: Context
    private val dbName = "test_migration.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun `verify migration from v1 to v2 preserves exact financial amounts IDs and dates`() = runBlocking {
        // Step 1: Create a Version 1 SQLite database with REAL numeric columns
        val v1Helper = object : SQLiteOpenHelper(context, dbName, null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE `user_settings` (
                        `id` INTEGER NOT NULL,
                        `monthlyIncome` REAL NOT NULL,
                        `savingsTarget` REAL NOT NULL,
                        `currency` TEXT NOT NULL,
                        `languageCode` TEXT NOT NULL,
                        `isOnboardingCompleted` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE `fixed_expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE `transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        // Step 2: Seed the v1 database with sample financial records
        val db = v1Helper.writableDatabase
        // Monthly income: 2000 TND, Savings: 300 TND
        db.execSQL(
            "INSERT INTO `user_settings` (`id`, `monthlyIncome`, `savingsTarget`, `currency`, `languageCode`, `isOnboardingCompleted`) VALUES (1, 2000.0, 300.0, 'DT', 'ar', 1)"
        )
        // Fixed expense: 650 TND with explicit ID
        db.execSQL(
            "INSERT INTO `fixed_expenses` (`id`, `title`, `amount`, `category`) VALUES (42, 'Rent & Utilities', 650.0, 'HOUSING')"
        )
        // Transactions: 100 TND, 50.500 TND, 29.750 TND with explicit IDs and timestamps
        val time1 = 1710000000000L
        val time2 = 1710100000000L
        val time3 = 1710200000000L
        db.execSQL(
            "INSERT INTO `transactions` (`id`, `title`, `amount`, `category`, `timestamp`) VALUES (101, 'Supermarket Groceries', 100.0, 'FOOD', $time1)"
        )
        db.execSQL(
            "INSERT INTO `transactions` (`id`, `title`, `amount`, `category`, `timestamp`) VALUES (102, 'Pharmacy Medicines', 50.50, 'HEALTH', $time2)"
        )
        db.execSQL(
            "INSERT INTO `transactions` (`id`, `title`, `amount`, `category`, `timestamp`) VALUES (103, 'City Taxi', 29.75, 'TRANSPORT', $time3)"
        )
        v1Helper.close()

        // Step 3: Upgrade application to Room Version 2 with MIGRATION_1_2
        val upgradedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

        // Step 4: Verify UserSettings survived intact
        val settings = upgradedDb.userSettingsDao().getSettingsOnce()
        assertNotNull("UserSettings must exist after migration", settings)
        assertEquals(1, settings!!.id)
        assertEquals("Monthly income 2000 TND must be preserved exactly", BigDecimal("2000.000"), settings.monthlyIncome)
        assertEquals("Savings target 300 TND must be preserved exactly", BigDecimal("300.000"), settings.savingsTarget)
        assertEquals("DT", settings.currency)
        assertEquals("ar", settings.languageCode)
        assertTrue("Onboarding completed status must be preserved", settings.isOnboardingCompleted)

        // Step 5: Verify Fixed Expenses survived intact
        val fixedExpenses = upgradedDb.fixedExpenseDao().getAllFixedExpenses().first()
        assertEquals("Fixed expense count must match exactly", 1, fixedExpenses.size)
        val rent = fixedExpenses[0]
        assertEquals("Fixed expense ID must be preserved exactly", 42L, rent.id)
        assertEquals("Rent & Utilities", rent.title)
        assertEquals("Fixed expense 650 TND must be preserved exactly", BigDecimal("650.000"), rent.amount)
        assertEquals("HOUSING", rent.category)

        // Step 6: Verify Transactions survived intact
        val transactions = upgradedDb.transactionDao().getAllTransactions().first()
        assertEquals("Transaction count must match exactly", 3, transactions.size)

        val txMap = transactions.associateBy { it.id }

        // Transaction 1: 100 TND
        val tx1 = txMap[101L]
        assertNotNull("Transaction 101 must exist", tx1)
        assertEquals("Supermarket Groceries", tx1!!.title)
        assertEquals("100 TND transaction must be preserved exactly", BigDecimal("100.000"), tx1.amount)
        assertEquals("FOOD", tx1.category)
        assertEquals(time1, tx1.timestamp)

        // Transaction 2: 50.500 TND
        val tx2 = txMap[102L]
        assertNotNull("Transaction 102 must exist", tx2)
        assertEquals("Pharmacy Medicines", tx2!!.title)
        assertEquals("50.500 TND transaction must be preserved exactly", BigDecimal("50.500"), tx2.amount)
        assertEquals("HEALTH", tx2.category)
        assertEquals(time2, tx2.timestamp)

        // Transaction 3: 29.750 TND
        val tx3 = txMap[103L]
        assertNotNull("Transaction 103 must exist", tx3)
        assertEquals("City Taxi", tx3!!.title)
        assertEquals("29.750 TND transaction must be preserved exactly", BigDecimal("29.750"), tx3.amount)
        assertEquals("TRANSPORT", tx3.category)
        assertEquals(time3, tx3.timestamp)

        upgradedDb.close()
    }

    @Test
    fun `verify application upgrade scenario with subsequent writes without reset or duplication`() = runBlocking {
        // Step 1: Create v1 database
        val v1Helper = object : SQLiteOpenHelper(context, dbName, null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL("CREATE TABLE `user_settings` (`id` INTEGER NOT NULL, `monthlyIncome` REAL NOT NULL, `savingsTarget` REAL NOT NULL, `currency` TEXT NOT NULL, `languageCode` TEXT NOT NULL, `isOnboardingCompleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE `fixed_expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, `category` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, `category` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }
        val v1Db = v1Helper.writableDatabase
        v1Db.execSQL("INSERT INTO `user_settings` VALUES (1, 2000.0, 300.0, 'DT', 'en', 1)")
        v1Db.execSQL("INSERT INTO `fixed_expenses` (`id`, `title`, `amount`, `category`) VALUES (1, 'Wifi', 50.0, 'UTILITIES')")
        v1Db.execSQL("INSERT INTO `transactions` (`id`, `title`, `amount`, `category`, `timestamp`) VALUES (1, 'Coffee', 3.5, 'FOOD', 1710000000000)")
        v1Helper.close()

        // Step 2: Open and migrate with v2
        var appDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

        // Verify initial migrated state
        assertEquals(1, appDb.transactionDao().getAllTransactions().first().size)
        assertEquals(1, appDb.fixedExpenseDao().getAllFixedExpenses().first().size)

        // Step 3: Perform user actions in v2 (add a new expense)
        val newTxId = appDb.transactionDao().insertTransaction(
            TransactionEntity(
                title = "Dinner",
                amount = BigDecimal("45.250"),
                category = "FOOD",
                timestamp = 1710050000000L
            )
        )
        assertTrue("New transaction should generate unique ID", newTxId > 1)

        // Step 4: Close and reopen database (simulate subsequent app launch)
        appDb.close()

        appDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

        // Verify both transactions exist without loss or duplication
        val allTx = appDb.transactionDao().getAllTransactions().first()
        assertEquals("Database must contain exactly 2 transactions after restart", 2, allTx.size)
        val coffee = allTx.first { it.id == 1L }
        val dinner = allTx.first { it.id == newTxId }

        assertEquals(BigDecimal("3.500"), coffee.amount)
        assertEquals(BigDecimal("45.250"), dinner.amount)

        // Verify settings and fixed expenses are unchanged
        val settings = appDb.userSettingsDao().getSettingsOnce()
        assertEquals(BigDecimal("2000.000"), settings!!.monthlyIncome)
        assertEquals(1, appDb.fixedExpenseDao().getAllFixedExpenses().first().size)

        appDb.close()
    }

    @Test
    fun `verify BigDecimalConverter roundtrip is strictly deterministic and locale independent`() {
        val converter = BigDecimalConverter()
        val originalLocale = Locale.getDefault()

        val testLocales = listOf(
            Locale.US,
            Locale.FRANCE,
            Locale("ar"),
            Locale.GERMANY,
            Locale.JAPAN
        )

        val testAmounts = listOf(
            BigDecimal("12.345"),
            BigDecimal("2000.000"),
            BigDecimal("300.000"),
            BigDecimal("650.000"),
            BigDecimal("100.000"),
            BigDecimal("50.500"),
            BigDecimal("29.750"),
            BigDecimal("0.000"),
            BigDecimal("0.125")
        )

        try {
            for (locale in testLocales) {
                Locale.setDefault(locale)

                for (amount in testAmounts) {
                    val serialized = converter.fromBigDecimal(amount)
                    assertNotNull(serialized)
                    assertTrue("Serialized string must use standard ASCII dot '.'", serialized!!.contains("."))
                    assertTrue("Serialized string must not contain commas", !serialized.contains(","))

                    val deserialized = converter.toBigDecimal(serialized)
                    assertNotNull(deserialized)
                    assertEquals("Roundtrip value must match exactly in locale $locale", amount, deserialized)
                }
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
