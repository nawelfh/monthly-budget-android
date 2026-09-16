package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.util.CurrencyUtils
import com.example.util.MoneyUtils
import java.math.BigDecimal

@Composable
fun OnboardingScreen(
    currentLanguageCode: String,
    onSelectLanguage: (String) -> Unit,
    onCompleteOnboarding: (
        income: BigDecimal,
        savings: BigDecimal,
        fixedExpenses: List<Pair<String, BigDecimal>>,
        languageCode: String
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(0) } // 0: Lang, 1: Income, 2: Fixed Expenses, 3: Savings
    var selectedLang by remember { mutableStateOf(currentLanguageCode) }

    var incomeInput by remember { mutableStateOf("2000") }
    var savingsInput by remember { mutableStateOf("300") }
    var incomeError by remember { mutableStateOf(false) }

    val defaultRentTitle = stringResource(R.string.default_rent_title)
    val defaultBillsTitle = stringResource(R.string.default_bills_title)

    val fixedList = remember {
        mutableStateListOf(
            Pair(defaultRentTitle, BigDecimal("500.000")),
            Pair(defaultBillsTitle, BigDecimal("150.000"))
        )
    }

    var newFixedTitle by remember { mutableStateOf("") }
    var newFixedAmount by remember { mutableStateOf("") }
    var newFixedError by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .imePadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("onboarding_back_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.onboarding_btn_back))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        when (step) {
                            0 -> step = 1
                            1 -> {
                                val parsed = MoneyUtils.parseAmount(incomeInput)
                                if (parsed != null) {
                                    incomeError = false
                                    step = 2
                                } else {
                                    incomeError = true
                                }
                            }
                            2 -> step = 3
                            3 -> {
                                val inc = MoneyUtils.parseAmount(incomeInput) ?: BigDecimal("2000.000")
                                val sav = MoneyUtils.parseAmount(savingsInput) ?: BigDecimal("300.000")
                                onCompleteOnboarding(inc, sav, fixedList.toList(), selectedLang)
                            }
                        }
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("onboarding_next_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (step == 3) {
                            stringResource(R.string.onboarding_btn_start)
                        } else {
                            stringResource(R.string.onboarding_btn_next)
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0..3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (i <= step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_step_animation"
            ) { currentStep ->
                when (currentStep) {
                    0 -> StepLanguage(
                        selectedLang = selectedLang,
                        onSelect = { lang ->
                            selectedLang = lang
                            onSelectLanguage(lang)
                        }
                    )
                    1 -> StepIncome(
                        income = incomeInput,
                        isError = incomeError,
                        onIncomeChange = {
                            incomeInput = MoneyUtils.sanitizeInput(it)
                            incomeError = false
                        }
                    )
                    2 -> StepFixedExpenses(
                        fixedExpenses = fixedList,
                        newTitle = newFixedTitle,
                        newAmount = newFixedAmount,
                        hasError = newFixedError,
                        onTitleChange = { newFixedTitle = it },
                        onAmountChange = {
                            newFixedAmount = MoneyUtils.sanitizeInput(it)
                            newFixedError = false
                        },
                        onAdd = {
                            val amt = MoneyUtils.parseAmount(newFixedAmount)
                            if (amt != null && amt > BigDecimal.ZERO && newFixedTitle.isNotBlank()) {
                                fixedList.add(Pair(newFixedTitle.trim(), amt))
                                newFixedTitle = ""
                                newFixedAmount = ""
                                newFixedError = false
                            } else {
                                newFixedError = true
                            }
                        },
                        onDelete = { idx -> fixedList.removeAt(idx) }
                    )
                    3 -> StepSavings(
                        income = MoneyUtils.parseAmount(incomeInput) ?: BigDecimal("2000.000"),
                        fixedTotal = fixedList.fold(MoneyUtils.ZERO) { acc, p -> acc.add(p.second) },
                        savings = savingsInput,
                        onSavingsChange = { savingsInput = MoneyUtils.sanitizeInput(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepLanguage(
    selectedLang: String,
    onSelect: (String) -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_step_lang),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(28.dp))

        val languages = listOf(
            Triple("ar", "العربية", "Arabic • تونس"),
            Triple("fr", "Français", "French"),
            Triple("en", "English", "English")
        )

        languages.forEach { (code, name, subtitle) ->
            val isSelected = selectedLang == code
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onSelect(code) }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .testTag("lang_option_$code"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIncome(
    income: String,
    isError: Boolean,
    onIncomeChange: (String) -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Wallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_step_income),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_step_income_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = income,
            onValueChange = onIncomeChange,
            label = { Text(stringResource(R.string.monthly_income)) },
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
                .testTag("onboarding_income_input"),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Income Suggestions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("1500", "2000", "2500", "3000").forEach { preset ->
                OutlinedButton(
                    onClick = { onIncomeChange(preset) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("$preset ${stringResource(R.string.currency_symbol)}")
                }
            }
        }
    }
}

@Composable
private fun StepFixedExpenses(
    fixedExpenses: List<Pair<String, BigDecimal>>,
    newTitle: String,
    newAmount: String,
    hasError: Boolean,
    onTitleChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit
) {
    val totalFixed = fixedExpenses.fold(MoneyUtils.ZERO) { acc, p -> acc.add(p.second) }

    Column {
        Text(
            text = stringResource(R.string.onboarding_step_fixed),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${stringResource(R.string.fixed_expenses)}: ${CurrencyUtils.formatCurrency(totalFixed)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Add row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = onTitleChange,
                        placeholder = { Text(stringResource(R.string.fixed_expense_placeholder), fontSize = 13.sp) },
                        modifier = Modifier.weight(1.3f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newAmount,
                        onValueChange = onAmountChange,
                        placeholder = { Text(stringResource(R.string.amount_placeholder)) },
                        suffix = { Text(stringResource(R.string.currency_symbol)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.new_fixed_expense))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(fixedExpenses) { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.first,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = CurrencyUtils.formatCurrency(item.second),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onDelete(index) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepSavings(
    income: BigDecimal,
    fixedTotal: BigDecimal,
    savings: String,
    onSavingsChange: (String) -> Unit
) {
    val parsedSavings = MoneyUtils.parseAmount(savings) ?: BigDecimal.ZERO
    val availableBudget = income.subtract(fixedTotal).subtract(parsedSavings)

    Column {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Savings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_step_savings),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_step_savings_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = savings,
            onValueChange = onSavingsChange,
            label = { Text(stringResource(R.string.planned_savings)) },
            placeholder = { Text(stringResource(R.string.amount_placeholder)) },
            suffix = { Text(stringResource(R.string.currency_symbol), fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_savings_input"),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Savings Suggestions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("100", "200", "300", "500").forEach { preset ->
                OutlinedButton(
                    onClick = { onSavingsChange(preset) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("$preset ${stringResource(R.string.currency_symbol)}")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Live Available Budget Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.available_budget),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = CurrencyUtils.formatCurrency(availableBudget),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (availableBudget >= BigDecimal.ZERO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.budget_formula_note,
                        CurrencyUtils.formatAmount(income),
                        CurrencyUtils.formatAmount(fixedTotal),
                        CurrencyUtils.formatAmount(parsedSavings)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
