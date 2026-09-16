package com.example.util

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface FormattedDateResult {
    data object Today : FormattedDateResult
    data object Yesterday : FormattedDateResult
    data class Formatted(val text: String) : FormattedDateResult
}

object DateUtils {

    fun getStartOfCurrentMonth(calendar: Calendar = Calendar.getInstance()): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfCurrentMonth(calendar: Calendar = Calendar.getInstance()): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /**
     * Number of days remaining in the month including today.
     * (e.g., in a 30-day month on day 15, there are 16 days left: 15..30)
     * Always returns at least 1 to prevent division by zero.
     */
    fun getDaysRemainingInMonth(calendar: Calendar = Calendar.getInstance()): Int {
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val remaining = maxDays - currentDay + 1
        return if (remaining < 1) 1 else remaining
    }

    fun getCurrentMonthName(locale: Locale = Locale.getDefault()): String {
        val sdf = SimpleDateFormat("MMMM yyyy", locale)
        return sdf.format(Date())
    }

    fun resolveDate(timestamp: Long, locale: Locale = Locale.getDefault()): FormattedDateResult {
        val calendarNow = Calendar.getInstance()
        val calendarTarget = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isSameDay = calendarNow.get(Calendar.YEAR) == calendarTarget.get(Calendar.YEAR) &&
                calendarNow.get(Calendar.DAY_OF_YEAR) == calendarTarget.get(Calendar.DAY_OF_YEAR)

        calendarNow.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = calendarNow.get(Calendar.YEAR) == calendarTarget.get(Calendar.YEAR) &&
                calendarNow.get(Calendar.DAY_OF_YEAR) == calendarTarget.get(Calendar.DAY_OF_YEAR)

        return when {
            isSameDay -> FormattedDateResult.Today
            isYesterday -> FormattedDateResult.Yesterday
            else -> {
                val sdf = SimpleDateFormat("d MMM yyyy", locale)
                FormattedDateResult.Formatted(sdf.format(Date(timestamp)))
            }
        }
    }
}

object CurrencyUtils {
    fun formatAmount(amount: BigDecimal): String = MoneyUtils.formatAmount(amount)
    fun formatCurrency(amount: BigDecimal, currency: String = "DT"): String = MoneyUtils.formatCurrency(amount, currency)
}
