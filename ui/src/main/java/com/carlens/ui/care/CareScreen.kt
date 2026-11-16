package com.carlens.ui.care

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.NotificationsPaused
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlens.domain.calc.DueState
import com.carlens.domain.calc.MaintenanceStatus
import com.carlens.domain.calc.Units
import com.carlens.domain.model.AppSettings
import com.carlens.domain.model.ExpenseCategory
import com.carlens.domain.model.MaintenanceRule
import com.carlens.domain.model.ServiceRecord
import com.carlens.ui.components.AttachmentCountBadge
import com.carlens.ui.components.CarLensCard
import com.carlens.ui.components.CarLensFilterChip
import com.carlens.ui.components.CategoryTag
import com.carlens.ui.components.EmptyStateCard
import com.carlens.ui.components.StatusPillDot
import com.carlens.ui.components.UrgencyStripCard
import com.carlens.ui.components.dueStateColor
import com.carlens.ui.components.expenseCategoryColor
import com.carlens.ui.theme.StatusAmber
import com.carlens.ui.theme.StatusGreen
import com.carlens.ui.theme.StatusMuted
import com.carlens.ui.theme.StatusRed
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class UpcomingFilter(val label: String) {
    ALL("All"),
    ATTENTION("Needs attention"),
    HEALTHY("Up to date")
}

