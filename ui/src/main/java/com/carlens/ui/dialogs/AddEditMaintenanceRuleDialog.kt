package com.carlens.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carlens.domain.calc.Units
import com.carlens.domain.model.MaintenanceRule
import com.carlens.domain.usecase.SaveMaintenanceRule
import com.carlens.domain.usecase.SaveResult
import com.carlens.ui.components.DatePickerField
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

@Composable
fun AddEditMaintenanceRuleDialog(
    vehicleId: String,
    currentMileageKm: Long?,
    ruleToEdit: MaintenanceRule? = null,
    saveMaintenanceRule: SaveMaintenanceRule,
    onDeleteRule: ((MaintenanceRule) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val isEditing = ruleToEdit != null
    var taskText by remember { mutableStateOf(ruleToEdit?.task ?: "") }
    var intervalMonthsText by remember {
        mutableStateOf(ruleToEdit?.intervalMonths?.toString() ?: "6")
    }
    var intervalKmText by remember {
        mutableStateOf(ruleToEdit?.intervalKm?.toString() ?: "10000")
    }
    var startDate by remember { mutableStateOf(ruleToEdit?.startDate ?: LocalDate.now()) }
    var startKmText by remember {
        mutableStateOf(
            if (isEditing) (ruleToEdit?.startKm?.toString() ?: "")
            else (currentMileageKm?.toString() ?: "")
        )
    }
    var warnDaysText by remember { mutableStateOf((ruleToEdit?.warnDays ?: 30).toString()) }
    var warnKmText by remember { mutableStateOf((ruleToEdit?.warnKm ?: 1000L).toString()) }
    var notesText by remember { mutableStateOf(ruleToEdit?.notes ?: "") }

    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun doSave() {
        val now = System.currentTimeMillis()
        val rule = MaintenanceRule(
            id = ruleToEdit?.id ?: UUID.randomUUID().toString(),
            vehicleId = vehicleId,
            task = taskText.trim(),
            intervalMonths = intervalMonthsText.toIntOrNull()?.takeIf { it > 0 },
            intervalKm = Units.parseKm(intervalKmText)?.takeIf { it > 0 },
            startDate = startDate,
            startKm = Units.parseKm(startKmText),
            warnDays = warnDaysText.toIntOrNull()?.coerceAtLeast(0) ?: 30,
            warnKm = Units.parseKm(warnKmText) ?: 1000L,
            snoozedUntil = ruleToEdit?.snoozedUntil,
            active = ruleToEdit?.active ?: true,
            notes = notesText.trim(),
            createdAt = ruleToEdit?.createdAt ?: now,
            updatedAt = now
        )

        isSaving = true
        scope.launch {
            when (val res = saveMaintenanceRule(rule)) {
                is SaveResult.Saved -> {
                    isSaving = false
                    onSuccess()
                }
                is SaveResult.Invalid -> {
                    isSaving = false
                    errors = res.errors
                }
                is SaveResult.NeedsConfirmation -> {
                    isSaving = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Maintenance Rule" else "Add Maintenance Rule",
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
                OutlinedTextField(
                    value = taskText,
                    onValueChange = { taskText = it },
                    label = { Text("Task Name") },
                    placeholder = { Text("e.g. Transmission Fluid (ATF)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = intervalMonthsText,
                        onValueChange = { intervalMonthsText = it },
                        label = { Text("Every (Months)") },
                        placeholder = { Text("e.g. 6") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = intervalKmText,
                        onValueChange = { intervalKmText = it },
                        label = { Text("Or Every (km)") },
                        placeholder = { Text("e.g. 10000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                DatePickerField(
                    label = "Baseline / Last Done Date",
                    selectedDate = startDate,
                    onDateSelected = { startDate = it },
                    showQuickRelativeChips = false
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = startKmText,
                    onValueChange = { startKmText = it },
                    label = { Text("Baseline / Last Done Odometer (km)") },
                    supportingText = { Text("Used until a service completion is recorded") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = warnDaysText,
                        onValueChange = { warnDaysText = it },
                        label = { Text("Warn Days Ahead") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = warnKmText,
                        onValueChange = { warnKmText = it },
                        label = { Text("Warn km Ahead") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes / Spec (optional)") },
                    placeholder = { Text("e.g. Toyota WS ATF, 4L") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isEditing && ruleToEdit != null && onDeleteRule != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            onDeleteRule(ruleToEdit)
                            onDismiss()
                        }
                    ) {
                        Text("Delete Schedule Rule", color = MaterialTheme.colorScheme.error)
                    }
                }

                if (errors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            errors.forEach {
                                Text(
                                    text = "• $it",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { doSave() },
                enabled = !isSaving
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
