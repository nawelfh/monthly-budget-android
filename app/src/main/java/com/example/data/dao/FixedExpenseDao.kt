package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.FixedExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedExpenseDao {
    @Query("SELECT * FROM fixed_expenses ORDER BY id DESC")
    fun getAllFixedExpenses(): Flow<List<FixedExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedExpense(expense: FixedExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<FixedExpenseEntity>)

    @Delete
    suspend fun deleteFixedExpense(expense: FixedExpenseEntity)

    @Query("DELETE FROM fixed_expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM fixed_expenses")
    suspend fun clear()
}
