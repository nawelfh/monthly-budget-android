package com.example

import com.example.ui.BudgetHealth
import com.example.ui.FinanceUiState
import com.example.util.DateUtils
import com.example.util.MoneyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun `verify user prompt budget calculation example`() {
        // User prompt example:
        // Monthly income: 2,000 TND
        // Fixed expenses: 650 TND
        // Savings: 300 TND
        // Variable expenses spent: 420 TND
        // Remaining money: 630 TND
        val state = FinanceUiState(
            isLoading = false,
            isOnboardingCompleted = true,
            monthlyIncome = BigDecimal("2000.000"),
            totalFixedExpenses = BigDecimal("650.000"),
            savingsTarget = BigDecimal("300.000"),
            totalVariableSpent = BigDecimal("420.000"),
            daysRemainingInMonth = 15
        )

        // Available budget = 2000 - 650 - 300 = 1050
        assertEquals(BigDecimal("1050.000"), state.availableBudget)
        // Remaining money = 1050 - 420 = 630
        assertEquals(BigDecimal("630.000"), state.remainingMoney)
        // Recommended daily spending = 630 / 15 = 42
        assertEquals(BigDecimal("42.000"), state.recommendedDailySpending)
        assertEquals(BudgetHealth.HEALTHY, state.health)
    }

    @Test
    fun `verify edge case - zero income`() {
        val state = FinanceUiState(
            monthlyIncome = BigDecimal.ZERO,
            totalFixedExpenses = BigDecimal("200.000"),
            savingsTarget = BigDecimal("100.000"),
            totalVariableSpent = BigDecimal.ZERO,
            daysRemainingInMonth = 10
        )

        // Available budget = 0 - 200 - 100 = -300
        assertEquals(BigDecimal("-300.000"), state.availableBudget)
        assertEquals(BigDecimal("-300.000"), state.remainingMoney)
        // Daily spending must be 0, never negative or crash
        assertEquals(BigDecimal.ZERO.setScale(3), state.recommendedDailySpending)
        assertEquals(BudgetHealth.OVERSPENT, state.health)
    }

    @Test
    fun `verify edge case - fixed expenses greater than income`() {
        val state = FinanceUiState(
            monthlyIncome = BigDecimal("1000.000"),
            totalFixedExpenses = BigDecimal("1200.000"),
            savingsTarget = BigDecimal.ZERO,
            totalVariableSpent = BigDecimal.ZERO,
            daysRemainingInMonth = 10
        )

        assertEquals(BigDecimal("-200.000"), state.availableBudget)
        assertEquals(BigDecimal("-200.000"), state.remainingMoney)
        assertEquals(BigDecimal.ZERO.setScale(3), state.recommendedDailySpending)
        assertEquals(BudgetHealth.OVERSPENT, state.health)
    }

    @Test
    fun `verify edge case - savings greater than available income`() {
        val state = FinanceUiState(
            monthlyIncome = BigDecimal("1000.000"),
            totalFixedExpenses = BigDecimal("600.000"),
            savingsTarget = BigDecimal("500.000"), // 1000 - 600 - 500 = -100
            totalVariableSpent = BigDecimal.ZERO,
            daysRemainingInMonth = 10
        )

        assertEquals(BigDecimal("-100.000"), state.availableBudget)
        assertEquals(BigDecimal("-100.000"), state.remainingMoney)
        assertEquals(BigDecimal.ZERO.setScale(3), state.recommendedDailySpending)
    }

    @Test
    fun `verify edge case - variable expenses exceed budget`() {
        val state = FinanceUiState(
            monthlyIncome = BigDecimal("1000.000"),
            totalFixedExpenses = BigDecimal("300.000"),
            savingsTarget = BigDecimal("100.000"), // Available: 600
            totalVariableSpent = BigDecimal("750.000"), // Remaining: -150
            daysRemainingInMonth = 5
        )

        assertEquals(BigDecimal("600.000"), state.availableBudget)
        assertEquals(BigDecimal("-150.000"), state.remainingMoney)
        assertEquals(BigDecimal.ZERO.setScale(3), state.recommendedDailySpending)
        assertEquals(BudgetHealth.OVERSPENT, state.health)
    }

    @Test
    fun `verify edge case - first day and last day of month`() {
        // First day of 31-day month (e.g. Day 1): 31 days remaining
        val calDay1 = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1)
        }
        assertEquals(31, DateUtils.getDaysRemainingInMonth(calDay1))

        // Last day of 31-day month (e.g. Day 31): 1 day remaining
        val calDay31 = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 31)
        }
        assertEquals(1, DateUtils.getDaysRemainingInMonth(calDay31))

        // State with 1 day left: daily safe spending equals total remaining
        val state = FinanceUiState(
            monthlyIncome = BigDecimal("1000.000"),
            totalFixedExpenses = BigDecimal("400.000"),
            savingsTarget = BigDecimal("100.000"),
            totalVariableSpent = BigDecimal("450.000"), // Remaining: 50
            daysRemainingInMonth = 1
        )
        assertEquals(BigDecimal("50.000"), state.remainingMoney)
        assertEquals(BigDecimal("50.000"), state.recommendedDailySpending)
    }

    @Test
    fun `verify edge case - leap year February vs non-leap year`() {
        // 2024 is leap year -> Feb has 29 days
        val calLeap = Calendar.getInstance().apply {
            set(2024, Calendar.FEBRUARY, 1)
        }
        assertEquals(29, DateUtils.getDaysRemainingInMonth(calLeap))

        // 2025 is non-leap year -> Feb has 28 days
        val calNonLeap = Calendar.getInstance().apply {
            set(2025, Calendar.FEBRUARY, 1)
        }
        assertEquals(28, DateUtils.getDaysRemainingInMonth(calNonLeap))
    }

    @Test
    fun `verify money precision - exact 3-decimal millime calculations`() {
        // Tunisian Dinar: 1 TND = 1000 millimes
        val initial = BigDecimal("100.125")
        val expense = BigDecimal("33.625")
        val remaining = initial.subtract(expense)
        assertEquals(BigDecimal("66.500"), remaining)

        // Multiple small transactions summing precisely without binary float drift
        var sum = BigDecimal.ZERO.setScale(3)
        for (i in 1..10) {
            sum = sum.add(BigDecimal("0.100"))
        }
        assertEquals(BigDecimal("1.000"), sum)
    }

    @Test
    fun `verify currency display formatting requirements`() {
        // Requirement: 2000 TND should appear as "2 000 DT"
        // 12.5 TND should appear as "12.500 DT"
        // Avoid "2000.000 DT"
        assertEquals("2 000 DT", MoneyUtils.formatCurrency(BigDecimal("2000.000"), "DT"))
        assertEquals("12.500 DT", MoneyUtils.formatCurrency(BigDecimal("12.500"), "DT"))
        assertEquals("630 DT", MoneyUtils.formatCurrency(BigDecimal("630.000"), "DT"))
        assertEquals("420 DT", MoneyUtils.formatCurrency(BigDecimal("420.000"), "DT"))
        assertEquals("0 DT", MoneyUtils.formatCurrency(BigDecimal("0.000"), "DT"))
        assertEquals("-150 DT", MoneyUtils.formatCurrency(BigDecimal("-150.000"), "DT"))
    }

    @Test
    fun `verify input validation - sanitization and parsing`() {
        // Valid inputs
        assertEquals(BigDecimal("12.500"), MoneyUtils.parseAmount("12.5"))
        assertEquals(BigDecimal("12.500"), MoneyUtils.parseAmount("12,5"))
        assertEquals(BigDecimal("2000.000"), MoneyUtils.parseAmount("2000"))
        assertEquals(BigDecimal("0.500"), MoneyUtils.parseAmount("0.5"))

        // Invalid inputs must return null and never throw
        assertNull(MoneyUtils.parseAmount(""))
        assertNull(MoneyUtils.parseAmount("   "))
        assertNull(MoneyUtils.parseAmount("-50"))
        assertNull(MoneyUtils.parseAmount("abc"))
        assertNull(MoneyUtils.parseAmount("12.34.56"))
        assertNull(MoneyUtils.parseAmount("100000000000000000000000")) // Exceeds MAX_MONEY_AMOUNT

        // Input sanitizer
        assertEquals("12.5", MoneyUtils.sanitizeInput("12.5"))
        assertEquals("12.5", MoneyUtils.sanitizeInput("12,5"))
        assertEquals("12.50", MoneyUtils.sanitizeInput("12..5..0"))
        assertEquals("1250", MoneyUtils.sanitizeInput("12abc50"))
    }
}
