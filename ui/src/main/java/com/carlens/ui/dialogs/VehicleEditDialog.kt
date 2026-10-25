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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.carlens.domain.model.Vehicle
import com.carlens.domain.repository.MaintenanceRepository
import com.carlens.domain.repository.VehicleRepository
import com.carlens.domain.usecase.DefaultMaintenanceRules
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

@Composable
fun VehicleEditDialog(
    vehicle: Vehicle? = null,
    vehicleRepo: VehicleRepository,
    maintenanceRepo: MaintenanceRepository? = null,
    onToggleArchive: ((Vehicle, Boolean) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSuccess: (Vehicle) -> Unit
) {
    val isEditing = vehicle != null
    var nameText by remember { mutableStateOf(vehicle?.name ?: "My Car") }
    var makeText by remember { mutableStateOf(vehicle?.make ?: "Toyota") }
    var modelText by remember { mutableStateOf(vehicle?.model ?: "Vios") }
    var yearText by remember { mutableStateOf(vehicle?.year?.toString() ?: "2024") }
    var plateText by remember { mutableStateOf(vehicle?.plate ?: "") }
    var fuelTypeText by remember { mutableStateOf(vehicle?.fuelType ?: "RON95") }
    var tankCapacityText by remember {
        mutableStateOf(vehicle?.tankCapacityMl?.let { (it / 1000).toString() } ?: "40")
    }
    var seedDefaultSchedule by remember { mutableStateOf(!isEditing) }

    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun doSave() {
        if (nameText.isBlank()) {
            error = "Please give your vehicle a nickname or name."
            return
        }
        val capacityMl = tankCapacityText.toLongOrNull()?.let { it * 1000L }

        val id = vehicle?.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val updated = Vehicle(
            id = id,
            name = nameText.trim(),
            make = makeText.trim(),
            model = modelText.trim(),
            year = yearText.toIntOrNull(),
            plate = plateText.trim().ifBlank { null },
            fuelType = fuelTypeText.trim().ifBlank { "RON95" },
            tankCapacityMl = capacityMl,
            archived = vehicle?.archived ?: false,
            createdAt = vehicle?.createdAt ?: now,
            updatedAt = now
        )

        isSaving = true
        scope.launch {
            vehicleRepo.upsert(updated)
            if (!isEditing && seedDefaultSchedule && maintenanceRepo != null) {
                DefaultMaintenanceRules.create(
                    vehicleId = updated.id,
                    startDate = LocalDate.now(),
                    startKm = null,
                    now = now
                ).forEach { maintenanceRepo.saveRule(it) }
            }
            isSaving = false
            onSuccess(updated)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (!isEditing) "Add Vehicle" else "Edit Vehicle",
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
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Vehicle Name / Nickname") },
                    placeholder = { Text("e.g. Daily Vios") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = makeText,
                        onValueChange = { makeText = it },
                        label = { Text("Make") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = modelText,
                        onValueChange = { modelText = it },
                        label = { Text("Model") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it },
                        label = { Text("Year") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = plateText,
                        onValueChange = { plateText = it },
                        label = { Text("Plate No.") },
                        placeholder = { Text("e.g. VBP 1234") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fuelTypeText,
                        onValueChange = { fuelTypeText = it },
                        label = { Text("Fuel Type") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tankCapacityText,
                        onValueChange = { tankCapacityText = it },
                        label = { Text("Tank Size (L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                if (!isEditing && maintenanceRepo != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Seed Standard Maintenance Rules",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Oil change, tyre rotation, air filter, brake fluid & spark plugs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = seedDefaultSchedule,
                            onCheckedChange = { seedDefaultSchedule = it }
                        )
                    }
                }

                if (isEditing && vehicle != null && onToggleArchive != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            onToggleArchive(vehicle, !vehicle.archived)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (vehicle.archived) "Restore Vehicle from Archive" else "Archive Vehicle")
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
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
