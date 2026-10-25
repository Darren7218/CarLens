package com.carlens.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carlens.domain.calc.DueState
import com.carlens.domain.calc.Units
import com.carlens.domain.model.Attachment
import com.carlens.domain.model.AttachmentOwner
import com.carlens.domain.repository.AttachmentRepository
import com.carlens.ui.theme.StatusAmber
import com.carlens.ui.theme.StatusGreen
import com.carlens.ui.theme.StatusMuted
import com.carlens.ui.theme.StatusRed
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

enum class Screen(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    FUEL("Fuel", Icons.Default.LocalGasStation),
    CARE("Care", Icons.Default.Build),
    COSTS("Costs", Icons.Default.Payment),
    GARAGE("Garage", Icons.Default.DirectionsCar)
}

@Composable
fun StatusBadge(state: DueState, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (state) {
        DueState.UP_TO_DATE -> StatusGreen.copy(alpha = 0.15f) to StatusGreen
        DueState.DUE_SOON -> StatusAmber.copy(alpha = 0.20f) to Color(0xFFE65100)
        DueState.DUE -> StatusAmber.copy(alpha = 0.25f) to Color(0xFFD84315)
        DueState.OVERDUE -> StatusRed.copy(alpha = 0.18f) to StatusRed
        DueState.MILEAGE_UNKNOWN -> StatusMuted.copy(alpha = 0.20f) to StatusMuted
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = state.label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PlateBadge(plate: String?, hidePlate: Boolean, modifier: Modifier = Modifier) {
    val displayText = if (plate.isNullOrBlank()) "NO PLATE" else if (hidePlate) "••• ••••" else plate.uppercase()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF151515))
            .border(1.dp, Color(0xFF666666), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = displayText,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
    }
}

@Composable
fun AttachmentCountBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AttachFile,
            contentDescription = "Attachments",
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun MoneyText(
    sen: Long,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge
) {
    Text(
        text = Units.formatRm(sen, hideAmounts),
        style = style,
        modifier = modifier
    )
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    showQuickRelativeChips: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val yesterday = remember { today.minusDays(1) }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedDate.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showDialog = true }
            )
        }

        if (showQuickRelativeChips) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedDate == today,
                    onClick = { onDateSelected(today) },
                    label = { Text("Today") }
                )
                FilterChip(
                    selected = selectedDate == yesterday,
                    onClick = { onDateSelected(yesterday) },
                    label = { Text("Yesterday") }
                )
                AssistChip(
                    onClick = { showDialog = true },
                    label = { Text("Custom Date…") }
                )
            }
        }
    }

    if (showDialog) {
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onDateSelected(picked)
                        }
                        showDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun SuggestionChipsRow(
    suggestions: List<String>,
    currentValue: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = remember(suggestions, currentValue) {
        val q = currentValue.trim()
        if (q.isEmpty()) suggestions.take(6)
        else suggestions.filter { it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true) }.take(6)
    }
    if (filtered.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        filtered.forEach { item ->
            SuggestionChip(
                onClick = { onSelect(item) },
                label = { Text(item, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

@Composable
fun AttachmentSection(
    ownerType: AttachmentOwner,
    ownerId: String,
    attachmentRepo: AttachmentRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val attachments by remember(ownerType, ownerId) {
        attachmentRepo.observe(ownerType, ownerId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (success && file != null && file.exists()) {
            scope.launch {
                runCatching {
                    attachmentRepo.importLocalFile(ownerType, ownerId, file, "image/jpeg")
                    file.delete()
                }.onFailure {
                    Toast.makeText(context, "Could not save photo: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    attachmentRepo.import(ownerType, ownerId, uri.toString())
                }.onFailure {
                    Toast.makeText(context, "Could not attach file: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun openAttachment(att: Attachment) {
        val file = attachmentRepo.fileFor(att)
        if (!file.exists()) {
            Toast.makeText(context, "Attachment file not found on device", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, att.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, att.displayName))
        }.onFailure {
            Toast.makeText(context, "No app available to open ${att.mimeType}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Receipts & Attachments (${attachments.size})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        runCatching {
                            val f = attachmentRepo.newCameraFile()
                            pendingCameraFile = f
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                            takePictureLauncher.launch(uri)
                        }.onFailure {
                            Toast.makeText(context, "Camera error: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Camera")
            }

            OutlinedButton(
                onClick = { pickFileLauncher.launch("*/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Attach File")
            }
        }

        if (attachments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            attachments.forEach { att ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable { openAttachment(att) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = att.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${att.sizeBytes / 1024} KB · Tap to view",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = { scope.launch { attachmentRepo.delete(att.id) } },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove attachment",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarLensScaffold(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    onAddFuelClick: () -> Unit,
    onAddServiceClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onUpdateMileageClick: () -> Unit,
    content: @Composable () -> Unit
) {
    var showActionSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Screen.values().forEach { screen ->
                    val selected = currentScreen == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onScreenSelected(screen) },
                        icon = {
                            Icon(imageVector = screen.icon, contentDescription = screen.title)
                        },
                        label = { Text(screen.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showActionSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add record")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            content()
        }

        if (showActionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showActionSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Quick Add",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    QuickActionRow(
                        title = "Add Fuel",
                        subtitle = "Record odometer, litres, and amount paid",
                        icon = Icons.Default.LocalGasStation,
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showActionSheet = false
                                onAddFuelClick()
                            }
                        }
                    )

                    QuickActionRow(
                        title = "Record Service",
                        subtitle = "Maintenance, repairs, parts, and workshop bill",
                        icon = Icons.Default.Build,
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showActionSheet = false
                                onAddServiceClick()
                            }
                        }
                    )

                    QuickActionRow(
                        title = "Add Expense",
                        subtitle = "Tolls, parking, road tax, or insurance",
                        icon = Icons.Default.Payment,
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showActionSheet = false
                                onAddExpenseClick()
                            }
                        }
                    )

                    QuickActionRow(
                        title = "Update Mileage",
                        subtitle = "Log odometer reading without creating an expense",
                        icon = Icons.Default.Speed,
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showActionSheet = false
                                onUpdateMileageClick()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
