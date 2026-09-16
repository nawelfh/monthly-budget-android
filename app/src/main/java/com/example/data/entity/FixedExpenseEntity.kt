package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "fixed_expenses")
data class FixedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: BigDecimal,
    val category: String = "HOUSING"
)
