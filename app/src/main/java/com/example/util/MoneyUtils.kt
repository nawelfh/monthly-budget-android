package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyUtils {
    val MAX_MONEY_AMOUNT: BigDecimal = BigDecimal("10000000.000") // 10 million DT max threshold
    val ZERO: BigDecimal = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_EVEN)

    /**
     * Sanitizes user input string in text fields to prevent invalid characters.
     * Allows digits and at most one decimal separator (period or comma).
     */
    fun sanitizeInput(input: String): String {
        val clean = StringBuilder()
        var hasSeparator = false

        for (char in input) {
            if (char.isDigit()) {
                clean.append(char)
            } else if ((char == '.' || char == ',') && !hasSeparator) {
                clean.append('.')
                hasSeparator = true
            }
        }
        return clean.toString()
    }

    /**
     * Validates and parses user string input into a BigDecimal with scale 3.
     * Returns null if input is empty, negative, invalid, or exceeds maximum threshold.
     */
    fun parseAmount(input: String): BigDecimal? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // Normalize comma to period
        val normalized = trimmed.replace(',', '.')

        // Check for invalid patterns like multiple dots, negative sign, non-digits
        val parsed = try {
            BigDecimal(normalized)
        } catch (e: Exception) {
            return null
        }

        if (parsed < BigDecimal.ZERO) return null
        if (parsed > MAX_MONEY_AMOUNT) return null

        return parsed.setScale(3, RoundingMode.HALF_EVEN)
    }

    /**
     * Formats an amount according to Tunisian Dinar conventions:
     * - Whole numbers appear without decimal places: "2000" -> "2 000"
     * - Fractional millimes appear with 3 decimal places: "12.5" -> "12.500", "39.375" -> "39.375"
     * - Uses space as grouping separator.
     */
    fun formatAmount(amount: BigDecimal): String {
        val scaled = amount.setScale(3, RoundingMode.HALF_EVEN)
        val isWhole = scaled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ' '
            decimalSeparator = '.'
        }

        val pattern = if (isWhole) "#,##0" else "#,##0.000"
        val df = DecimalFormat(pattern, symbols)
        return df.format(scaled)
    }

    /**
     * Formats amount with currency suffix, e.g. "2 000 DT" or "12.500 DT"
     */
    fun formatCurrency(amount: BigDecimal, currency: String = "DT"): String {
        val formatted = formatAmount(amount)
        return "$formatted $currency"
    }
}