@Composable
fun CareScreen(
    maintenanceStatuses: List<MaintenanceStatus>,
    serviceRecords: List<ServiceRecord>,
    attachmentCounts: Map<String, Int>,
    settings: AppSettings,
    onMarkCompleted: (MaintenanceRule) -> Unit,
    onSnoozeRule: (MaintenanceRule, Long?) -> Unit,
    onAddRuleClick: () -> Unit,
    onEditRule: (MaintenanceRule) -> Unit,
    onEditServiceRecord: (ServiceRecord) -> Unit,
    onDeleteServiceRecord: (ServiceRecord) -> Unit,
    onAddServiceClick: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Upcoming, 1 = History
    var upcomingFilter by remember { mutableStateOf(UpcomingFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }

    val overdueCount = remember(maintenanceStatuses) {
        maintenanceStatuses.count { it.state == DueState.OVERDUE }
    }
    val dueSoonCount = remember(maintenanceStatuses) {
        maintenanceStatuses.count { it.state == DueState.DUE || it.state == DueState.DUE_SOON }
    }
    val healthyCount = remember(maintenanceStatuses) {
        maintenanceStatuses.count { it.state == DueState.UP_TO_DATE }
    }

    val filteredStatuses = remember(maintenanceStatuses, upcomingFilter) {
        when (upcomingFilter) {
            UpcomingFilter.ALL -> maintenanceStatuses
            UpcomingFilter.ATTENTION -> maintenanceStatuses.filter {
                it.state == DueState.OVERDUE || it.state == DueState.DUE || it.state == DueState.DUE_SOON
            }
            UpcomingFilter.HEALTHY -> maintenanceStatuses.filter { it.state == DueState.UP_TO_DATE }
        }
    }

    val availableCategories = remember(serviceRecords) {
        serviceRecords.map { it.category }.distinct().sortedBy { it.label }
    }

    val filteredRecords = remember(serviceRecords, searchQuery, selectedCategory) {
        val q = searchQuery.trim()
        serviceRecords.filter { rec ->
            val matchesQuery = q.isEmpty() ||
                rec.title.contains(q, ignoreCase = true) ||
                rec.workshop.contains(q, ignoreCase = true) ||
                rec.notes.contains(q, ignoreCase = true) ||
                rec.items.any {
                    it.task.contains(q, ignoreCase = true) || it.part.contains(q, ignoreCase = true)
                }
            val matchesCat = selectedCategory == null || rec.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Header + Contextual Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Care & Service",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${maintenanceStatuses.size} reminders · ${serviceRecords.size} records",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                onClick = if (selectedTab == 0) onAddRuleClick else onAddServiceClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (selectedTab == 0) "Reminder" else "Service")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pill Segmented Switcher
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Upcoming" to 0, "Service history" to 1).forEach { (label, idx) ->
                    val selected = selectedTab == idx
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .clickable { selectedTab = idx },
                        shape = RoundedCornerShape(11.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        },
                        border = if (selected) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        } else null
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Status Overview Strip
                if (maintenanceStatuses.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UrgencySummaryChip(
                                count = overdueCount,
                                label = "Overdue",
                                color = StatusRed,
                                modifier = Modifier.weight(1f)
                            )
                            UrgencySummaryChip(
                                count = dueSoonCount,
                                label = "Due soon",
                                color = StatusAmber,
                                modifier = Modifier.weight(1f)
                            )
                            UrgencySummaryChip(
                                count = healthyCount,
                                label = "Healthy",
                                color = StatusGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(UpcomingFilter.entries) { filter ->
                                CarLensFilterChip(
                                    selected = upcomingFilter == filter,
                                    onClick = { upcomingFilter = filter },
                                    label = when (filter) {
                                        UpcomingFilter.ALL -> "All (${maintenanceStatuses.size})"
                                        UpcomingFilter.ATTENTION -> "Attention (${overdueCount + dueSoonCount})"
                                        UpcomingFilter.HEALTHY -> "Up to date ($healthyCount)"
                                    }
                                )
                            }
                        }
                    }
                }

                if (filteredStatuses.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Build,
                            title = if (maintenanceStatuses.isEmpty()) {
                                "No maintenance rules yet"
                            } else {
                                "No reminders in this filter"
                            },
                            subtitle = if (maintenanceStatuses.isEmpty()) {
                                "Track oil changes, ATF, brake fluid, and tyre rotations by date or mileage — whichever comes first."
                            } else {
                                "Switch filter chips above to view all scheduled maintenance tasks."
                            },
                            actionLabel = if (maintenanceStatuses.isEmpty()) "Add maintenance rule" else "Show all",
                            onAction = if (maintenanceStatuses.isEmpty()) {
                                onAddRuleClick
                            } else {
                                { upcomingFilter = UpcomingFilter.ALL }
                            }
                        )
                    }
                } else {
                    items(filteredStatuses, key = { it.rule.id }) { status ->
                        MaintenanceStatusCard(
                            status = status,
                            dateFmt = dateFmt,
                            onMarkCompleted = { onMarkCompleted(status.rule) },
                            onSnooze = { days -> onSnoozeRule(status.rule, days) },
                            onEditRule = { onEditRule(status.rule) }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                if (serviceRecords.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search workshop, tasks, parts, or notes") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
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

                            if (availableCategories.size > 1) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        CarLensFilterChip(
                                            selected = selectedCategory == null,
                                            onClick = { selectedCategory = null },
                                            label = "All (${serviceRecords.size})"
                                        )
                                    }
                                    items(availableCategories) { cat ->
                                        CarLensFilterChip(
                                            selected = selectedCategory == cat,
                                            onClick = {
                                                selectedCategory = if (selectedCategory == cat) null else cat
                                            },
                                            label = cat.label,
                                            leadingDotColor = expenseCategoryColor(cat)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (filteredRecords.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Handyman,
                            title = if (serviceRecords.isEmpty()) "No service records yet" else "No matching records",
                            subtitle = if (serviceRecords.isEmpty()) {
                                "Log workshop visits, itemized parts, and receipt photos — costs sync automatically with your expenses."
                            } else {
                                "Try clearing your search or category filter."
                            },
                            actionLabel = if (serviceRecords.isEmpty()) "Record service" else "Clear filters",
                            onAction = if (serviceRecords.isEmpty()) {
                                onAddServiceClick
                            } else {
                                {
                                    searchQuery = ""
                                    selectedCategory = null
                                }
                            }
                        )
                    }
                } else {
                    items(filteredRecords, key = { it.id }) { record ->
                        ServiceRecordCard(
                            record = record,
                            attachmentCount = attachmentCounts[record.id] ?: 0,
                            hideAmounts = settings.hideAmounts,
                            dateText = record.time.localDate.format(dateFmt),
                            onClick = { onEditServiceRecord(record) },
                            onDelete = { onDeleteServiceRecord(record) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UrgencySummaryChip(
    count: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MaintenanceStatusCard(
    status: MaintenanceStatus,
    dateFmt: DateTimeFormatter,
    onMarkCompleted: () -> Unit,
    onSnooze: (Long?) -> Unit,
    onEditRule: () -> Unit,
) {
    val accent = dueStateColor(status.state)
    val isUrgent = status.state == DueState.OVERDUE || status.state == DueState.DUE || status.state == DueState.DUE_SOON
    var snoozeExpanded by remember { mutableStateOf(false) }

    UrgencyStripCard(
        accentColor = accent,
        highlighted = isUrgent,
        onClick = onEditRule
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = status.rule.task,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val intervalDesc = buildList {
                    status.rule.intervalMonths?.let { add("Every $it mo") }
                    status.rule.intervalKm?.let { add("Every ${Units.formatKm(it)}") }
                }.joinToString(" · ").ifEmpty { "One-off / custom" }
                Text(
                    text = intervalDesc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusPillDot(text = status.state.label, color = accent)
                if (status.snoozed) {
                    val snoozeDate = status.rule.snoozedUntil
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsPaused,
                            contentDescription = null,
                            tint = StatusMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (snoozeDate != null) "Until ${snoozeDate.format(dateFmt)}" else "Snoozed",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Due summary box
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = status.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isUrgent) accent else MaterialTheme.colorScheme.onSurface
                    )
                    val lastKm = status.lastDoneKm
                    Text(
                        text = buildString {
                            append("Last ${status.lastDoneDate.format(dateFmt)}")
                            if (lastKm != null) append(" · ${Units.formatKm(lastKm)}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val progress = status.progressFraction
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = accent,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }

                val estKmDate = status.estimatedKmDueDate
                if (estKmDate != null || status.mileageStale) {
                    val note = when {
                        status.mileageStale -> "Update odometer for accurate mileage estimate"
                        estKmDate != null -> "Projected mileage due ~${estKmDate.format(dateFmt)}"
                        else -> ""
                    }
                    if (note.isNotEmpty()) {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (status.mileageStale) StatusAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isUrgent) {
                    Button(
                        onClick = onMarkCompleted,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark completed", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    FilledTonalButton(
                        onClick = onMarkCompleted,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark completed", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Box {
                    OutlinedButton(
                        onClick = { snoozeExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Snooze", style = MaterialTheme.typography.labelMedium)
                    }
                    DropdownMenu(
                        expanded = snoozeExpanded,
                        onDismissRequest = { snoozeExpanded = false }
                    ) {
                        if (status.snoozed) {
                            DropdownMenuItem(
                                text = { Text("Unsnooze reminder") },
                                onClick = {
                                    snoozeExpanded = false
                                    onSnooze(null)
                                }
                            )
                        }
                        listOf(7L to "Snooze 7 days", 14L to "Snooze 14 days", 30L to "Snooze 30 days")
                            .forEach { (days, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        snoozeExpanded = false
                                        onSnooze(days)
                                    }
                                )
                            }
                    }
                }
            }

            IconButton(
                onClick = onEditRule,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit rule",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ServiceRecordCard(
    record: ServiceRecord,
    attachmentCount: Int,
    hideAmounts: Boolean,
    dateText: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val catColor = expenseCategoryColor(record.category)
    val odoKm = record.odometerKm

    CarLensCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = catColor.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Build,
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier
                            .padding(9.dp)
                            .size(18.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = record.title.ifBlank { record.category.label },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildList {
                            add(dateText)
                            if (odoKm != null) add(Units.formatKm(odoKm))
                            if (record.workshop.isNotBlank()) add(record.workshop)
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = Units.formatRm(record.totalSen, hideAmounts),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                CategoryTag(text = record.category.label, color = catColor)
            }
        }

        if (record.items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    record.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = buildString {
                                        append(item.task)
                                        if (item.part.isNotBlank()) append(" (${item.part})")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val itemCost = item.costSen
                            if (itemCost != null) {
                                Text(
                                    text = Units.formatRm(itemCost, hideAmounts),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (record.unallocatedSen != 0L && record.items.any { it.costSen != null }) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Labour / other unallocated",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Units.formatRm(record.unallocatedSen, hideAmounts),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (record.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = record.notes,
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
                Text(
                    text = "Tap card to edit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete service record",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
