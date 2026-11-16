package com.carlens.ui.garage

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
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlens.domain.calc.DocumentStatus
import com.carlens.domain.calc.DueState
import com.carlens.domain.calc.Units
import com.carlens.domain.model.AppSettings
import com.carlens.domain.model.ThemeMode
import com.carlens.domain.model.Vehicle
import com.carlens.domain.model.VehicleDocument
import com.carlens.ui.components.AttachmentCountBadge
import com.carlens.ui.components.CarLensCard
import com.carlens.ui.components.CarLensFilterChip
import com.carlens.ui.components.CategoryTag
import com.carlens.ui.components.EmptyStateCard
import com.carlens.ui.components.PlateBadge
import com.carlens.ui.components.SectionHeader
import com.carlens.ui.components.StatusPillDot
import com.carlens.ui.components.UrgencyStripCard
import com.carlens.ui.components.dueStateColor
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GarageScreen(
    vehicles: List<Vehicle>,
    allVehicles: List<Vehicle>,
    selectedVehicle: Vehicle?,
    documents: List<DocumentStatus>,
    settings: AppSettings,
    attachmentCounts: Map<String, Int>,
    hasRecoveryBackup: Boolean,
    onSelectVehicle: (Vehicle) -> Unit,
    onAddVehicleClick: () -> Unit,
    onEditVehicleClick: (Vehicle) -> Unit,
    onToggleArchiveVehicle: (Vehicle, Boolean) -> Unit,
    onAddDocumentClick: () -> Unit,
    onEditDocument: (VehicleDocument) -> Unit,
    onRenewDocument: (VehicleDocument) -> Unit,
    onDeleteDocument: (VehicleDocument) -> Unit,
    onExportBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    onRestoreRecoveryClick: () -> Unit,
    onExportFuelCsvClick: () -> Unit,
    onExportExpensesCsvClick: () -> Unit,
    onExportServicesCsvClick: () -> Unit,
    onEditBudgetClick: () -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onHidePlateChanged: (Boolean) -> Unit,
    onHideAmountsChanged: (Boolean) -> Unit,
    onBiometricLockChanged: (Boolean) -> Unit,
    onHideNotifDetailsChanged: (Boolean) -> Unit,
) {
    var showArchived by remember { mutableStateOf(false) }
    val archivedCount = remember(allVehicles) { allVehicles.count { it.archived } }
    val visibleVehicles = if (showArchived) allVehicles else vehicles
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US) }

    val totalMonthlySavings = remember(documents) {
        val sum = documents.mapNotNull { it.monthlySavingsSen }.sum()
        if (sum > 0L) sum else null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Garage & Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${vehicles.size} active ${if (vehicles.size == 1) "vehicle" else "vehicles"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onAddVehicleClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Vehicle")
                }
            }
        }

        // Vehicles Section
        item {
            SectionHeader(
                title = "Vehicles",
                trailing = if (archivedCount > 0) {
                    {
                        CarLensFilterChip(
                            selected = showArchived,
                            onClick = { showArchived = !showArchived },
                            label = if (showArchived) "Hide archived" else "Archived ($archivedCount)"
                        )
                    }
                } else null
            )
        }

        if (visibleVehicles.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.DirectionsCar,
                    title = "No vehicles in garage",
                    subtitle = "Add your primary car — database and reports stay clean across multiple vehicles.",
                    actionLabel = "Add vehicle",
                    onAction = onAddVehicleClick
                )
            }
        } else {
            items(visibleVehicles, key = { it.id }) { vehicle ->
                val isSelected = vehicle.id == selectedVehicle?.id
                VehicleCard(
                    vehicle = vehicle,
                    isSelected = isSelected,
                    hidePlate = settings.hidePlate,
                    onClick = { if (!vehicle.archived) onSelectVehicle(vehicle) },
                    onEdit = { onEditVehicleClick(vehicle) },
                    onToggleArchive = { onToggleArchiveVehicle(vehicle, !vehicle.archived) }
                )
            }
        }

        // Documents & Renewals Section
        if (selectedVehicle != null) {
            item {
                SectionHeader(
                    title = "Documents & Renewals",
                    subtitle = selectedVehicle.name,
                    trailing = {
                        TextButton(onClick = onAddDocumentClick) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add doc")
                        }
                    }
                )
            }

            if (totalMonthlySavings != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estimated monthly renewal reserve",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${Units.formatRm(totalMonthlySavings, settings.hideAmounts)}/mo",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            if (documents.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Description,
                        title = "No documents tracked",
                        subtitle = "Store insurance, road tax, warranty, and inspection expiry dates with renewal reminders.",
                        actionLabel = "Add document",
                        onAction = onAddDocumentClick
                    )
                }
            } else {
                items(documents, key = { it.document.id }) { docStatus ->
                    DocumentStatusCard(
                        docStatus = docStatus,
                        attachmentCount = attachmentCounts[docStatus.document.id] ?: 0,
                        hideAmounts = settings.hideAmounts,
                        dateFmt = dateFmt,
                        onEdit = { onEditDocument(docStatus.document) },
                        onRenew = { onRenewDocument(docStatus.document) },
                        onDelete = { onDeleteDocument(docStatus.document) }
                    )
                }
            }
        }

        // Backup & Export Section
        item {
            SectionHeader(
                title = "Backup & Export",
                subtitle = "100% offline encrypted archives and CSV spreadsheets"
            )
        }

        item {
            CarLensCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExportBackupClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = onRestoreBackupClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (hasRecoveryBackup) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onRestoreRecoveryClick),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "On-device auto-recovery available",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap to inspect and restore latest snapshot",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    text = "Export CSV (${selectedVehicle?.name ?: "Vehicle"})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CsvExportChip(
                        label = "Fuel",
                        onClick = onExportFuelCsvClick,
                        modifier = Modifier.weight(1f)
                    )
                    CsvExportChip(
                        label = "Expenses",
                        onClick = onExportExpensesCsvClick,
                        modifier = Modifier.weight(1f)
                    )
                    CsvExportChip(
                        label = "Service",
                        onClick = onExportServicesCsvClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Theme & Settings Section
        item {
            SectionHeader(
                title = "Appearance & Privacy",
                subtitle = "Theme, budget, and sensitive information masking"
            )
        }

        item {
            CarLensCard {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

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
                        val options = listOf(
                            Triple(ThemeMode.SYSTEM, "System", Icons.Rounded.BrightnessAuto),
                            Triple(ThemeMode.LIGHT, "Light", Icons.Rounded.LightMode),
                            Triple(ThemeMode.DARK, "Dark", Icons.Rounded.DarkMode)
                        )
                        options.forEach { (mode, label, icon) ->
                            val selected = settings.themeMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onThemeModeChanged(mode) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                val budgetSen = settings.monthlyBudgetSen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onEditBudgetClick)
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Monthly spend budget",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (budgetSen != null && budgetSen > 0) {
                                    Units.formatRm(budgetSen, settings.hideAmounts)
                                } else {
                                    "Tap to set monthly target"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(onClick = onEditBudgetClick) {
                        Text(if (budgetSen != null && budgetSen > 0) "Edit" else "Set")
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                PrivacySwitchRow(
                    icon = Icons.Rounded.DirectionsCar,
                    title = "Hide number plates",
                    subtitle = "Mask plate badges in shared screenshots",
                    checked = settings.hidePlate,
                    onCheckedChange = onHidePlateChanged
                )
                PrivacySwitchRow(
                    icon = Icons.Rounded.VisibilityOff,
                    title = "Hide money amounts",
                    subtitle = "Replace RM values with RM •••",
                    checked = settings.hideAmounts,
                    onCheckedChange = onHideAmountsChanged
                )
                PrivacySwitchRow(
                    icon = Icons.Rounded.Fingerprint,
                    title = "Biometric / device lock",
                    subtitle = "Require authentication when opening CarLens",
                    checked = settings.biometricLock,
                    onCheckedChange = onBiometricLockChanged
                )
                PrivacySwitchRow(
                    icon = Icons.Rounded.NotificationsOff,
                    title = "Private reminder notifications",
                    subtitle = "Hide vehicle plates and task specifics on lock screen",
                    checked = settings.hideNotificationDetails,
                    onCheckedChange = onHideNotifDetailsChanged
                )
            }
        }
    }
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    isSelected: Boolean,
    hidePlate: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
) {
    CarLensCard(
        onClick = onClick,
        borderColor = if (isSelected && !vehicle.archived) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        containerColor = if (isSelected && !vehicle.archived) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.DirectionsCar,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = vehicle.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isSelected && !vehicle.archived) {
                            CategoryTag(text = "Active", color = MaterialTheme.colorScheme.primary)
                        } else if (vehicle.archived) {
                            CategoryTag(text = "Archived", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    val tankMl = vehicle.tankCapacityMl
                    val specs = listOfNotNull(
                        vehicle.year?.toString(),
                        listOf(vehicle.make, vehicle.model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null },
                        vehicle.fuelType.ifBlank { null },
                        tankMl?.let { "${Units.mlToPlain(it)} L tank" }
                    ).joinToString(" · ")
                    if (specs.isNotBlank()) {
                        Text(
                            text = specs,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            PlateBadge(plate = vehicle.plate, hidePlate = hidePlate)
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
            Text(
                text = when {
                    vehicle.archived -> "Archived — unarchive to switch"
                    isSelected -> "Currently selected vehicle"
                    else -> "Tap card to switch to ${vehicle.name}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit vehicle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onToggleArchive, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (vehicle.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                        contentDescription = if (vehicle.archived) "Unarchive" else "Archive",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentStatusCard(
    docStatus: DocumentStatus,
    attachmentCount: Int,
    hideAmounts: Boolean,
    dateFmt: DateTimeFormatter,
    onEdit: () -> Unit,
    onRenew: () -> Unit,
    onDelete: () -> Unit,
) {
    val doc = docStatus.document
    val accent = dueStateColor(docStatus.state)
    val isUrgent = docStatus.state != DueState.UP_TO_DATE
    val expiry = doc.expiryDate
    val daysRem = docStatus.daysRemaining
    val expectedCost = doc.expectedRenewalSen
    val monthlySavings = docStatus.monthlySavingsSen

    UrgencyStripCard(
        accentColor = accent,
        highlighted = isUrgent,
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = doc.title.ifBlank { doc.type.label },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val meta = listOfNotNull(
                    doc.type.label,
                    doc.provider.takeIf { it.isNotBlank() },
                    doc.reference.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPillDot(text = docStatus.state.label, color = accent)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val expiryStr = when {
                    daysRem != null && daysRem < 0 -> "Expired ${-daysRem} days ago (${expiry?.format(dateFmt) ?: ""})"
                    daysRem != null -> "Expires in $daysRem days (${expiry?.format(dateFmt) ?: ""})"
                    expiry != null -> "Expires ${expiry.format(dateFmt)}"
                    else -> "No expiry set"
                }
                Text(
                    text = expiryStr,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isUrgent) accent else MaterialTheme.colorScheme.onSurface
                )
                if (expectedCost != null && expectedCost > 0) {
                    val savingsText = monthlySavings?.let { " · Save ${Units.formatRm(it, hideAmounts)}/mo" } ?: ""
                    Text(
                        text = "Est. renewal ${Units.formatRm(expectedCost, hideAmounts)}$savingsText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
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
                FilledTonalButton(
                    onClick = onRenew,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Renew", style = MaterialTheme.typography.labelMedium)
                }
                AttachmentCountBadge(count = attachmentCount)
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit document",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete document",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CsvExportChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.TableChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PrivacySwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
