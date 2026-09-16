package com.example.data.repository

import com.example.data.dao.FixedExpenseDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.UserSettingsDao
import com.example.data.entity.FixedExpenseEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserSettingsEntity
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

class FinanceRepository(
    private val userSettingsDao: UserSettingsDao,
    private val fixedExpenseDao: FixedExpenseDao,
    private val transactionDao: TransactionDao
) {

    val settings: Flow<UserSettingsEntity?> = userSettingsDao.getSettings()

    val fixedExpenses: Flow<List<FixedExpenseEntity>> = fixedExpenseDao.getAllFixedExpenses()

    val currentMonthTransactions: Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForMonth(
            DateUtils.getStartOfCurrentMonth(),
            DateUtils.getEndOfCurrentMonth()
        )

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun getSettingsOnce(): UserSettingsEntity? {
        return userSettingsDao.getSettingsOnce()
    }

    suspend fun saveInitialSettings(
        monthlyIncome: BigDecimal,
        savingsTarget: BigDecimal,
        fixedExpensesList: List<Pair<String, BigDecimal>>,
        languageCode: String
    ) {
        val settings = UserSettingsEntity(
            id = 1,
            monthlyIncome = monthlyIncome,
            savingsTarget = savingsTarget,
            currency = "DT",
            languageCode = languageCode,
            isOnboardingCompleted = true
        )
        userSettingsDao.insertOrUpdate(settings)

        fixedExpenseDao.clear()
        if (fixedExpensesList.isNotEmpty()) {
            val entities = fixedExpensesList.map { (title, amount) ->
                FixedExpenseEntity(
                    title = title,
                    amount = amount,
                    category = "HOUSING"
                )
            }
            fixedExpenseDao.insertAll(entities)
        }
    }

    suspend fun updateBudget(income: BigDecimal, savings: BigDecimal) {
        userSettingsDao.updateIncomeAndSavings(income, savings)
    }

    suspend fun updateLanguage(languageCode: String) {
        val existing = userSettingsDao.getSettingsOnce()
        if (existing != null) {
            userSettingsDao.updateLanguage(languageCode)
        } else {
            userSettingsDao.insertOrUpdate(
                UserSettingsEntity(
                    id = 1,
                    monthlyIncome = BigDecimal("2000.000"),
                    savingsTarget = BigDecimal("300.000"),
                    currency = "DT",
                    languageCode = languageCode,
                    isOnboardingCompleted = false
                )
            )
        }
    }

    suspend fun addFixedExpense(title: String, amount: BigDecimal, category: String = "HOUSING") {
        fixedExpenseDao.insertFixedExpense(
            FixedExpenseEntity(
                title = title,
                amount = amount,
                category = category
            )
        )
    }

    suspend fun deleteFixedExpense(id: Long) {
        fixedExpenseDao.deleteById(id)
    }

    suspend fun addTransaction(
        title: String,
        amount: BigDecimal,
        category: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        transactionDao.insertTransaction(
            TransactionEntity(
                title = title,
                amount = amount,
                category = category,
                timestamp = timestamp
            )
        )
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun resetData() {
        userSettingsDao.clear()
        fixedExpenseDao.clear()
        transactionDao.clear()
    }
}
