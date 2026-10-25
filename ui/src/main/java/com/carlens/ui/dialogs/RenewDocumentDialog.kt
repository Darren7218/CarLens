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
import com.carlens.domain.model.VehicleDocument
import com.carlens.domain.usecase.RenewDocument
import com.carlens.domain.usecase.SaveResult
import com.carlens.ui.components.DatePickerField
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun RenewDocumentDialog(
    document: VehicleDocument,
    renewDocument: RenewDocument,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val baseDate = remember(document) {
        val exp = document.expiryDate
        if (exp != null && exp.isAfter(LocalDate.now())) exp else LocalDate.now()
    }
    var newExpiryDate by remember { mutableStateOf(baseDate.plusYears(1)) }
    var paidAmountText by remember {
        mutableStateOf(document.expectedRenewalSen?.let { Units.senToPlain(it) } ?: "")
    }
    var paidOnDate by remember { mutableStateOf(LocalDate.now()) }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun doRenew() {
        val paidSen = if (paidAmountText.isBlank()) null else Units.parseMoneyToSen(paidAmountText)
        if (paidAmountText.isNotBlank() && (paidSen == null || paidSen <= 0)) {
            errors = listOf("Enter a valid amount in RM, or leave it blank.")
            return
        }

        isSaving = true
        scope.launch {
            when (val res = renewDocument(document, newExpiryDate, paidSen, paidOnDate)) {
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
                text = "Renew ${document.title}",
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
                    text = "Roll over this document to its new expiry date and optionally log the renewal payment as an expense.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                DatePickerField(
                    label = "New Expiry Date",
                    selectedDate = newExpiryDate,
                    onDateSelected = { newExpiryDate = it },
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
                        onClick = { newExpiryDate = baseDate.plusMonths(6) },
                        label = { Text("+6 Months", style = MaterialTheme.typography.labelSmall) }
                    )
                    SuggestionChip(
                        onClick = { newExpiryDate = baseDate.plusYears(1) },
                        label = { Text("+1 Year", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = paidAmountText,
                    onValueChange = { paidAmountText = it },
                    label = { Text("Renewal Amount Paid (RM, optional)") },
                    supportingText = { Text("Leave blank if you don't want to create an expense") },
                    prefix = { Text("RM ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (paidAmountText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DatePickerField(
                        label = "Payment Date",
                        selectedDate = paidOnDate,
                        onDateSelected = { paidOnDate = it }
                    )
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
                onClick = { doRenew() },
                enabled = !isSaving
            ) {
                Text("Confirm Renewal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
