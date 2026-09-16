package com.example.data

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.math.RoundingMode

class BigDecimalConverter {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.setScale(3, RoundingMode.HALF_EVEN)?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        if (value.isNullOrBlank()) return null
        return try {
            BigDecimal(value).setScale(3, RoundingMode.HALF_EVEN)
        } catch (e: Exception) {
            BigDecimal.ZERO.setScale(3, RoundingMode.HALF_EVEN)
        }
    }
}
