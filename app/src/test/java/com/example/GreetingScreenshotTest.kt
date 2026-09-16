package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.FinanceUiState
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun dashboard_screenshot() {
    val sampleState = FinanceUiState(
        isLoading = false,
        isOnboardingCompleted = true,
        monthlyIncome = BigDecimal("2000.000"),
        totalFixedExpenses = BigDecimal("650.000"),
        savingsTarget = BigDecimal("300.000"),
        totalVariableSpent = BigDecimal("420.000"),
        daysRemainingInMonth = 16
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          DashboardScreen(
              uiState = sampleState,
              onAddExpenseClick = {},
              onManageFixedExpensesClick = {},
              onEditBudgetClick = {},
              onViewHistoryClick = {},
              onChangeLanguageClick = {},
              onResetDataClick = {}
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
