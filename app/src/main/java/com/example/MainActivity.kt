package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.repository.FinanceRepository
import com.example.ui.FinanceViewModel
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.screens.AddExpenseSheet
import com.example.ui.screens.BudgetSettingsSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FixedExpensesSheet
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.TransactionHistoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.LocaleHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy {
        FinanceRepository(
            userSettingsDao = database.userSettingsDao(),
            fixedExpenseDao = database.fixedExpenseDao(),
            transactionDao = database.transactionDao()
        )
    }

    private val viewModel: FinanceViewModel by viewModels {
        FinanceViewModel.Factory(repository)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val baseContext = LocalContext.current
            val localizedContext = remember(uiState.languageCode) {
                LocaleHelper.getLocalizedContext(baseContext, uiState.languageCode)
            }
            val layoutDirection = remember(uiState.languageCode) {
                LocaleHelper.getLayoutDirection(uiState.languageCode)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalLayoutDirection provides layoutDirection
            ) {
                MyApplicationTheme {
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        MainAppContent(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: FinanceViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showHistoryScreen by remember { mutableStateOf(false) }
    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var showFixedExpensesSheet by remember { mutableStateOf(false) }
    var showBudgetSettingsSheet by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val addExpenseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fixedExpensesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val budgetSettingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (!uiState.isOnboardingCompleted) {
        OnboardingScreen(
            currentLanguageCode = uiState.languageCode,
            onSelectLanguage = { code -> viewModel.updateLanguage(code) },
            onCompleteOnboarding = { income, savings, fixedList, lang ->
                viewModel.completeOnboarding(
                    income = income,
                    savings = savings,
                    fixedExpenses = fixedList,
                    languageCode = lang
                )
            }
        )
    } else {
        if (showHistoryScreen) {
            BackHandler { showHistoryScreen = false }
            TransactionHistoryScreen(
                transactions = uiState.currentMonthTransactions,
                languageCode = uiState.languageCode,
                onBack = { showHistoryScreen = false },
                onDeleteTransaction = { id -> viewModel.deleteExpense(id) }
            )
        } else {
            DashboardScreen(
                uiState = uiState,
                onAddExpenseClick = { showAddExpenseSheet = true },
                onManageFixedExpensesClick = { showFixedExpensesSheet = true },
                onEditBudgetClick = { showBudgetSettingsSheet = true },
                onViewHistoryClick = { showHistoryScreen = true },
                onChangeLanguageClick = { showLanguageDialog = true },
                onResetDataClick = { showResetDialog = true }
            )
        }

        // Add Expense Bottom Sheet
        if (showAddExpenseSheet) {
            AddExpenseSheet(
                sheetState = addExpenseSheetState,
                onDismiss = {
                    scope.launch { addExpenseSheetState.hide() }.invokeOnCompletion {
                        showAddExpenseSheet = false
                    }
                },
                onSaveExpense = { title, amount, category ->
                    viewModel.addExpense(title, amount, category)
                }
            )
        }

        // Fixed Expenses Bottom Sheet
        if (showFixedExpensesSheet) {
            FixedExpensesSheet(
                sheetState = fixedExpensesSheetState,
                fixedExpenses = uiState.fixedExpenses,
                totalFixedExpenses = uiState.totalFixedExpenses,
                onDismiss = {
                    scope.launch { fixedExpensesSheetState.hide() }.invokeOnCompletion {
                        showFixedExpensesSheet = false
                    }
                },
                onAddFixedExpense = { title, amount ->
                    viewModel.addFixedExpense(title, amount)
                },
                onDeleteFixedExpense = { id ->
                    viewModel.deleteFixedExpense(id)
                }
            )
        }

        // Budget Settings Bottom Sheet
        if (showBudgetSettingsSheet) {
            BudgetSettingsSheet(
                sheetState = budgetSettingsSheetState,
                currentIncome = uiState.monthlyIncome,
                currentSavings = uiState.savingsTarget,
                totalFixedExpenses = uiState.totalFixedExpenses,
                onDismiss = {
                    scope.launch { budgetSettingsSheetState.hide() }.invokeOnCompletion {
                        showBudgetSettingsSheet = false
                    }
                },
                onSaveBudget = { income, savings ->
                    viewModel.updateBudget(income, savings)
                }
            )
        }

        // Language Selector Dialog
        if (showLanguageDialog) {
            LanguageSelectorDialog(
                currentLanguageCode = uiState.languageCode,
                onLanguageSelected = { code ->
                    viewModel.updateLanguage(code)
                },
                onDismiss = { showLanguageDialog = false }
            )
        }

        // Reset Data Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(stringResource(R.string.reset_data)) },
                text = { Text(stringResource(R.string.reset_confirm)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.resetData()
                            showResetDialog = false
                            showHistoryScreen = false
                        },
                        modifier = Modifier.testTag("confirm_reset_button")
                    ) {
                        Text(stringResource(R.string.reset_data), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
