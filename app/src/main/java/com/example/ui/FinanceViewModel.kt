package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.UserSettingsEntity
import com.example.data.repository.FinanceRepository
import com.example.util.DateUtils
import com.example.util.MoneyUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

class FinanceViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    val uiState: StateFlow<FinanceUiState> = combine(
        repository.settings,
        repository.fixedExpenses,
        repository.currentMonthTransactions
    ) { settings, fixedExpensesList, transactionsList ->
        val totalFixed = fixedExpensesList.fold(MoneyUtils.ZERO) { acc, e ->
            acc.add(e.amount)
        }
        val totalVariable = transactionsList.fold(MoneyUtils.ZERO) { acc, t ->
            acc.add(t.amount)
        }
        val daysRemaining = DateUtils.getDaysRemainingInMonth()

        val currentSettings = settings ?: UserSettingsEntity(
            id = 1,
            monthlyIncome = BigDecimal("2000.000"),
            savingsTarget = BigDecimal("300.000"),
            currency = "DT",
            languageCode = "en",
            isOnboardingCompleted = false
        )

        FinanceUiState(
            isLoading = false,
            isOnboardingCompleted = currentSettings.isOnboardingCompleted,
            monthlyIncome = currentSettings.monthlyIncome,
            savingsTarget = currentSettings.savingsTarget,
            totalFixedExpenses = totalFixed,
            totalVariableSpent = totalVariable,
            currency = currentSettings.currency,
            languageCode = currentSettings.languageCode,
            fixedExpenses = fixedExpensesList,
            currentMonthTransactions = transactionsList,
            daysRemainingInMonth = daysRemaining
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceUiState(isLoading = true)
    )

    fun completeOnboarding(
        income: BigDecimal,
        savings: BigDecimal,
        fixedExpenses: List<Pair<String, BigDecimal>>,
        languageCode: String
    ) {
        viewModelScope.launch {
            repository.saveInitialSettings(
                monthlyIncome = income,
                savingsTarget = savings,
                fixedExpensesList = fixedExpenses,
                languageCode = languageCode
            )
        }
    }

    fun updateBudget(income: BigDecimal, savings: BigDecimal) {
        viewModelScope.launch {
            repository.updateBudget(income, savings)
        }
    }

    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            repository.updateLanguage(languageCode)
        }
    }

    fun addExpense(
        title: String,
        amount: BigDecimal,
        category: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.addTransaction(title, amount, category, timestamp)
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun addFixedExpense(title: String, amount: BigDecimal, category: String = "HOUSING") {
        viewModelScope.launch {
            repository.addFixedExpense(title, amount, category)
        }
    }

    fun deleteFixedExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteFixedExpense(id)
        }
    }

    fun resetData() {
        viewModelScope.launch {
            repository.resetData()
        }
    }

    class Factory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
                return FinanceViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
