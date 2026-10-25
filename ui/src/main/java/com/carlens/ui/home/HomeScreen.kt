package com.carlens.ui.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlens.domain.calc.CostSummary
import com.carlens.domain.calc.DocumentStatus
import com.carlens.domain.calc.DueState
import com.carlens.domain.calc.FuelEconomyReport
import com.carlens.domain.calc.MaintenanceStatus
import com.carlens.domain.calc.MileageSnapshot
import com.carlens.domain.calc.Units
import com.carlens.domain.model.AppSettings
import com.carlens.domain.model.Vehicle
import com.carlens.domain.model.VehicleDocument
import com.carlens.ui.components.PlateBadge
import com.carlens.ui.components.StatCard
import com.carlens.ui.components.StatusBadge
import com.carlens.ui.theme.StatusAmber
import com.carlens.ui.theme.StatusRed
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
    onAddFuelClick: () -> Unit,
    onUpdateMileageClick: () -> Unit,
    onViewOdometerHistoryClick: () -> Unit,
    onSelectVehicleClick: () -> Unit,
    onNavigateToCare: () -> Unit,
    onNavigateToCosts: () -> Unit,
    onNavigateToFuel: () -> Unit,
    onNavigateToGarage: () -> Unit,
    onRenewDocumentClick: (VehicleDocument) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Vehicle Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onSelectVehicleClick)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedVehicle?.name ?: "No Vehicle Selected",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (selectedVehicle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${selectedVehicle.make} ${selectedVehicle.model} ${selectedVehicle.year ?: ""}".trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selectedVehicle != null) {
                PlateBadge(plate = selectedVehicle.plate, hidePlate = settings.hidePlate)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Card: Current Mileage & Primary "Add Fuel" CTA
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onViewOdometerHistoryClick)
                    ) {
                        Text(
                            text = "LATEST MILEAGE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mileage.currentKm?.let { Units.formatKm(it) } ?: "No readings yet",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val metaParts = buildList {
                            mileage.readingDate?.let { add("As of $it") }
                            mileage.kmPerDay?.let { add(String.format(Locale.US, "~%.0f km/day", it)) }
                        }
                        if (metaParts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = metaParts.joinToString(" · ") + " · Tap for log",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(onClick = onUpdateMileageClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Update Mileage",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAddFuelClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.LocalGasStation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Fuel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Three Core Questions Section

        // 1. What did I spend?
        Text(
            text = "What Did I Spend?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        val spendingSen = monthlyCost?.totalSen ?: 0L
        val budgetSen = settings.monthlyBudgetSen
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToCosts),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "This Month (${java.time.YearMonth.now().month.name.lowercase().replaceFirstChar { it.uppercase() }})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = Units.formatRm(spendingSen, settings.hideAmounts),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (budgetSen != null && budgetSen > 0) {
                    val remaining = budgetSen - spendingSen
                    val progress = (spendingSen.toFloat() / budgetSen.toFloat()).coerceIn(0f, 1f)
                    val overBudget = remaining < 0
                    Text(
                        text = if (!overBudget) {
                            "Under budget by ${Units.formatRm(remaining, settings.hideAmounts)} (of ${Units.formatRm(budgetSen, settings.hideAmounts)})"
                        } else {
                            "Over budget by ${Units.formatRm(-remaining, settings.hideAmounts)} (budget: ${Units.formatRm(budgetSen, settings.hideAmounts)})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overBudget) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (overBudget) StatusRed else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    Text(
                        text = "Tap to inspect category breakdown & 6-month trend",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. How efficiently am I driving?
        Text(
            text = "How Efficiently Am I Driving?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val latestInterval = fuelEconomy.latest
            StatCard(
                title = "Latest Economy",
                value = latestInterval?.let { "${Units.formatKmPerL(it.kmPerL)} km/L" } ?: "—",
                subtitle = latestInterval?.let { "${Units.formatLPer100(it.lPer100Km)} L/100 km" } ?: "Need 2 full fill-ups",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToFuel
            )

            StatCard(
                title = "Overall Average",
                value = fuelEconomy.overallKmPerL?.let { "${Units.formatKmPerL(it)} km/L" } ?: "—",
                subtitle = fuelEconomy.costPerKmSen?.let { Units.formatRmPerKm(it, settings.hideAmounts) } ?: "All valid intervals",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToFuel
            )
        }

        // Fuel Trend Alert (if detected offline)
        if (fuelEconomy.trend != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(
                    containerColor = when (fuelEconomy.trend!!.direction) {
                        com.carlens.domain.calc.TrendDirection.INCREASED -> StatusAmber.copy(alpha = 0.15f)
                        com.carlens.domain.calc.TrendDirection.DECREASED -> MaterialTheme.colorScheme.primaryContainer
                        com.carlens.domain.calc.TrendDirection.STABLE -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${fuelEconomy.trend!!.message} (${String.format(Locale.US, "%+.1f%%", fuelEconomy.trend!!.changePercent)})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. What is due next?
        Text(
            text = "What Needs Attention Next?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (nextMaintenance == null && nextRenewal == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "All maintenance and renewals are up to date!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            if (nextMaintenance != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToCare),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = nextMaintenance.rule.task,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            StatusBadge(state = nextMaintenance.state)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Due: ${nextMaintenance.summary}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (nextMaintenance.estimatedKmDueDate != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Estimated due date based on driving: ${nextMaintenance.estimatedKmDueDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (nextRenewal != null) {
                if (nextMaintenance != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToGarage),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = nextRenewal.document.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            StatusBadge(state = nextRenewal.state)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val days = nextRenewal.daysRemaining ?: 0
                            Text(
                                text = if (days >= 0) {
                                    "Expires in $days days (${nextRenewal.document.expiryDate})"
                                } else {
                                    "Expired ${-days} days ago (${nextRenewal.document.expiryDate})"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (nextRenewal.state != DueState.UP_TO_DATE) {
                                TextButton(onClick = { onRenewDocumentClick(nextRenewal.document) }) {
                                    Text("Renew")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
