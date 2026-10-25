package com.carlens.ui.dialogs

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
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
import com.carlens.domain.model.AttachmentOwner
import com.carlens.domain.model.DocumentType
import com.carlens.domain.model.VehicleDocument
import com.carlens.domain.repository.AttachmentRepository
import com.carlens.domain.usecase.SaveDocument
import com.carlens.domain.usecase.SaveResult
import com.carlens.ui.components.AttachmentSection
import com.carlens.ui.components.DatePickerField
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

@Composable
fun AddDocumentDialog(
    vehicleId: String,
    docToEdit: VehicleDocument? = null,
    saveDocument: SaveDocument,
    attachmentRepo: AttachmentRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val isEditing = docToEdit != null
    val docId = remember { docToEdit?.id ?: UUID.randomUUID().toString() }

    var selectedType by remember { mutableStateOf(docToEdit?.type ?: DocumentType.INSURANCE) }
    var titleText by remember { mutableStateOf(docToEdit?.title ?: "") }
    var providerText by remember { mutableStateOf(docToEdit?.provider ?: "") }
    var referenceText by remember { mutableStateOf(docToEdit?.reference ?: "") }
    var expiryDate by remember {
        mutableStateOf(docToEdit?.expiryDate ?: LocalDate.now().plusYears(1))
    }
    var warnDaysText by remember { mutableStateOf((docToEdit?.warnDays ?: 30).toString()) }
    var renewalCostText by remember {
        mutableStateOf(docToEdit?.expectedRenewalSen?.let { Units.senToPlain(it) } ?: "")
    }
    var notesText by remember { mutableStateOf(docToEdit?.notes ?: "") }

    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun doSave() {
        val warnDays = warnDaysText.toIntOrNull() ?: 30
        val renewalSen = Units.parseMoneyToSen(renewalCostText)
        val now = System.currentTimeMillis()

        val doc = VehicleDocument(
            id = docId,
            vehicleId = vehicleId,
            type = selectedType,
            title = titleText.trim().ifBlank { selectedType.label },
            provider = providerText.trim(),
            reference = referenceText.trim(),
            startDate = docToEdit?.startDate ?: LocalDate.now(),
            expiryDate = expiryDate,
            warnDays = warnDays,
            expectedRenewalSen = renewalSen,
            notes = notesText.trim(),
            createdAt = docToEdit?.createdAt ?: now,
            updatedAt = now
        )

        isSaving = true
        scope.launch {
            when (val res = saveDocument(doc)) {
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
                text = if (isEditing) "Edit Document / Renewal" else "Add Document / Renewal",
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
                Text(
                    text = "Document Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DocumentType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Title (optional)") },
                    placeholder = { Text(selectedType.label) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = providerText,
                        onValueChange = { providerText = it },
                        label = { Text("Provider / Insurer") },
                        placeholder = { Text("e.g. Etiqa, Allianz, JPJ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = referenceText,
                        onValueChange = { referenceText = it },
                        label = { Text("Policy / Ref No.") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                DatePickerField(
                    label = "Expiry Date",
                    selectedDate = expiryDate,
                    onDateSelected = { expiryDate = it },
                    showQuickRelativeChips = false
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = { expiryDate = LocalDate.now().plusMonths(6) },
                        label = { Text("+6 Months", style = MaterialTheme.typography.labelSmall) }
                    )
                    SuggestionChip(
                        onClick = { expiryDate = LocalDate.now().plusYears(1) },
                        label = { Text("+1 Year", style = MaterialTheme.typography.labelSmall) }
                    )
                    SuggestionChip(
                        onClick = { expiryDate = LocalDate.now().plusYears(2) },
                        label = { Text("+2 Years", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = warnDaysText,
                        onValueChange = { warnDaysText = it },
                        label = { Text("Warn ahead (days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = renewalCostText,
                        onValueChange = { renewalCostText = it },
                        label = { Text("Expected Cost (RM)") },
                        prefix = { Text("RM ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
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
                    ownerType = AttachmentOwner.DOCUMENT,
                    ownerId = docId,
                    attachmentRepo = attachmentRepo
                )

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
