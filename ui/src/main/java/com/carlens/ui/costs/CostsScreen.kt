package com.carlens.ui.costs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carlens.domain.calc.CostSummary
import com.carlens.domain.calc.CostView
import com.carlens.domain.calc.Units
import com.carlens.domain.model.AppSettings
import com.carlens.domain.model.Expense
import com.carlens.domain.model.ExpenseCategory
import com.carlens.ui.components.AttachmentCountBadge
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CostsScreen(
    monthlyCost: CostSummary?,
    monthlyTrend: List<Pair<YearMonth, Long>> = emptyList(),
    selectedMonth: YearMonth = YearMonth.now(),
    allExpenses: List<Expense>,
    currentView: CostView,
    settings: AppSettings,
    attachmentCounts: Map<String, Int> = emptyMap(),
    onSelectMonth: (YearMonth) -> Unit = {},
    onViewChanged: (CostView) -> Unit,
    onEditExpense: (Expense) -> Unit = {},
    onDeleteExpense: (Expense) -> Unit,
    onAddExpenseClick: () -> Unit,
    onEditBudgetClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy") }
    val shortMonthFormatter = remember { DateTimeFormatter.ofPattern("MMM") }
    val currentYearMonth = remember { YearMonth.now() }

    val filteredExpenses = remember(allExpenses, searchQuery, selectedCategoryFilter) {
        allExpenses.filter { exp ->
            val matchesCategory = selectedCategoryFilter == null || exp.category == selectedCategoryFilter
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isBlank() ||
                exp.merchant.lowercase().contains(q) ||
                exp.notes.lowercase().contains(q) ||
                exp.category.label.lowercase().contains(q) ||
                exp.time.localDate.toString().contains(q)
            matchesCategory && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Costs & Spending",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedButton(onClick = onAddExpenseClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Expense")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Month Navigator + View Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onSelectMonth(selectedMonth.minusMonths(1)) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }
                    Text(
                        text = selectedMonth.format(monthFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { onSelectMonth(selectedMonth.plusMonths(1)) },
                        enabled = selectedMonth.isBefore(currentYearMonth)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                    if (selectedMonth != currentYearMonth) {
                        TextButton(onClick = { onSelectMonth(currentYearMonth) }) {
                            Text("Today")
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = currentView == CostView.RUNNING_COSTS,
                        onClick = { onViewChanged(CostView.RUNNING_COSTS) },
                        label = { Text("Running") }
                    )
                    FilterChip(
                        selected = currentView == CostView.CASH_OUTFLOW,
                        onClick = { onViewChanged(CostView.CASH_OUTFLOW) },
                        label = { Text("Outflow") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Monthly Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedMonth.format(monthFormatter)} · ${currentView.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onEditBudgetClick) {
                            Text(if ((settings.monthlyBudgetSen ?: 0L) > 0L) "Edit Budget" else "Set Budget")
                        }
                    }
                    Text(
                        text = Units.formatRm(monthlyCost?.totalSen ?: 0L, settings.hideAmounts),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val budgetSen = settings.monthlyBudgetSen
                    if (budgetSen != null && budgetSen > 0L) {
                        val spentSen = monthlyCost?.totalSen ?: 0L
                        val ratio = (spentSen.toFloat() / budgetSen.toFloat()).coerceIn(0f, 1f)
                        val isOverBudget = spentSen > budgetSen
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isOverBudget) "Over budget by ${Units.formatRm(spentSen - budgetSen, settings.hideAmounts)}"
                                else "${Units.formatRm(budgetSen - spentSen, settings.hideAmounts)} remaining of ${Units.formatRm(budgetSen, settings.hideAmounts)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isOverBudget) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "${((spentSen * 100) / budgetSen)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Cost per km",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = monthlyCost?.costPerKmSen?.let {
                                Units.formatRmPerKm(it, settings.hideAmounts)
                            } ?: "Update mileage to calculate",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 6-Month Spending Trend Bar Chart
            if (monthlyTrend.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "6-Month Spending Trend",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val maxSen = monthlyTrend.maxOf { it.second }.coerceAtLeast(100L)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            monthlyTrend.forEach { (ym, totalSen) ->
                                val isSelected = ym == selectedMonth
                                val fraction = (totalSen.toFloat() / maxSen.toFloat()).coerceIn(0.06f, 1f)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { onSelectMonth(ym) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Text(
                                        text = if (settings.hideAmounts) "••" else "${totalSen / 100}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.65f)
                                            .fillMaxHeight(fraction * 0.65f)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ym.format(shortMonthFormatter),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Breakdown Card
            if (monthlyCost != null && monthlyCost.byCategory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category Breakdown (Tap to filter)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (selectedCategoryFilter != null) {
                                TextButton(onClick = { selectedCategoryFilter = null }) {
                                    Text("Clear Filter")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        val total = monthlyCost.totalSen.coerceAtLeast(1L)
                        monthlyCost.byCategory.forEach { catTotal ->
                            val fraction = catTotal.totalSen.toFloat() / total
                            val isSelectedCat = selectedCategoryFilter == catTotal.category
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelectedCat) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable {
                                        selectedCategoryFilter = if (isSelectedCat) null else catTotal.category
                                    }
                                    .padding(vertical = 6.dp, horizontal = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${catTotal.category.label} (${catTotal.count})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelectedCat) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = Units.formatRm(catTotal.totalSen, settings.hideAmounts),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Transactions (${filteredExpenses.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (allExpenses.size > 3 || searchQuery.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search merchant, category, notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (filteredExpenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (allExpenses.isEmpty()) "No expenses recorded for this vehicle."
                            else "No matching transactions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredExpenses, key = { it.id }) { expense ->
                val attachKey = expense.fuelEntryId ?: expense.serviceRecordId ?: expense.id
                ExpenseCard(
                    expense = expense,
                    settings = settings,
                    attachmentCount = attachmentCounts[attachKey] ?: 0,
                    onEdit = { onEditExpense(expense) },
                    onDelete = { onDeleteExpense(expense) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    settings: AppSettings,
    attachmentCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.category.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (expense.isLinked) {
                        Spacer(modifier = Modifier.size(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (expense.fuelEntryId != null) "Fuel" else "Service",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (attachmentCount > 0) {
                        Spacer(modifier = Modifier.size(6.dp))
                        AttachmentCountBadge(count = attachmentCount)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${expense.time.localDate}${if (expense.merchant.isNotBlank()) " · ${expense.merchant}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (expense.notes.isNotBlank()) {
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Units.formatRm(expense.amountSen, settings.hideAmounts),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (!expense.isLinked) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
