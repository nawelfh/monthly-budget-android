package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ExpenseCategory
import com.example.util.MoneyUtils
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSaveExpense: (title: String, amount: BigDecimal, category: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var isError by remember { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.new_expense),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_add_expense")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input with sanitization
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = MoneyUtils.sanitizeInput(input)
                    isError = false
                },
                label = { Text(stringResource(R.string.amount)) },
                placeholder = { Text(stringResource(R.string.amount_placeholder)) },
                suffix = { Text(stringResource(R.string.currency_symbol), fontWeight = FontWeight.Bold) },
                isError = isError,
                supportingText = if (isError) {
                    { Text(stringResource(R.string.invalid_amount_error), color = MaterialTheme.colorScheme.error) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_amount_input"),
                shape = RoundedCornerShape(16.dp)
            )

            // Quick preset chips
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5, 10, 20, 50, 100).forEach { presetInt ->
                    OutlinedButton(
                        onClick = {
                            val current = MoneyUtils.parseAmount(amountText) ?: BigDecimal.ZERO
                            val updated = current.add(BigDecimal(presetInt))
                            amountText = MoneyUtils.formatAmount(updated).replace(" ", "")
                            isError = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+$presetInt")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title / Description
            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text(stringResource(R.string.title)) },
                placeholder = { Text(stringResource(R.string.expense_title_placeholder)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_title_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Category selector
            Text(
                text = stringResource(R.string.category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExpenseCategory.entries.forEach { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(stringResource(category.stringResId)) },
                        leadingIcon = {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color(category.colorHex)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("category_chip_${category.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Actions
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
                        val parsed = MoneyUtils.parseAmount(amountText)
                        if (parsed != null && parsed > BigDecimal.ZERO) {
                            val resolvedTitle = if (titleText.isNotBlank()) titleText.trim() else ""
                            onSaveExpense(resolvedTitle, parsed, selectedCategory.name)
                            onDismiss()
                        } else {
                            isError = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("save_expense_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
