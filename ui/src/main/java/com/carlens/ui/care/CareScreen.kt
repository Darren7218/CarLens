package com.carlens.ui.care

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carlens.domain.calc.MaintenanceStatus
import com.carlens.domain.calc.Units
import com.carlens.domain.model.AppSettings
import com.carlens.domain.model.MaintenanceRule
import com.carlens.domain.model.ServiceRecord
import com.carlens.ui.components.AttachmentCountBadge
import com.carlens.ui.components.StatusBadge

@Composable
fun CareScreen(
    maintenanceStatuses: List<MaintenanceStatus>,
    serviceRecords: List<ServiceRecord>,
    attachmentCounts: Map<String, Int> = emptyMap(),
    settings: AppSettings,
    onMarkCompleted: (MaintenanceRule) -> Unit,
    onSnoozeRule: (MaintenanceRule, days: Long?) -> Unit,
    onAddRuleClick: () -> Unit,
    onEditRule: (MaintenanceRule) -> Unit,
    onEditServiceRecord: (ServiceRecord) -> Unit,
    onDeleteServiceRecord: (ServiceRecord) -> Unit,
    onAddServiceClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Upcoming (${maintenanceStatuses.size})", "Service History (${serviceRecords.size})")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vehicle Care",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedButton(
                    onClick = if (selectedTab == 0) onAddRuleClick else onAddServiceClick
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (selectedTab == 0) "Add Schedule" else "Add Service")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
            UpcomingMaintenanceTab(
                statuses = maintenanceStatuses,
                onMarkCompleted = onMarkCompleted,
                onSnoozeRule = onSnoozeRule,
                onEditRule = onEditRule,
                onAddRuleClick = onAddRuleClick
            )
        } else {
            ServiceHistoryTab(
                records = serviceRecords,
                attachmentCounts = attachmentCounts,
                settings = settings,
                onEdit = onEditServiceRecord,
                onDelete = onDeleteServiceRecord
            )
        }
    }
}

@Composable
private fun UpcomingMaintenanceTab(
    statuses: List<MaintenanceStatus>,
    onMarkCompleted: (MaintenanceRule) -> Unit,
    onSnoozeRule: (MaintenanceRule, Long?) -> Unit,
    onEditRule: (MaintenanceRule) -> Unit,
    onAddRuleClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (statuses.isEmpty()) {
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
                            text = "No maintenance schedule configured for this vehicle.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onAddRuleClick) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Maintenance Rule")
                        }
                    }
                }
            }
        } else {
            items(statuses, key = { it.rule.id }) { status ->
                MaintenanceStatusCard(
                    status = status,
                    onMarkCompleted = { onMarkCompleted(status.rule) },
                    onSnooze = { days -> onSnoozeRule(status.rule, days) },
                    onEdit = { onEditRule(status.rule) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun MaintenanceStatusCard(
    status: MaintenanceStatus,
    onMarkCompleted: () -> Unit,
    onSnooze: (Long?) -> Unit,
    onEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.rule.task,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = status.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(state = status.state)

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Rule / Interval") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Snooze 7 days") },
                            onClick = {
                                showMenu = false
                                onSnooze(7L)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Snooze 30 days") },
                            onClick = {
                                showMenu = false
                                onSnooze(30L)
                            }
                        )
                        if (status.snoozed) {
                            DropdownMenuItem(
                                text = { Text("Clear Snooze") },
                                onClick = {
                                    showMenu = false
                                    onSnooze(null)
                                }
                            )
                        }
                    }
                }
            }

            val lastDoneSummary = buildList {
                add("Last baseline: ${status.lastDoneDate}")
                status.lastDoneKm?.let { add(Units.formatKm(it)) }
                if (status.snoozed && status.rule.snoozedUntil != null) {
                    add("Snoozed until ${status.rule.snoozedUntil}")
                }
            }.joinToString(" · ")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = lastDoneSummary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (status.rule.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = status.rule.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (status.estimatedKmDueDate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Forecasted date: ~${status.estimatedKmDueDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onMarkCompleted,
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mark Completed")
                }
            }
        }
    }
}

@Composable
private fun ServiceHistoryTab(
    records: List<ServiceRecord>,
    attachmentCounts: Map<String, Int>,
    settings: AppSettings,
    onEdit: (ServiceRecord) -> Unit,
    onDelete: (ServiceRecord) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredRecords = remember(records, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) records
        else records.filter { r ->
            r.title.contains(q, ignoreCase = true) ||
                r.workshop.contains(q, ignoreCase = true) ||
                r.notes.contains(q, ignoreCase = true) ||
                r.time.localDate.toString().contains(q) ||
                r.items.any { it.task.contains(q, ignoreCase = true) || it.part.contains(q, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (records.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search task, part, workshop, or date…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        if (filteredRecords.isEmpty()) {
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
                            text = if (records.isEmpty()) "No service records saved yet."
                            else "No service records match your search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredRecords, key = { it.id }) { record ->
                ServiceRecordCard(
                    record = record,
                    attachmentCount = attachmentCounts[record.id] ?: 0,
                    settings = settings,
                    onEdit = { onEdit(record) },
                    onDelete = { onDelete(record) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ServiceRecordCard(
    record: ServiceRecord,
    attachmentCount: Int,
    settings: AppSettings,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.title.ifBlank { "Service Record" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (attachmentCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            AttachmentCountBadge(count = attachmentCount)
                        }
                    }
                    Text(
                        text = "${record.time.localDate} · ${record.category.label} · ${record.workshop.ifBlank { "Independent Workshop" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Units.formatRm(record.totalSen, settings.hideAmounts),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
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

            if (record.odometerKm != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Odometer: ${Units.formatKm(record.odometerKm!!)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Itemized parts & tasks
            if (record.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                record.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• ${item.task}${if (item.part.isNotBlank()) " (${item.part})" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.costSen != null) {
                            Text(
                                text = Units.formatRm(item.costSen!!, settings.hideAmounts),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (record.allocatedSen > 0 && record.unallocatedSen > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Unallocated (labour / tax / misc)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Units.formatRm(record.unallocatedSen, settings.hideAmounts),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
