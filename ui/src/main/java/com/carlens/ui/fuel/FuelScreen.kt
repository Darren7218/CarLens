package com.carlens.ui.fuel

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlens.domain.calc.EconomyInterval
import com.carlens.domain.calc.FuelEconomyReport
import com.carlens.domain.calc.TrendDirection
import com.carlens.domain.calc.Units
import com.carlens.domain.model.AppSettings
import com.carlens.domain.model.FuelEntry
import com.carlens.domain.model.TankStatus
import com.carlens.ui.components.AttachmentCountBadge
import com.carlens.ui.components.CarLensCard
import com.carlens.ui.components.CarLensFilterChip
import com.carlens.ui.components.CategoryTag
import com.carlens.ui.components.EmptyStateCard
import com.carlens.ui.components.FuelEconomyChart
import com.carlens.ui.components.HeroAccentCard
import com.carlens.ui.components.SectionHeader
import com.carlens.ui.components.StatusPillDot
import com.carlens.ui.components.UrgencyStripCard
import com.carlens.ui.theme.StatusAmber
import com.carlens.ui.theme.StatusGreen
import com.carlens.ui.theme.StatusMuted
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FuelScreen(
    fuelEntries: List<FuelEntry>,
    fuelEconomy: FuelEconomyReport,
    settings: AppSettings,
    attachmentCounts: Map<String, Int>,
    onEditEntry: (FuelEntry) -> Unit,
    onDeleteEntry: (FuelEntry) -> Unit,
    onAddFuelClick: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFuelType by remember { mutableStateOf<String?>(null) }
    var selectedStation by remember { mutableStateOf<String?>(null) }

    val availableFuelTypes = remember(fuelEntries) {
        fuelEntries.map { it.fuelType.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    val availableStations = remember(fuelEntries) {
        fuelEntries.map { it.station.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    val anomalyIds = remember(fuelEconomy.anomalies) {
        fuelEconomy.anomalies.map { it.entryId }.toSet()
    }

    val filteredEntries = remember(fuelEntries, searchQuery, selectedFuelType, selectedStation) {
        fuelEntries.filter { entry ->
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                entry.station.contains(q, ignoreCase = true) ||
                entry.fuelType.contains(q, ignoreCase = true) ||
                entry.notes.contains(q, ignoreCase = true) ||
                entry.odometerKm.toString().contains(q)
            val matchesFuel = selectedFuelType == null || entry.fuelType.equals(selectedFuelType, ignoreCase = true)
            val matchesStation = selectedStation == null || entry.station.equals(selectedStation, ignoreCase = true)
            matchesQuery && matchesFuel && matchesStation
        }
    }

    val dateTimeFmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Fuel & Economy",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${fuelEntries.size} ${if (fuelEntries.size == 1) "fill-up" else "fill-ups"} recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onAddFuelClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add fuel")
                }
            }
        }

        // Overall Fuel Economy Hero Card
        item {
            val overallKmPerL = fuelEconomy.overallKmPerL
            val overallL100 = fuelEconomy.overallLPer100
            val costPerKm = fuelEconomy.costPerKmSen
            val best = fuelEconomy.bestInterval
            val trend = fuelEconomy.trend

            HeroAccentCard {
                if (overallKmPerL != null && overallL100 != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "OVERALL EFFICIENCY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = Units.formatKmPerL(overallKmPerL),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "km/L",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                            Text(
                                text = "${Units.formatLPer100(overallL100)} L/100 km",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (costPerKm != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Fuel cost/km",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = Units.formatRmPerKm(costPerKm, settings.hideAmounts),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FuelMetricMiniTile(
                            label = "Best",
                            value = best?.let { "${Units.formatKmPerL(it.kmPerL)} km/L" } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        FuelMetricMiniTile(
                            label = "Tracked dist",
                            value = Units.formatKm(fuelEconomy.totalValidDistanceKm),
                            modifier = Modifier.weight(1f)
                        )
                        FuelMetricMiniTile(
                            label = "Fuel burned",
                            value = Units.formatLitres(fuelEconomy.totalValidVolumeMl),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (trend != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val trendColor = when (trend.direction) {
                            TrendDirection.DECREASED -> StatusGreen
                            TrendDirection.INCREASED -> StatusAmber
                            TrendDirection.STABLE -> MaterialTheme.colorScheme.primary
                        }
                        Text(
                            text = trend.message,
                            style = MaterialTheme.typography.labelMedium,
                            color = trendColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "FULL-TO-FULL TRACKING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Need 2 Full Tank Fill-ups",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "The first full fill-up sets your baseline. Partial fill-ups between two full tanks are combined automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Interactive Economy Curve + Text Summary
        item {
            CarLensCard {
                SectionHeader(
                    title = "Economy History",
                    subtitle = "Full-to-full intervals"
                )
                Spacer(modifier = Modifier.height(12.dp))
                FuelEconomyChart(
                    intervals = fuelEconomy.intervals,
                    overallKmPerL = fuelEconomy.overallKmPerL,
                    hideAmounts = settings.hideAmounts,
                    onInspectClosingEntry = { entryId ->
                        fuelEntries.firstOrNull { it.id == entryId }?.let(onEditEntry)
                    }
                )
            }
        }

        // Anomaly Warnings
        if (fuelEconomy.anomalies.isNotEmpty()) {
            item {
                UrgencyStripCard(accentColor = StatusAmber) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = StatusAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Check ${fuelEconomy.anomalies.size} fill-up reading${if (fuelEconomy.anomalies.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    fuelEconomy.anomalies.take(3).forEach { anomaly ->
                        Text(
                            text = "• ${anomaly.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Timeline Section Header & Filters
        if (fuelEntries.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Fill-up Timeline",
                    subtitle = "Tap any record to inspect or edit"
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search station, fuel type, odometer, or notes") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null)
                        },
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

                    if (availableFuelTypes.isNotEmpty() || availableStations.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                CarLensFilterChip(
                                    selected = selectedFuelType == null && selectedStation == null,
                                    onClick = {
                                        selectedFuelType = null
                                        selectedStation = null
                                    },
                                    label = "All (${fuelEntries.size})"
                                )
                            }
                            items(availableFuelTypes) { type ->
                                CarLensFilterChip(
                                    selected = selectedFuelType == type,
                                    onClick = {
                                        selectedFuelType = if (selectedFuelType == type) null else type
                                    },
                                    label = type
                                )
                            }
                            items(availableStations) { station ->
                                CarLensFilterChip(
                                    selected = selectedStation == station,
                                    onClick = {
                                        selectedStation = if (selectedStation == station) null else station
                                    },
                                    label = station
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filteredEntries.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.LocalGasStation,
                    title = if (fuelEntries.isEmpty()) "No fill-ups recorded yet" else "No matching fill-ups",
                    subtitle = if (fuelEntries.isEmpty()) {
                        "Record your first fill-up in seconds — CarLens remembers your last station and fuel grade."
                    } else {
                        "Try clearing your search or station/fuel filters."
                    },
                    actionLabel = if (fuelEntries.isEmpty()) "Add first fill-up" else "Clear filters",
                    onAction = if (fuelEntries.isEmpty()) {
                        onAddFuelClick
                    } else {
                        {
                            searchQuery = ""
                            selectedFuelType = null
                            selectedStation = null
                        }
                    }
                )
            }
        } else {
            items(filteredEntries, key = { it.id }) { entry ->
                FuelEntryCard(
                    entry = entry,
                    interval = fuelEconomy.economyByClosingEntry[entry.id],
                    isAnomaly = entry.id in anomalyIds,
                    attachmentCount = attachmentCounts[entry.id] ?: 0,
                    hideAmounts = settings.hideAmounts,
                    dateTimeText = entry.time.localDateTime.format(dateTimeFmt),
                    onClick = { onEditEntry(entry) },
                    onDelete = { onDeleteEntry(entry) }
                )
            }
        }
    }
}

@Composable
private fun FuelMetricMiniTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FuelEntryCard(
    entry: FuelEntry,
    interval: EconomyInterval?,
    isAnomaly: Boolean,
    attachmentCount: Int,
    hideAmounts: Boolean,
    dateTimeText: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val totalSen = entry.totalSen
    val unitPriceMilli = entry.unitPriceMilliSen
    val derivedUnitPrice = unitPriceMilli ?: if (totalSen != null && entry.volumeMl > 0) {
        Units.unitPriceMilliSen(totalSen, entry.volumeMl)
    } else null

    CarLensCard(
        onClick = onClick,
        borderColor = if (isAnomaly) StatusAmber.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = dateTimeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = entry.station.ifBlank { "Petrol station" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.fuelType.isNotBlank()) {
                        CategoryTag(
                            text = entry.fuelType,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (interval != null) {
                    StatusPillDot(
                        text = "${Units.formatKmPerL(interval.kmPerL)} km/L",
                        color = if (interval.suspicious) StatusAmber else MaterialTheme.colorScheme.primary
                    )
                }
                CategoryTag(
                    text = when {
                        entry.missedPrevious -> "Missed prev"
                        entry.tankStatus == TankStatus.PARTIAL -> "Partial"
                        else -> "Full tank"
                    },
                    color = when {
                        entry.missedPrevious -> StatusAmber
                        entry.tankStatus == TankStatus.PARTIAL -> StatusMuted
                        else -> StatusGreen
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Metrics Strip
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FuelCellMetric(
                    label = "Odometer",
                    value = Units.formatKm(entry.odometerKm),
                    modifier = Modifier.weight(1.1f)
                )
                FuelCellMetric(
                    label = "Volume",
                    value = Units.formatLitres(entry.volumeMl),
                    modifier = Modifier.weight(0.95f)
                )
                FuelCellMetric(
                    label = "Unit",
                    value = if (hideAmounts) "RM •••/L" else (derivedUnitPrice?.let { Units.formatUnitPrice(it) } ?: "—"),
                    modifier = Modifier.weight(1f)
                )
                FuelCellMetric(
                    label = "Paid",
                    value = totalSen?.let { Units.formatRm(it, hideAmounts) } ?: "—",
                    alignEnd = true,
                    emphasize = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val hasFooter = entry.notes.isNotBlank() || attachmentCount > 0 || entry.startsNewSegment || isAnomaly
        if (hasFooter) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (entry.startsNewSegment) {
            Text(
                text = "Starts new odometer segment",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (entry.notes.isNotBlank()) {
            Text(
                text = entry.notes,
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
                    contentDescription = "Delete fill-up",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FuelCellMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
    emphasize: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
