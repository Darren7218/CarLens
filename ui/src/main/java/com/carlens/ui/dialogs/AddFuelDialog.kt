package com.carlens.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carlens.domain.calc.Units
import com.carlens.domain.model.AttachmentOwner
import com.carlens.domain.model.EventTime
import com.carlens.domain.model.FuelEntry
import com.carlens.domain.model.FuelEntryInput
import com.carlens.domain.model.TankStatus
import com.carlens.domain.repository.AttachmentRepository
import com.carlens.domain.repository.SettingsRepository
import com.carlens.domain.usecase.SaveFuelEntry
import com.carlens.domain.usecase.SaveResult
import com.carlens.ui.components.AttachmentSection
import com.carlens.ui.components.DatePickerField
import com.carlens.ui.components.SuggestionChipsRow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

@Composable
fun AddFuelDialog(
    vehicleId: String,
    lastMileageKm: Long?,
    defaultStation: String,
    defaultFuelType: String,
    stationSuggestions: List<String> = emptyList(),
    entryToEdit: FuelEntry? = null,
    saveFuelEntry: SaveFuelEntry,
    attachmentRepo: AttachmentRepository,
    settingsRepo: SettingsRepository? = null,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val isEditing = entryToEdit != null
    val draftKey = "fuel_$vehicleId"
    var tempOwnerId by remember { mutableStateOf(entryToEdit?.id ?: "draft_fuel_${UUID.randomUUID()}") }

    var selectedDate by remember { mutableStateOf(entryToEdit?.time?.localDate ?: LocalDate.now()) }
    var odoText by remember { mutableStateOf(entryToEdit?.odometerKm?.toString() ?: "") }
    var litresText by remember {
        mutableStateOf(entryToEdit?.volumeMl?.let { Units.mlToPlain(it) } ?: "")
    }
    var totalPaidText by remember {
        mutableStateOf(entryToEdit?.totalSen?.let { Units.senToPlain(it) } ?: "")
    }
    var unitPriceText by remember {
        mutableStateOf(entryToEdit?.unitPriceMilliSen?.let { Units.unitPriceToPlain(it) } ?: "")
    }
    var tankStatus by remember { mutableStateOf(entryToEdit?.tankStatus ?: TankStatus.FULL) }
    var missedPrevious by remember { mutableStateOf(entryToEdit?.missedPrevious ?: false) }
    var newSegment by remember { mutableStateOf(entryToEdit?.startsNewSegment ?: false) }
    var stationText by remember { mutableStateOf(entryToEdit?.station ?: defaultStation) }
    var fuelTypeText by remember {
        mutableStateOf(entryToEdit?.fuelType ?: defaultFuelType.ifBlank { "RON95" })
    }
    var notesText by remember { mutableStateOf(entryToEdit?.notes ?: "") }

    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var warnings by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }
    var showMoreFields by remember {
        mutableStateOf(isEditing || (entryToEdit?.notes?.isNotBlank() == true))
    }

    val scope = rememberCoroutineScope()
    var draftLoaded by remember { mutableStateOf(isEditing) }

    LaunchedEffect(vehicleId) {
        if (!isEditing && settingsRepo != null) {
            val raw = settingsRepo.loadDraft(draftKey)
            if (!raw.isNullOrBlank()) {
                runCatching {
                    val obj = JSONObject(raw)
                    obj.optString("odo").takeIf { it.isNotBlank() }?.let { odoText = it }
                    obj.optString("litres").takeIf { it.isNotBlank() }?.let { litresText = it }
                    obj.optString("totalPaid").takeIf { it.isNotBlank() }?.let { totalPaidText = it }
                    obj.optString("station").takeIf { it.isNotBlank() }?.let { stationText = it }
                    obj.optString("fuelType").takeIf { it.isNotBlank() }?.let { fuelTypeText = it }
                    obj.optString("notes").takeIf { it.isNotBlank() }?.let { notesText = it }
                }
            }
            draftLoaded = true
        }
    }

    LaunchedEffect(odoText, litresText, totalPaidText, stationText, fuelTypeText, notesText, draftLoaded) {
        if (!isEditing && draftLoaded && settingsRepo != null) {
            if (odoText.isBlank() && litresText.isBlank() && totalPaidText.isBlank() && notesText.isBlank()) {
                settingsRepo.saveDraft(draftKey, null)
            } else {
                val json = JSONObject().apply {
                    put("odo", odoText)
                    put("litres", litresText)
                    put("totalPaid", totalPaidText)
                    put("station", stationText)
                    put("fuelType", fuelTypeText)
                    put("notes", notesText)
                }.toString()
                settingsRepo.saveDraft(draftKey, json)
            }
        }
    }

    fun handleDismiss(clearTextDraft: Boolean) {
        scope.launch {
            if (!isEditing) {
                attachmentRepo.deleteForOwner(tempOwnerId)
                if (clearTextDraft) {
                    settingsRepo?.saveDraft(draftKey, null)
                }
            }
            onDismiss()
        }
    }

    val totalSen = Units.parseMoneyToSen(totalPaidText)
    val volumeMl = Units.parseLitresToMl(litresText)
    val explicitUnitPrice = Units.parseUnitPriceToMilliSen(unitPriceText)
    val calculatedUnitPrice = when {
        totalSen != null && volumeMl != null && volumeMl > 0 -> Units.unitPriceMilliSen(totalSen, volumeMl)
        explicitUnitPrice != null -> explicitUnitPrice
        else -> null
    }
    val suggestedTotalSen = if (totalSen == null && volumeMl != null && volumeMl > 0 && explicitUnitPrice != null && explicitUnitPrice > 0) {
        Units.suggestedTotalSen(volumeMl, explicitUnitPrice)
    } else null

    fun resolveEventTime(): EventTime {
        if (entryToEdit != null && selectedDate == entryToEdit.time.localDate) {
            return entryToEdit.time
        }
        return if (selectedDate == LocalDate.now()) EventTime.now() else EventTime.ofDate(selectedDate)
    }

    fun doSave(confirmed: Boolean, addAnother: Boolean) {
        val odoKm = Units.parseKm(odoText)
        if (odoKm == null) {
            errors = listOf("Please enter a valid odometer reading.")
            return
        }
        val finalVolumeMl = volumeMl
        if (finalVolumeMl == null || finalVolumeMl <= 0) {
            errors = listOf("Please enter the litres pumped.")
            return
        }
        val finalTotalSen = totalSen ?: suggestedTotalSen
        if (finalTotalSen == null || finalTotalSen <= 0) {
            errors = listOf("Please enter the total amount paid (RM).")
            return
        }

        val input = FuelEntryInput(
            id = entryToEdit?.id,
            vehicleId = vehicleId,
            time = resolveEventTime(),
            odometerKm = odoKm,
            startsNewSegment = newSegment,
            volumeMl = finalVolumeMl,
            totalSen = finalTotalSen,
            unitPriceMilliSen = Units.unitPriceMilliSen(finalTotalSen, finalVolumeMl),
            tankStatus = tankStatus,
            missedPrevious = missedPrevious,
            station = stationText.trim(),
            fuelType = fuelTypeText.trim(),
            notes = notesText.trim()
        )

        isSaving = true
        scope.launch {
            val result = saveFuelEntry(input, confirmed = confirmed)
            isSaving = false
            when (result) {
                is SaveResult.Saved -> {
                    if (!isEditing) {
                        attachmentRepo.reassign(tempOwnerId, result.id)
                        settingsRepo?.saveDraft(draftKey, null)
                    }
                    if (addAnother) {
                        tempOwnerId = "draft_fuel_${UUID.randomUUID()}"
                        odoText = ""
                        litresText = ""
                        totalPaidText = ""
                        errors = emptyList()
                        warnings = emptyList()
                    } else {
                        onSuccess()
                    }
                }
                is SaveResult.NeedsConfirmation -> {
                    warnings = result.warnings
                }
                is SaveResult.Invalid -> {
                    errors = result.errors
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { handleDismiss(clearTextDraft = false) },
        title = {
            Text(
                text = if (isEditing) "Edit Fuel Entry" else "Add Fuel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                DatePickerField(
                    label = "Fill-up Date",
                    selectedDate = selectedDate,
                    onDateSelected = {
                        selectedDate = it
                        warnings = emptyList()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Odometer
                OutlinedTextField(
                    value = odoText,
                    onValueChange = {
                        odoText = it
                        warnings = emptyList()
                    },
                    label = { Text("Odometer reading (km)") },
                    supportingText = {
                        if (lastMileageKm != null) {
                            Text("Latest reading: ${Units.formatKm(lastMileageKm)}")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Total paid & Litres row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = totalPaidText,
                        onValueChange = {
                            totalPaidText = it
                            warnings = emptyList()
                        },
                        label = { Text("Total Paid (RM)") },
                        prefix = { Text("RM ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = litresText,
                        onValueChange = {
                            litresText = it
                            warnings = emptyList()
                        },
                        label = { Text("Litres (L)") },
                        suffix = { Text("L") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Bidirectional Price per Litre / Computed summary
                if (calculatedUnitPrice != null && totalSen != null && volumeMl != null && volumeMl > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Worked out price: ${Units.formatUnitPrice(calculatedUnitPrice)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else if (suggestedTotalSen != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Suggested total: ${Units.formatRm(suggestedTotalSen)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        SuggestionChip(
                            onClick = { totalPaidText = Units.senToPlain(suggestedTotalSen) },
                            label = { Text("Apply") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tank Status chips
                Text(
                    text = "Tank Status",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = tankStatus == TankStatus.FULL && !missedPrevious,
                        onClick = {
                            tankStatus = TankStatus.FULL
                            missedPrevious = false
                        },
                        label = { Text("Full Tank") }
                    )
                    FilterChip(
                        selected = tankStatus == TankStatus.PARTIAL,
                        onClick = {
                            tankStatus = TankStatus.PARTIAL
                            missedPrevious = false
                        },
                        label = { Text("Partial") }
                    )
                    FilterChip(
                        selected = missedPrevious,
                        onClick = {
                            missedPrevious = !missedPrevious
                        },
                        label = { Text("Missed Previous") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick presets for Station & Fuel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stationText,
                        onValueChange = { stationText = it },
                        label = { Text("Station") },
                        placeholder = { Text("e.g. Petronas") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fuelTypeText,
                        onValueChange = { fuelTypeText = it },
                        label = { Text("Fuel Type") },
                        placeholder = { Text("RON95 / Diesel") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                SuggestionChipsRow(
                    suggestions = stationSuggestions,
                    currentValue = stationText,
                    onSelect = { stationText = it },
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("RON95", "RON97", "Diesel", "Euro 5 Diesel").forEach { preset ->
                        if (!fuelTypeText.equals(preset, ignoreCase = true)) {
                            SuggestionChip(
                                onClick = { fuelTypeText = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Collapsible extra options
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (showMoreFields) "▲ Hide extra details" else "▼ Price/L calculator, receipt, notes & meter reset",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { showMoreFields = !showMoreFields }
                        .padding(vertical = 4.dp)
                )

                if (showMoreFields) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = unitPriceText,
                        onValueChange = {
                            unitPriceText = it
                            val up = Units.parseUnitPriceToMilliSen(it)
                            if (up != null && up > 0) {
                                val vol = Units.parseLitresToMl(litresText)
                                val tot = Units.parseMoneyToSen(totalPaidText)
                                if (vol != null && vol > 0 && tot == null) {
                                    totalPaidText = Units.senToPlain(Units.suggestedTotalSen(vol, up))
                                } else if (tot != null && tot > 0 && vol == null) {
                                    val calcMl = (tot * 1_000_000L) / up
                                    litresText = Units.mlToPlain(calcMl)
                                }
                            }
                        },
                        label = { Text("Pump Price per Litre (RM/L, optional)") },
                        placeholder = { Text("e.g. 2.05") },
                        prefix = { Text("RM ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes") },
                        placeholder = { Text("e.g. Highway trip, tyre pressure checked") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    AttachmentSection(
                        ownerType = AttachmentOwner.FUEL,
                        ownerId = tempOwnerId,
                        attachmentRepo = attachmentRepo
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Odometer Discontinuity", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Turn on if gauge or cluster was replaced",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = newSegment,
                            onCheckedChange = { newSegment = it }
                        )
                    }
                }

                // Errors display
                if (errors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            errors.forEach { err ->
                                Text(
                                    text = "• $err",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Confirmation / Warnings display
                if (warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Please verify entry:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            warnings.forEach { warn ->
                                Text(
                                    text = "• $warn",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (warnings.isNotEmpty()) {
                    Button(
                        onClick = { doSave(confirmed = true, addAnother = false) },
                        enabled = !isSaving
                    ) {
                        Text("Confirm & Save")
                    }
                } else {
                    if (!isEditing) {
                        OutlinedButton(
                            onClick = { doSave(confirmed = false, addAnother = true) },
                            enabled = !isSaving
                        ) {
                            Text("Save & Add")
                        }
                    }
                    Button(
                        onClick = { doSave(confirmed = false, addAnother = false) },
                        enabled = !isSaving
                    ) {
                        Text("Save")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { handleDismiss(clearTextDraft = true) }) {
                Text("Cancel")
            }
        }
    )
}
