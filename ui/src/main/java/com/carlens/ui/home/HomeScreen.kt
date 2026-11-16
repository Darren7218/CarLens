package com.carlens.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EditRoad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlens.domain.calc.CostSummary
import com.carlens.domain.calc.DocumentStatus
import com.carlens.domain.calc.DueState
import com.carlens.domain.calc.FuelEconomyReport
import com.carlens.domain.calc.MaintenanceStatus
import com.carlens.domain.calc.MileageSnapshot
import com.carlens.domain.calc.TrendDirection
import com.carlens.domain.calc.Units
import com.carlens.domain.model.AppSettings
import com.carlens.domain.model.Vehicle
import com.carlens.domain.model.VehicleDocument
import com.carlens.ui.components.CarLensCard
import com.carlens.ui.components.EmptyStateCard
import com.carlens.ui.components.HeroAccentCard
import com.carlens.ui.components.SectionHeader
import com.carlens.ui.components.StatusPillDot
import com.carlens.ui.components.UrgencyStripCard
import com.carlens.ui.components.VehicleSelectorHeader
import com.carlens.ui.components.dueStateColor
import com.carlens.ui.theme.StatusAmber
import com.carlens.ui.theme.StatusGreen
import com.carlens.ui.theme.StatusRed
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    selectedVehicle: Vehicle?,
    allVehicles: List<Vehicle>,
    mileage: MileageSnapshot,
    monthlyCost: CostSummary?,
    fuelEconomy: FuelEconomyReport,
    nextMaintenance: MaintenanceStatus?,
    nextRenewal: DocumentStatus?,
    settings: AppSettings,
    onTogglePrivacy: () -> Unit = {},
    onAddFuelClick: () -> Unit,
    onUpdateMileageClick: () -> Unit,
    onViewOdometerHistoryClick: () -> Unit,
    onSelectVehicle: (Vehicle) -> Unit,
    onSelectVehicleClick: () -> Unit,
    onNavigateToCare: () -> Unit,
    onNavigateToCosts: () -> Unit,
    onNavigateToFuel: () -> Unit,
    onNavigateToGarage: () -> Unit,
    onRenewDocumentClick: (VehicleDocument) -> Unit,
) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US) }
    val monthFmt = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US) }
    val currentMonthLabel = remember { YearMonth.now().format(monthFmt) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        item {
            VehicleSelectorHeader(
                selectedVehicle = selectedVehicle,
                vehicles = allVehicles,
                hidePlate = settings.hidePlate,
                hideAmounts = settings.hideAmounts,
                onSelectVehicle = onSelectVehicle,
                onTogglePrivacy = onTogglePrivacy
            )
        }

        if (selectedVehicle == null) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.DirectionsCar,
                    title = "Welcome to CarLens",
                    subtitle = "Add your car in the Garage tab to track fuel economy, service intervals, renewals, and ownership costs offline.",
                    actionLabel = "Open Garage",
                    onAction = onSelectVehicleClick
                )
            }
            return@LazyColumn
        }

        // Hero Odometer & Primary Actions
        item {
            val currentKm = mileage.currentKm
            val readingDate = mileage.readingDate
            val kmPerDay = mileage.kmPerDay

            HeroAccentCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "CURRENT ODOMETER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (currentKm != null) Units.formatKm(currentKm) else "No reading yet",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val meta = buildList {
                            if (readingDate != null) add("Updated ${readingDate.format(dateFmt)}")
                            if (kmPerDay != null && kmPerDay > 0.5) {
                                add("~${kmPerDay.toInt()} km/day avg")
                            }
                        }.joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onViewOdometerHistoryClick),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = "Odometer history",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "History",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAddFuelClick,
                        modifier = Modifier
                            .weight(1.45f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalGasStation,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add fuel",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = onUpdateMileageClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditRoad,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Log km",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Question 1: What did I spend?
        item {
            val budgetSen = settings.monthlyBudgetSen
            val spendTotalSen = monthlyCost?.totalSen ?: 0L
            val spendExpenses = monthlyCost?.expenses.orEmpty()
            val costPerKm = monthlyCost?.costPerKmSen
            val topCat = monthlyCost?.byCategory?.firstOrNull()

            CarLensCard(onClick = onNavigateToCosts) {
                HomeCardHeader(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    iconTint = MaterialTheme.colorScheme.primary,
                    overline = "WHAT DID I SPEND?",
                    actionLabel = currentMonthLabel
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = Units.formatRm(spendTotalSen, settings.hideAmounts),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val spendSub = buildList {
                            add("${spendExpenses.size} ${if (spendExpenses.size == 1) "record" else "records"}")
                            if (topCat != null && spendTotalSen > 0) {
                                val pct = ((topCat.totalSen.toDouble() / spendTotalSen.toDouble()) * 100).toInt()
                                add("${topCat.category.label} $pct%")
                            }
                        }.joinToString(" · ")
                        Text(
                            text = spendSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (costPerKm != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = Units.formatRmPerKm(costPerKm, settings.hideAmounts),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (budgetSen != null && budgetSen > 0L) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val ratio = (spendTotalSen.toFloat() / budgetSen.toFloat()).coerceIn(0f, 1.5f)
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
                            text = "Monthly budget ${Units.formatRmCompact(budgetSen, settings.hideAmounts)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val remaining = budgetSen - spendTotalSen
                        Text(
                            text = if (remaining >= 0) {
                                "${Units.formatRmCompact(remaining, settings.hideAmounts)} left"
                            } else {
                                "${Units.formatRmCompact(-remaining, settings.hideAmounts)} over"
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

        // Question 2: How efficiently am I driving?
        item {
            val overallKmPerL = fuelEconomy.overallKmPerL
            val overallL100 = fuelEconomy.overallLPer100
            val fuelCostPerKm = fuelEconomy.costPerKmSen
            val latest = fuelEconomy.latest
            val trend = fuelEconomy.trend

            CarLensCard(onClick = onNavigateToFuel) {
                HomeCardHeader(
                    icon = Icons.Rounded.Speed,
                    iconTint = MaterialTheme.colorScheme.primary,
                    overline = "HOW EFFICIENTLY AM I DRIVING?",
                    actionLabel = "Fuel log"
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (overallKmPerL != null && overallL100 != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = Units.formatKmPerL(overallKmPerL),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "km/L",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                            Text(
                                text = "Overall (${fuelEconomy.intervals.count { !it.suspicious }} valid intervals)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = "${Units.formatLPer100(overallL100)} L/100 km",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            if (fuelCostPerKm != null) {
                                Text(
                                    text = Units.formatRmPerKm(fuelCostPerKm, settings.hideAmounts),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (latest != null || trend != null || fuelEconomy.anomalies.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (latest != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Last fill-up (${latest.endDate.format(dateFmt)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${Units.formatKmPerL(latest.kmPerL)} km/L · ${Units.formatKm(latest.distanceKm)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (trend != null) {
                                val (trendColor, trendIcon) = when (trend.direction) {
                                    TrendDirection.DECREASED -> StatusGreen to Icons.AutoMirrored.Rounded.TrendingDown
                                    TrendDirection.INCREASED -> StatusAmber to Icons.AutoMirrored.Rounded.TrendingUp
                                    TrendDirection.STABLE -> MaterialTheme.colorScheme.primary to Icons.Rounded.CheckCircle
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = trendIcon,
                                        contentDescription = null,
                                        tint = trendColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = trend.message,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = trendColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (fuelEconomy.anomalies.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = StatusAmber,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "${fuelEconomy.anomalies.size} fill-up entry needs review",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusAmber,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Record two full tank fill-ups to calculate km/L, L/100 km, and fuel cost/km.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Question 3: What needs attention next?
        item {
            SectionHeader(
                title = "What needs attention next?",
                subtitle = "Service schedule and document renewals",
                trailing = {
                    TextButton(onClick = onNavigateToCare) {
                        Text("View all")
                    }
                }
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (nextMaintenance == null && nextRenewal == null) {
                    CarLensCard(onClick = onNavigateToCare) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusGreen.copy(alpha = 0.14f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusGreen,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No maintenance or renewals configured",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Add service reminders in Care or road tax/insurance dates in Garage.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (nextMaintenance != null) {
                    val accent = dueStateColor(nextMaintenance.state)
                    val isUrgent = nextMaintenance.state != DueState.UP_TO_DATE
                    UrgencyStripCard(
                        accentColor = accent,
                        highlighted = isUrgent,
                        onClick = onNavigateToCare
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Build,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = nextMaintenance.rule.task,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            StatusPillDot(
                                text = nextMaintenance.state.label,
                                color = accent
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = nextMaintenance.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val progress = nextMaintenance.progressFraction
                        if (progress != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = accent,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        }

                        val estDate = nextMaintenance.estimatedKmDueDate
                        if (estDate != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Estimated mileage due ~${estDate.format(dateFmt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (nextRenewal != null) {
                    val accent = dueStateColor(nextRenewal.state)
                    val isUrgent = nextRenewal.state != DueState.UP_TO_DATE
                    val expiry = nextRenewal.document.expiryDate
                    val daysRem = nextRenewal.daysRemaining
                    val expectedCost = nextRenewal.document.expectedRenewalSen

                    UrgencyStripCard(
                        accentColor = accent,
                        highlighted = isUrgent,
                        onClick = onNavigateToGarage
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Description,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = nextRenewal.document.title.ifBlank { nextRenewal.document.type.label },
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            StatusPillDot(
                                text = nextRenewal.state.label,
                                color = accent
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                val expiryText = when {
                                    daysRem != null && daysRem < 0 -> "Expired ${-daysRem} days ago"
                                    daysRem != null -> "Expires in $daysRem days (${expiry?.format(dateFmt) ?: ""})"
                                    expiry != null -> "Expires ${expiry.format(dateFmt)}"
                                    else -> "No expiry date"
                                }
                                Text(
                                    text = expiryText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (expectedCost != null && expectedCost > 0) {
                                    Text(
                                        text = "Est. renewal ${Units.formatRm(expectedCost, settings.hideAmounts)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isUrgent) {
                                FilledTonalButton(
                                    onClick = { onRenewDocumentClick(nextRenewal.document) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Renew", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeCardHeader(
    icon: ImageVector,
    iconTint: Color,
    overline: String,
    actionLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = iconTint.copy(alpha = 0.13f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp)
                )
            }
            Text(
                text = overline,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
