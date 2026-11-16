package com.carlens.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.carlens.ui.theme.StatusAmber
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

    val parsedOdo = Units.parseKm(odoText)
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

    val previewTripKm = if (!isEditing && lastMileageKm != null && parsedOdo != null && parsedOdo > lastMileageKm) {
        parsedOdo - lastMileageKm
    } else null
    val previewKmPerL = if (previewTripKm != null && volumeMl != null && volumeMl > 0 && tankStatus == TankStatus.FULL && !missedPrevious) {
        previewTripKm.toDouble() / (volumeMl / 1000.0)
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

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
    )
    val fieldShape = RoundedCornerShape(14.dp)

    Dialog(
        onDismissRequest = { handleDismiss(clearTextDraft = false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .heightIn(max = 720.dp)
                .imePadding(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalGasStation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isEditing) "Edit fill-up" else "Record fill-up",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (lastMileageKm != null && !isEditing) {
                                Text(
                                    text = "Last odometer: ${Units.formatKm(lastMileageKm)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(onClick = { handleDismiss(clearTextDraft = true) }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Form Scroll Content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DatePickerField(
                        label = "Fill-up date",
                        selectedDate = selectedDate,
                        onDateSelected = {
                            selectedDate = it
                            warnings = emptyList()
                        }
                    )

                    // Odometer with live trip preview
                    OutlinedTextField(
                        value = odoText,
                        onValueChange = {
                            odoText = it
                            warnings = emptyList()
                        },
                        label = { Text("Odometer (km)") },
                        supportingText = {
                            val hint = buildList {
                                if (lastMileageKm != null) add("Previous: ${Units.formatKm(lastMileageKm)}")
                                if (previewTripKm != null) add("+${Units.formatKm(previewTripKm)}")
                            }.joinToString(" · ")
                            if (hint.isNotEmpty()) Text(hint)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = fieldShape,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Total paid & Litres
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = totalPaidText,
                            onValueChange = {
                                totalPaidText = it
                                warnings = emptyList()
                            },
                            label = { Text("Total paid") },
                            prefix = { Text("RM ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = fieldShape,
                            colors = fieldColors,
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = litresText,
                            onValueChange = {
                                litresText = it
                                warnings = emptyList()
                            },
                            label = { Text("Litres") },
                            suffix = { Text("L") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = fieldShape,
                            colors = fieldColors,
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Live calculation summary pill
                    if (calculatedUnitPrice != null && totalSen != null && volumeMl != null && volumeMl > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Price: ${Units.formatUnitPrice(calculatedUnitPrice)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (previewKmPerL != null && previewKmPerL in 2.0..45.0) {
                                    Text(
                                        text = "~${Units.formatKmPerL(previewKmPerL)} km/L",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    } else if (suggestedTotalSen != null) {
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

                    // Tank status
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Tank status",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("Full tank", tankStatus == TankStatus.FULL && !missedPrevious) {
                                    tankStatus = TankStatus.FULL
                                    missedPrevious = false
                                },
                                Triple("Partial", tankStatus == TankStatus.PARTIAL) {
                                    tankStatus = TankStatus.PARTIAL
                                    missedPrevious = false
                                },
                                Triple("Missed previous", missedPrevious) {
                                    missedPrevious = !missedPrevious
                                }
                            ).forEach { (label, selected, onClick) ->
                                FilterChip(
                                    selected = selected,
                                    onClick = onClick,
                                    shape = RoundedCornerShape(12.dp),
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // Station & Fuel grade
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = stationText,
                            onValueChange = { stationText = it },
                            label = { Text("Station") },
                            placeholder = { Text("e.g. Petronas") },
                            shape = fieldShape,
                            colors = fieldColors,
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = fuelTypeText,
                            onValueChange = { fuelTypeText = it },
                            label = { Text("Fuel grade") },
                            placeholder = { Text("RON95") },
                            shape = fieldShape,
                            colors = fieldColors,
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    if (stationSuggestions.isNotEmpty()) {
                        SuggestionChipsRow(
                            suggestions = stationSuggestions,
                            currentValue = stationText,
                            onSelect = { stationText = it }
                        )
                    }

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

                    // Collapsed optional details
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showMoreFields = !showMoreFields },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Receipt, pump price, notes & meter reset",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (showMoreFields) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedVisibility(visible = showMoreFields) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                label = { Text("Pump price per litre (RM/L)") },
                                placeholder = { Text("2.05") },
                                prefix = { Text("RM ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = fieldShape,
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                label = { Text("Notes") },
                                placeholder = { Text("e.g. Highway trip, tyre pressure checked") },
                                shape = fieldShape,
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            AttachmentSection(
                                ownerType = AttachmentOwner.FUEL,
                                ownerId = tempOwnerId,
                                attachmentRepo = attachmentRepo
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Odometer discontinuity", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Turn on if cluster was replaced",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = newSegment,
                                    onCheckedChange = { newSegment = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    if (errors.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
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

                    if (warnings.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = StatusAmber.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = StatusAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Please verify entry:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusAmber
                                    )
                                }
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Sticky Bottom Actions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    if (warnings.isNotEmpty()) {
                        Button(
                            onClick = { doSave(confirmed = true, addAnother = false) },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("Confirm & save", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!isEditing) {
                                OutlinedButton(
                                    onClick = { doSave(confirmed = false, addAnother = true) },
                                    enabled = !isSaving,
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Text("Save & add another", maxLines = 1)
                                }
                            }
                            Button(
                                onClick = { doSave(confirmed = false, addAnother = false) },
                                enabled = !isSaving,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
