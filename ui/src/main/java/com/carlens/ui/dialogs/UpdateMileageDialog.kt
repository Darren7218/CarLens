package com.carlens.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import com.carlens.domain.calc.Units
import com.carlens.domain.model.EventTime
import com.carlens.domain.usecase.RecordMileage
import com.carlens.domain.usecase.SaveResult
import com.carlens.ui.components.DatePickerField
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun UpdateMileageDialog(
    vehicleId: String,
    lastMileageKm: Long?,
    recordMileage: RecordMileage,
    onViewHistoryClick: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var kmText by remember { mutableStateOf("") }
    var newSegment by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var warnings by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun doSave(confirmed: Boolean) {
        val km = Units.parseKm(kmText)
        if (km == null) {
            errors = listOf("Please enter a valid odometer reading.")
            return
        }

        val eventTime = if (selectedDate == LocalDate.now()) EventTime.now() else EventTime.ofDate(selectedDate)

        isSaving = true
        scope.launch {
            val res = recordMileage(
                vehicleId = vehicleId,
                time = eventTime,
                km = km,
                newSegment = newSegment,
                confirmed = confirmed
            )
            isSaving = false
            when (res) {
                is SaveResult.Saved -> onSuccess()
                is SaveResult.NeedsConfirmation -> warnings = res.warnings
                is SaveResult.Invalid -> errors = res.errors
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Update Mileage",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Update your odometer reading to keep maintenance reminders and driving trends accurate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                DatePickerField(
                    label = "Reading Date",
                    selectedDate = selectedDate,
                    onDateSelected = {
                        selectedDate = it
                        warnings = emptyList()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = kmText,
                    onValueChange = {
                        kmText = it
                        warnings = emptyList()
                    },
                    label = { Text("Current Odometer (km)") },
                    supportingText = {
                        if (lastMileageKm != null) {
                            Text("Last recorded: ${Units.formatKm(lastMileageKm)}")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                            "Turn on if odometer cluster was replaced",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = newSegment,
                        onCheckedChange = {
                            newSegment = it
                            warnings = emptyList()
                        }
                    )
                }

                if (onViewHistoryClick != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = onViewHistoryClick) {
                        Text("View Odometer Log & History")
                    }
                }

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
