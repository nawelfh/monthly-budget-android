package com.example.ui

import com.example.data.entity.FixedExpenseEntity
import com.example.data.entity.TransactionEntity
import com.example.util.MoneyUtils
import java.math.BigDecimal
import java.math.RoundingMode

enum class BudgetHealth {
    HEALTHY,
    MODERATE,
    CAUTION,
    OVERSPENT
}

data class FinanceUiState(
    val isLoading: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val monthlyIncome: BigDecimal = BigDecimal("2000.000"),
    val savingsTarget: BigDecimal = BigDecimal("300.000"),
    val totalFixedExpenses: BigDecimal = MoneyUtils.ZERO,
    val totalVariableSpent: BigDecimal = MoneyUtils.ZERO,
    val currency: String = "DT",
    val languageCode: String = "en",
    val fixedExpenses: List<FixedExpenseEntity> = emptyList(),
    val currentMonthTransactions: List<TransactionEntity> = emptyList(),
    val daysRemainingInMonth: Int = 1
) {
    /**
     * Available budget = Monthly income - Fixed expenses - Planned savings
     */
    val availableBudget: BigDecimal
        get() = monthlyIncome.subtract(totalFixedExpenses).subtract(savingsTarget)

    /**
     * Remaining money = Available budget - Variable expenses already spent
     */
    val remainingMoney: BigDecimal
        get() = availableBudget.subtract(totalVariableSpent)

    /**
     * Recommended daily spending = Remaining money / Number of days remaining in the month.
     * Always returns 0 when remaining money is negative or zero, or when days is <= 0.
     * Never throws ArithmeticException, produces NaN, or Infinity.
     */
    val recommendedDailySpending: BigDecimal
        get() {
            if (daysRemainingInMonth <= 0) return MoneyUtils.ZERO
            if (remainingMoney <= BigDecimal.ZERO) return MoneyUtils.ZERO

            return remainingMoney.divide(
                BigDecimal(daysRemainingInMonth),
                3,
                RoundingMode.HALF_EVEN
            )
        }

    /**
     * Percentage of available budget spent so far (0f..1f).
     * Protected against division by zero and negative budgets.
     */
    val spentPercentage: Float
        get() {
            if (availableBudget <= BigDecimal.ZERO) {
                return if (totalVariableSpent > BigDecimal.ZERO) 1f else 0f
            }
            return try {
                val ratio = totalVariableSpent.divide(availableBudget, 4, RoundingMode.HALF_EVEN)
                ratio.toFloat().coerceIn(0f, 1f)
            } catch (e: Exception) {
                0f
            }
        }

    val health: BudgetHealth
        get() {
            if (remainingMoney <= BigDecimal.ZERO) return BudgetHealth.OVERSPENT
            if (availableBudget <= BigDecimal.ZERO) return BudgetHealth.OVERSPENT
            val ratio = try {
                totalVariableSpent.divide(availableBudget, 4, RoundingMode.HALF_EVEN)
            } catch (e: Exception) {
                BigDecimal.ZERO
            }
            return when {
                ratio > BigDecimal("0.8500") -> BudgetHealth.CAUTION
                ratio > BigDecimal("0.5000") -> BudgetHealth.MODERATE
                else -> BudgetHealth.HEALTHY
            }
        }
}
