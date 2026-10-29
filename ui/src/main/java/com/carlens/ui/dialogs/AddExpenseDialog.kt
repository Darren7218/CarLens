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
import com.carlens.domain.model.EventTime
import com.carlens.domain.model.Expense
import com.carlens.domain.model.ExpenseCategory
import com.carlens.domain.repository.AttachmentRepository
import com.carlens.domain.usecase.SaveExpense
import com.carlens.domain.usecase.SaveResult
import com.carlens.ui.components.AttachmentSection
import com.carlens.ui.components.DatePickerField
import com.carlens.ui.components.SuggestionChipsRow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

@Composable
fun AddExpenseDialog(
    vehicleId: String,
    merchantSuggestions: List<String> = emptyList(),
    expenseToEdit: Expense? = null,
    saveExpense: SaveExpense,
    attachmentRepo: AttachmentRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val isEditing = expenseToEdit != null
    val expenseId = remember { expenseToEdit?.id ?: UUID.randomUUID().toString() }
    val tempOwnerId = remember { expenseToEdit?.id ?: "draft_expense_${UUID.randomUUID()}" }

    var selectedDate by remember { mutableStateOf(expenseToEdit?.time?.localDate ?: LocalDate.now()) }
    var amountText by remember {
        mutableStateOf(expenseToEdit?.amountSen?.let { Units.senToPlain(it) } ?: "")
    }
    var selectedCategory by remember { mutableStateOf(expenseToEdit?.category ?: ExpenseCategory.TOLL) }
    var merchantText by remember { mutableStateOf(expenseToEdit?.merchant ?: "") }
    var notesText by remember { mutableStateOf(expenseToEdit?.notes ?: "") }

    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var warnings by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val standaloneCategories = remember {
        listOf(
            ExpenseCategory.TOLL,
            ExpenseCategory.PARKING,
            ExpenseCategory.CLEANING,
            ExpenseCategory.INSURANCE,
            ExpenseCategory.ROAD_TAX,
            ExpenseCategory.ACCESSORIES,
            ExpenseCategory.FINES,
            ExpenseCategory.LOAN_PAYMENT,
            ExpenseCategory.OTHER
        )
    }

    fun resolveEventTime(): EventTime {
        if (expenseToEdit != null && selectedDate == expenseToEdit.time.localDate) {
            return expenseToEdit.time
        }
        return if (selectedDate == LocalDate.now()) EventTime.now() else EventTime.ofDate(selectedDate)
    }

    fun doSave(confirmed: Boolean) {
        val sen = Units.parseMoneyToSen(amountText)
        if (sen == null || sen <= 0) {
            errors = listOf("Please enter an amount in RM.")
            return
        }

        val now = System.currentTimeMillis()
        val expense = Expense(
            id = expenseId,
            vehicleId = vehicleId,
            time = resolveEventTime(),
            amountSen = sen,
            currency = "MYR",
            category = selectedCategory,
            merchant = merchantText.trim(),
            notes = notesText.trim(),
            documentId = expenseToEdit?.documentId,
            createdAt = expenseToEdit?.createdAt ?: now,
            updatedAt = now
        )

        isSaving = true
        scope.launch {
            val res = saveExpense(expense, confirmed = confirmed)
            isSaving = false
            when (res) {
                is SaveResult.Saved -> {
                    if (!isEditing) {
                        attachmentRepo.reassign(tempOwnerId, expenseId)
                    }
                    onSuccess()
                }
                is SaveResult.NeedsConfirmation -> warnings = res.warnings
                is SaveResult.Invalid -> errors = res.errors
            }
        }
    }

    fun handleDismiss() {
        scope.launch {
            if (!isEditing) {
                attachmentRepo.deleteForOwner(tempOwnerId)
            }
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = { handleDismiss() },
        title = {
            Text(
                text = if (isEditing) "Edit Expense" else "Add Expense",
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
                    label = "Expense Date",
                    selectedDate = selectedDate,
                    onDateSelected = {
                        selectedDate = it
                        warnings = emptyList()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        warnings = emptyList()
                    },
                    label = { Text("Amount (RM)") },
                    prefix = { Text("RM ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Category (${if (selectedCategory.isRunningCost) "Running Cost" else "Cash Outflow Only"})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    standaloneCategories.chunked(3).forEach { rowCats ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowCats.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat.label) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text("Merchant / Location") },
                    placeholder = { Text("e.g. PLUS Highway, Sunway Pyramid") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                SuggestionChipsRow(
                    suggestions = merchantSuggestions,
                    currentValue = merchantText,
                    onSelect = { merchantText = it },
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                AttachmentSection(
                    ownerType = AttachmentOwner.EXPENSE,
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
            TextButton(onClick = { handleDismiss() }) {
                Text("Cancel")
            }
        }
    )
}
