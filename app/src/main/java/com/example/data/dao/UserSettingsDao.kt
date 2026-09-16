package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: UserSettingsEntity)

    @Query("UPDATE user_settings SET languageCode = :languageCode WHERE id = 1")
    suspend fun updateLanguage(languageCode: String)

    @Query("UPDATE user_settings SET monthlyIncome = :income, savingsTarget = :savings WHERE id = 1")
    suspend fun updateIncomeAndSavings(income: BigDecimal, savings: BigDecimal)

    @Query("UPDATE user_settings SET isOnboardingCompleted = :completed WHERE id = 1")
    suspend fun setOnboardingCompleted(completed: Boolean)

    @Query("DELETE FROM user_settings")
    suspend fun clear()
}
