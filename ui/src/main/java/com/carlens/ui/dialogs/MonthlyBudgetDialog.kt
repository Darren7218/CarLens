package com.carlens.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carlens.domain.calc.Units
import java.util.Locale

@Composable
fun MonthlyBudgetDialog(
    currentBudgetSen: Long?,
    onSaveBudgetSen: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var budgetInputText by remember(currentBudgetSen) {
        mutableStateOf(
            if (currentBudgetSen != null && currentBudgetSen > 0L) {
                String.format(Locale.US, "%.2f", currentBudgetSen / 100.0)
            } else ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly Spending Budget") },
        text = {
            Column {
                Text(
                    text = "Set a monthly target for your vehicle expenses. Leave blank to disable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = budgetInputText,
                    onValueChange = { budgetInputText = it },
                    label = { Text("Monthly Budget (RM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val sen = Units.parseMoneyToSen(budgetInputText)
                onSaveBudgetSen(if (sen != null && sen > 0L) sen else null)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if ((currentBudgetSen ?: 0L) > 0L) {
                    TextButton(onClick = {
                        onSaveBudgetSen(null)
                        onDismiss()
                    }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
