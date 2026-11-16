package com.carlens.ui.costs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlens.domain.calc.CostSummary
import com.carlens.domain.calc.CostView
import com.carlens.domain.calc.Units
import com.carlens.domain.model.AppSettings
import com.carlens.domain.model.Expense
import com.carlens.domain.model.ExpenseCategory
import com.carlens.ui.components.AttachmentCountBadge
import com.carlens.ui.components.CarLensCard
import com.carlens.ui.components.CarLensFilterChip
import com.carlens.ui.components.CategoryDonutAndBreakdown
import com.carlens.ui.components.CategoryTag
import com.carlens.ui.components.EmptyStateCard
import com.carlens.ui.components.HeroAccentCard
import com.carlens.ui.components.MonthlySpendChart
import com.carlens.ui.components.SectionHeader
import com.carlens.ui.components.expenseCategoryColor
import com.carlens.ui.components.expenseCategoryIcon
import com.carlens.ui.theme.StatusAmber
import com.carlens.ui.theme.StatusRed
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CostsScreen(
    monthlyCost: CostSummary?,
    allTimeCost: CostSummary?,
    monthlyTrend: List<Pair<YearMonth, Long>>,
    selectedMonth: YearMonth,
    allExpenses: List<Expense>,
    currentView: CostView,
    settings: AppSettings,
    attachmentCounts: Map<String, Int>,
    onSelectMonth: (YearMonth) -> Unit,
    onViewChanged: (CostView) -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onAddExpenseClick: () -> Unit,
    onEditBudgetClick: () -> Unit,
) {
    var showAllTime by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val activeSummary = if (showAllTime) allTimeCost else monthlyCost
    val monthFmt = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US) }

    val visibleExpenses = remember(activeSummary, selectedCategory, searchQuery) {
        val q = searchQuery.trim()
        activeSummary?.expenses.orEmpty().filter { exp ->
            val matchesCat = selectedCategory == null || exp.category == selectedCategory
            val matchesQ = q.isEmpty() ||
                exp.merchant.contains(q, ignoreCase = true) ||
                exp.notes.contains(q, ignoreCase = true) ||
                exp.category.label.contains(q, ignoreCase = true)
            matchesCat && matchesQ
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Costs & Analytics",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${allExpenses.size} total ${if (allExpenses.size == 1) "expense" else "expenses"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onAddExpenseClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Expense")
                }
            }
        }

        // Scope + Cost View Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Month vs All-time segmented pill
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Monthly" to false, "All-time" to true).forEach { (label, isAllTime) ->
                                val selected = showAllTime == isAllTime
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            showAllTime = isAllTime
                                            selectedCategory = null
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Running Costs vs Cash Outflow toggle chips
                    CarLensFilterChip(
                        selected = currentView == CostView.RUNNING_COSTS,
                        onClick = { onViewChanged(CostView.RUNNING_COSTS) },
                        label = "Running"
                    )
                    CarLensFilterChip(
                        selected = currentView == CostView.CASH_OUTFLOW,
                        onClick = { onViewChanged(CostView.CASH_OUTFLOW) },
                        label = "Outflow"
                    )
                }

                if (!showAllTime) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onSelectMonth(selectedMonth.minusMonths(1)) }) {
                                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month")
                            }
                            Text(
                                text = selectedMonth.format(monthFmt),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onSelectMonth(selectedMonth.plusMonths(1)) }) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month")
                            }
                        }
                    }
                }
            }
        }

        // Hero Summary Card
        item {
            val budgetSen = settings.monthlyBudgetSen
            val totalSen = activeSummary?.totalSen ?: 0L
            val txCount = activeSummary?.expenses?.size ?: 0
            val distKm = activeSummary?.distanceKm
            val costPerKm = activeSummary?.costPerKmSen

            HeroAccentCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (showAllTime) {
                                "ALL TIME · ${currentView.label.uppercase()}"
                            } else {
                                "${selectedMonth.format(monthFmt).uppercase()} · ${currentView.label.uppercase()}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = Units.formatRm(totalSen, settings.hideAmounts),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (!showAllTime) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onEditBudgetClick),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = "Edit budget",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (budgetSen != null && budgetSen > 0) "Budget" else "Set budget",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$txCount ${if (txCount == 1) "transaction" else "transactions"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (distKm != null && distKm > 0) {
                        Text(
                            text = "· ${Units.formatKm(distKm)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (costPerKm != null) {
                        Text(
                            text = "· ${Units.formatRmPerKm(costPerKm, settings.hideAmounts)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (!showAllTime && budgetSen != null && budgetSen > 0L) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val ratio = (totalSen.toFloat() / budgetSen.toFloat()).coerceIn(0f, 1.5f)
                    val barColor = when {
                        ratio > 1f -> StatusRed
                        ratio >= 0.8f -> StatusAmber
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Budget ${Units.formatRmCompact(budgetSen, settings.hideAmounts)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val rem = budgetSen - totalSen
                        Text(
                            text = if (rem >= 0) {
                                "${Units.formatRmCompact(rem, settings.hideAmounts)} remaining"
                            } else {
                                "${Units.formatRmCompact(-rem, settings.hideAmounts)} over budget"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = barColor
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { ratio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
            }
        }

        // 6-Month Spend Trend Chart
        if (monthlyTrend.isNotEmpty()) {
            item {
                CarLensCard {
                    SectionHeader(
                        title = "6-Month Trend",
                        subtitle = currentView.label
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MonthlySpendChart(
                        trend = monthlyTrend,
                        selectedMonth = selectedMonth,
                        budgetSen = settings.monthlyBudgetSen,
                        hideAmounts = settings.hideAmounts,
                        onSelectMonth = { month ->
                            showAllTime = false
                            selectedCategory = null
                            onSelectMonth(month)
                        }
                    )
                }
            }
        }

        // Interactive Category Donut & Breakdown
        val categories = activeSummary?.byCategory.orEmpty()
        if (categories.isNotEmpty()) {
            item {
                CarLensCard {
                    SectionHeader(
                        title = "Category Breakdown",
                        subtitle = "Tap any category to filter transactions below",
                        trailing = if (selectedCategory != null) {
                            {
                                TextButton(onClick = { selectedCategory = null }) {
                                    Text("Clear")
                                }
                            }
                        } else null
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    CategoryDonutAndBreakdown(
                        byCategory = categories,
                        totalSen = activeSummary?.totalSen ?: 0L,
                        selectedCategory = selectedCategory,
                        hideAmounts = settings.hideAmounts,
                        onSelectCategory = { cat -> selectedCategory = cat }
                    )
                }
            }
        }

        // Transactions Section Header & Search
        item {
            SectionHeader(
                title = selectedCategory?.let { "${it.label} Transactions" } ?: "Transactions",
                subtitle = "${visibleExpenses.size} shown"
            )
        }

        if (activeSummary?.expenses.orEmpty().isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search merchant, category, or notes") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (visibleExpenses.isEmpty()) {
            val periodEmpty = activeSummary?.expenses.orEmpty().isEmpty()
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = if (periodEmpty) "No expenses in this period" else "No matching expenses",
                    subtitle = if (periodEmpty) {
                        "Fuel fill-ups and service records appear here automatically alongside tolls, parking, and renewals."
                    } else {
                        "Try clearing your category filter or search query."
                    },
                    actionLabel = if (periodEmpty) "Add expense" else "Show all",
                    onAction = if (periodEmpty) {
                        onAddExpenseClick
                    } else {
                        {
                            selectedCategory = null
                            searchQuery = ""
                        }
                    }
                )
            }
        } else {
            items(visibleExpenses, key = { it.id }) { expense ->
                ExpenseCard(
                    expense = expense,
                    attachmentCount = attachmentCounts[expense.id] ?: 0,
                    hideAmounts = settings.hideAmounts,
                    dateText = expense.time.localDate.format(dateFmt),
                    onClick = { onEditExpense(expense) },
                    onDelete = { onDeleteExpense(expense) }
                )
            }
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    attachmentCount: Int,
    hideAmounts: Boolean,
    dateText: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val catColor = expenseCategoryColor(expense.category)

    CarLensCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = catColor.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = expenseCategoryIcon(expense.category),
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = expense.merchant.ifBlank { expense.category.label },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CategoryTag(text = expense.category.label, color = catColor)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = Units.formatRm(expense.amountSen, hideAmounts),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (expense.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = expense.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttachmentCountBadge(count = attachmentCount)
                if (expense.isLinked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (expense.fuelEntryId != null) {
                                "Linked fuel entry · tap to edit"
                            } else {
                                "Linked service · tap to edit"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Tap card to edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!expense.isLinked) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete expense",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
