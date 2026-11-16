package com.carlens.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.carlens.domain.repository.BackupPreview
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ExportBackupDialog(
    onConfirm: (password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var passwordText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export CarLens Backup") },
        text = {
            PasswordField(
                value = passwordText,
                onValueChange = { passwordText = it },
                label = "Password (optional)",
                supportingText = "Leave blank for unencrypted .carlens archive"
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(passwordText) }) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun InspectBackupDialog(
    onConfirm: (password: CharArray?) -> Unit,
    onDismiss: () -> Unit
) {
    var passwordText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inspect Backup Archive") },
        text = {
            PasswordField(
                value = passwordText,
                onValueChange = { passwordText = it },
                label = "Password",
                supportingText = "This archive is encrypted. Enter its password to preview contents before restoring."
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val pw = if (passwordText.isBlank()) null else passwordText.toCharArray()
                passwordText = ""
                onConfirm(pw)
            }) {
                Text("Inspect Archive")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                passwordText = ""
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = { Text(supportingText) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ConfirmRestoreDialog(
    preview: BackupPreview,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val exportedDate = remember(preview.createdAt) {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(preview.createdAt))
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Confirm Backup Restore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Exported: $exportedDate · ${if (preview.encrypted) "Encrypted archive" else "Standard archive"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val vehicleSummary = preview.vehicleNames.joinToString(", ").ifBlank { "None" }
                Text(
                    text = "Vehicles (${preview.vehicleNames.size}): $vehicleSummary",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                preview.counts.forEach { (key, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = key, style = MaterialTheme.typography.bodySmall)
                        Text(text = "$count", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
                if (preview.attachmentCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Attachments", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${preview.attachmentCount} (${formatByteSize(preview.attachmentBytes)})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A safety recovery backup of your current data will be saved automatically before replacing.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Restore Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

private fun formatByteSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "${bytes / 1024L} KB"
    else -> "$bytes B"
}
