package com.carlens.ui.dialogs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.carlens.domain.model.ExpenseCategory
import com.carlens.domain.model.MaintenanceRule
import com.carlens.domain.model.ServiceItemInput
import com.carlens.domain.model.ServiceRecord
import com.carlens.domain.model.ServiceRecordInput
import com.carlens.domain.repository.AttachmentRepository
import com.carlens.domain.usecase.RecordService
import com.carlens.domain.usecase.SaveResult
import com.carlens.ui.components.AttachmentSection
import com.carlens.ui.components.DatePickerField
import com.carlens.ui.components.SuggestionChipsRow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class ServiceItemDraft(
    var id: String? = null,
    var task: String = "",
    var part: String = "",
    var costText: String = "",
    var ruleId: String? = null
)

@Composable
fun AddServiceDialog(
    vehicleId: String,
    lastMileageKm: Long?,
    availableRules: List<MaintenanceRule>,
    workshopSuggestions: List<String> = emptyList(),
    taskSuggestions: List<String> = emptyList(),
    initialRule: MaintenanceRule? = null,
    recordToEdit: ServiceRecord? = null,
    recordService: RecordService,
    attachmentRepo: AttachmentRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val isEditing = recordToEdit != null
    val tempOwnerId = remember { recordToEdit?.id ?: "draft_service_${UUID.randomUUID()}" }

    var selectedDate by remember { mutableStateOf(recordToEdit?.time?.localDate ?: LocalDate.now()) }
    var category by remember { mutableStateOf(recordToEdit?.category ?: ExpenseCategory.MAINTENANCE) }
    var titleText by remember {
        mutableStateOf(recordToEdit?.title ?: initialRule?.task ?: "Routine Service")
    }
    var workshopText by remember { mutableStateOf(recordToEdit?.workshop ?: "") }
    var odometerText by remember {
        mutableStateOf(
            if (isEditing) (recordToEdit?.odometerKm?.toString() ?: "")
            else (lastMileageKm?.toString() ?: "")
        )
    }
    var totalBillText by remember {
        mutableStateOf(recordToEdit?.totalSen?.let { Units.senToPlain(it) } ?: "")
    }
    var notesText by remember { mutableStateOf(recordToEdit?.notes ?: "") }

    val items = remember {
        if (recordToEdit != null && recordToEdit.items.isNotEmpty()) {
            mutableStateListOf(
                *recordToEdit.items.map {
                    ServiceItemDraft(
                        id = it.id,
                        task = it.task,
                        part = it.part,
                        costText = it.costSen?.let { c -> Units.senToPlain(c) } ?: "",
                        ruleId = it.maintenanceRuleId
                    )
                }.toTypedArray()
            )
        } else {
            mutableStateListOf(
                ServiceItemDraft(
                    task = initialRule?.task ?: "Engine Oil & Filter",
                    ruleId = initialRule?.id
                )
            )
        }
    }

    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var warnings by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val sumOfItemsSen = items.sumOf { Units.parseMoneyToSen(it.costText) ?: 0L }

    fun resolveEventTime(): EventTime {
        if (recordToEdit != null && selectedDate == recordToEdit.time.localDate) {
            return recordToEdit.time
        }
        return if (selectedDate == LocalDate.now()) EventTime.now() else EventTime.ofDate(selectedDate)
    }

    fun doSave(confirmed: Boolean) {
        val explicitTotalSen = Units.parseMoneyToSen(totalBillText)
        val totalSen = explicitTotalSen ?: sumOfItemsSen
        val odoKm = Units.parseKm(odometerText)

        val inputItems = items.map { draft ->
            ServiceItemInput(
                id = draft.id,
                task = draft.task.trim(),
                part = draft.part.trim(),
                quantity = null,
                costSen = Units.parseMoneyToSen(draft.costText),
                maintenanceRuleId = draft.ruleId
            )
        }

        val input = ServiceRecordInput(
            id = recordToEdit?.id,
            vehicleId = vehicleId,
            time = resolveEventTime(),
            odometerKm = odoKm,
            title = titleText.trim(),
            workshop = workshopText.trim(),
            notes = notesText.trim(),
            category = category,
            totalSen = totalSen,
            items = inputItems
        )

        isSaving = true
        scope.launch {
            val result = recordService(input, confirmed = confirmed)
            isSaving = false
            when (result) {
                is SaveResult.Saved -> {
                    if (!isEditing) {
                        attachmentRepo.reassign(tempOwnerId, result.id)
                    }
                    onSuccess()
                }
                is SaveResult.NeedsConfirmation -> warnings = result.warnings
                is SaveResult.Invalid -> errors = result.errors
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Service Record" else "Record Service",
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
                // Category switcher: Maintenance vs Repair
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = category == ExpenseCategory.MAINTENANCE,
                        onClick = { category = ExpenseCategory.MAINTENANCE },
                        label = { Text("Maintenance") }
                    )
                    FilterChip(
                        selected = category == ExpenseCategory.REPAIR,
                        onClick = { category = ExpenseCategory.REPAIR },
                        label = { Text("Repair") }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                DatePickerField(
                    label = "Service Date",
                    selectedDate = selectedDate,
                    onDateSelected = {
                        selectedDate = it
                        warnings = emptyList()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Service Title") },
                    placeholder = { Text("e.g. 50,000 km Major Service") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = workshopText,
                        onValueChange = { workshopText = it },
                        label = { Text("Workshop") },
                        placeholder = { Text("e.g. Toyota Service Centre") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = odometerText,
                        onValueChange = { odometerText = it },
                        label = { Text("Odometer (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                SuggestionChipsRow(
                    suggestions = workshopSuggestions,
                    currentValue = workshopText,
                    onSelect = { workshopText = it },
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = totalBillText,
                    onValueChange = {
                        totalBillText = it
                        warnings = emptyList()
                    },
                    label = { Text("Total Bill Amount (RM)") },
                    supportingText = {
                        if (sumOfItemsSen > 0) {
                            Text("Sum of itemized costs: ${Units.formatRm(sumOfItemsSen)}")
                        }
                    },
                    prefix = { Text("RM ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Work Performed / Parts (${items.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { items.add(ServiceItemDraft()) }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item")
                    }
                }

                items.forEachIndexed { index, itemDraft ->
                    var showRuleDropdown by remember { mutableStateOf(false) }
                    val linkedRule = availableRules.firstOrNull { it.id == itemDraft.ruleId }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Item #${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (items.size > 1) {
                                    IconButton(
                                        onClick = { items.removeAt(index) },
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = itemDraft.task,
                                onValueChange = { itemDraft.task = it; items[index] = itemDraft.copy() },
                                label = { Text("Task / Operation") },
                                placeholder = { Text("e.g. Engine Oil Change") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            SuggestionChipsRow(
                                suggestions = taskSuggestions,
                                currentValue = itemDraft.task,
                                onSelect = {
                                    itemDraft.task = it
                                    items[index] = itemDraft.copy()
                                },
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = itemDraft.part,
                                    onValueChange = { itemDraft.part = it; items[index] = itemDraft.copy() },
                                    label = { Text("Part / Fluid") },
                                    placeholder = { Text("Toyota 0W-20") },
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = itemDraft.costText,
                                    onValueChange = { itemDraft.costText = it; items[index] = itemDraft.copy() },
                                    label = { Text("Cost (RM)") },
                                    prefix = { Text("RM ") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            if (availableRules.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box {
                                    AssistChip(
                                        onClick = { showRuleDropdown = true },
                                        leadingIcon = {
                                            Icon(Icons.Default.Link, contentDescription = null)
                                        },
                                        label = {
                                            Text(
                                                text = linkedRule?.let { "Resets: ${it.task}" }
                                                    ?: "Link to Maintenance Schedule…",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    )
                                    DropdownMenu(
                                        expanded = showRuleDropdown,
                                        onDismissRequest = { showRuleDropdown = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("None (Standalone Task)") },
                                            onClick = {
                                                itemDraft.ruleId = null
                                                items[index] = itemDraft.copy()
                                                showRuleDropdown = false
                                            }
                                        )
                                        availableRules.forEach { rule ->
                                            DropdownMenuItem(
                                                text = { Text(rule.task) },
                                                onClick = {
                                                    itemDraft.ruleId = rule.id
                                                    if (itemDraft.task.isBlank()) {
                                                        itemDraft.task = rule.task
                                                    }
                                                    items[index] = itemDraft.copy()
                                                    showRuleDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                AttachmentSection(
                    ownerType = AttachmentOwner.SERVICE,
                    ownerId = tempOwnerId,
                    attachmentRepo = attachmentRepo
                )

                if (errors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            errors.forEach {
                                Text("• $it", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            warnings.forEach {
                                Text("• $it", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { doSave(confirmed = warnings.isNotEmpty()) },
                enabled = !isSaving
            ) {
                Text(if (warnings.isNotEmpty()) "Confirm" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
