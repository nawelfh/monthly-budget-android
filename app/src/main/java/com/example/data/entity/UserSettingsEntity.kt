package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val monthlyIncome: BigDecimal = BigDecimal("2000.000"),
    val savingsTarget: BigDecimal = BigDecimal("300.000"),
    val currency: String = "DT",
    val languageCode: String = "en",
    val isOnboardingCompleted: Boolean = false
)
