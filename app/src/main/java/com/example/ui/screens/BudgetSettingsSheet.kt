package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.util.CurrencyUtils
import com.example.util.MoneyUtils
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsSheet(
    sheetState: SheetState,
    currentIncome: BigDecimal,
    currentSavings: BigDecimal,
    totalFixedExpenses: BigDecimal,
    onDismiss: () -> Unit,
    onSaveBudget: (income: BigDecimal, savings: BigDecimal) -> Unit
) {
    var incomeText by remember { mutableStateOf(MoneyUtils.formatAmount(currentIncome).replace(" ", "")) }
    var savingsText by remember { mutableStateOf(MoneyUtils.formatAmount(currentSavings).replace(" ", "")) }
    var isIncomeError by remember { mutableStateOf(false) }

    val parsedIncome = MoneyUtils.parseAmount(incomeText) ?: BigDecimal.ZERO
    val parsedSavings = MoneyUtils.parseAmount(savingsText) ?: BigDecimal.ZERO
    val calculatedAvailable = parsedIncome.subtract(totalFixedExpenses).subtract(parsedSavings)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .imePadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.edit_budget),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_budget_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Income field
            OutlinedTextField(
                value = incomeText,
                onValueChange = { input ->
                    incomeText = MoneyUtils.sanitizeInput(input)
                    isIncomeError = false
                },
                label = { Text(stringResource(R.string.monthly_income)) },
                suffix = { Text(stringResource(R.string.currency_symbol)) },
                isError = isIncomeError,
                supportingText = if (isIncomeError) {
                    { Text(stringResource(R.string.invalid_amount_error), color = MaterialTheme.colorScheme.error) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_income_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Savings field
            OutlinedTextField(
                value = savingsText,
                onValueChange = { input ->
                    savingsText = MoneyUtils.sanitizeInput(input)
                },
                label = { Text(stringResource(R.string.planned_savings)) },
                suffix = { Text(stringResource(R.string.currency_symbol)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_savings_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live Calculation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.available_budget),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = CurrencyUtils.formatCurrency(calculatedAvailable),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (calculatedAvailable >= BigDecimal.ZERO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.budget_formula_note,
                            CurrencyUtils.formatAmount(parsedIncome),
                            CurrencyUtils.formatAmount(totalFixedExpenses),
                            CurrencyUtils.formatAmount(parsedSavings)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = {
                        val parsedInc = MoneyUtils.parseAmount(incomeText)
                        val parsedSav = MoneyUtils.parseAmount(savingsText) ?: BigDecimal.ZERO
                        if (parsedInc != null) {
                            onSaveBudget(parsedInc, parsedSav)
                            onDismiss()
                        } else {
                            isIncomeError = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("save_budget_btn"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
