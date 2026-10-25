package com.carlens.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carlens.domain.calc.Units
import com.carlens.domain.model.Expense
import com.carlens.domain.model.FuelEntry
import com.carlens.domain.model.MaintenanceRule
import com.carlens.domain.model.ServiceRecord
import com.carlens.domain.model.Vehicle
import com.carlens.domain.model.VehicleDocument
import com.carlens.domain.repository.BackupPreview
import com.carlens.ui.care.CareScreen
import com.carlens.ui.components.CarLensScaffold
import com.carlens.ui.components.Screen
import com.carlens.ui.costs.CostsScreen
import com.carlens.ui.dialogs.AddDocumentDialog
import com.carlens.ui.dialogs.AddEditMaintenanceRuleDialog
import com.carlens.ui.dialogs.AddExpenseDialog
import com.carlens.ui.dialogs.AddFuelDialog
import com.carlens.ui.dialogs.AddServiceDialog
import com.carlens.ui.dialogs.OdometerHistoryDialog
import com.carlens.ui.dialogs.RenewDocumentDialog
import com.carlens.ui.dialogs.UpdateMileageDialog
import com.carlens.ui.dialogs.VehicleEditDialog
import com.carlens.ui.fuel.FuelScreen
import com.carlens.ui.garage.GarageScreen
import com.carlens.ui.home.HomeScreen
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CarLensAppView(
    viewModel: MainViewModel
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val allVehicles by viewModel.allVehicles.collectAsStateWithLifecycle()
    val selectedVehicle by viewModel.selectedVehicle.collectAsStateWithLifecycle()
    val mileage by viewModel.currentMileage.collectAsStateWithLifecycle()
    val odometerReadings by viewModel.odometerReadings.collectAsStateWithLifecycle()
    val fuelEntries by viewModel.fuelEntries.collectAsStateWithLifecycle()
    val fuelEconomy by viewModel.fuelEconomy.collectAsStateWithLifecycle()
    val maintenanceStatuses by viewModel.maintenanceStatuses.collectAsStateWithLifecycle()
    val serviceRecords by viewModel.serviceRecords.collectAsStateWithLifecycle()
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val monthlyCost by viewModel.monthlyCost.collectAsStateWithLifecycle()
    val currentMonthCost by viewModel.currentMonthCost.collectAsStateWithLifecycle()
    val monthlyTrend by viewModel.monthlyTrend.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val costView by viewModel.costView.collectAsStateWithLifecycle()
    val nextMaintenance by viewModel.nextMaintenance.collectAsStateWithLifecycle()
    val nextRenewal by viewModel.nextRenewal.collectAsStateWithLifecycle()
    val stationSuggestions by viewModel.stationSuggestions.collectAsStateWithLifecycle()
    val workshopSuggestions by viewModel.workshopSuggestions.collectAsStateWithLifecycle()
    val taskSuggestions by viewModel.taskSuggestions.collectAsStateWithLifecycle()
    val merchantSuggestions by viewModel.merchantSuggestions.collectAsStateWithLifecycle()
    val attachmentCounts by viewModel.attachmentCounts.collectAsStateWithLifecycle()
    val hasRecoveryBackup by viewModel.hasRecoveryBackup.collectAsStateWithLifecycle()
    val pendingScreen by viewModel.pendingScreen.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    // Deep-link screen navigation
    LaunchedEffect(pendingScreen) {
        val target = pendingScreen ?: return@LaunchedEffect
        currentScreen = target
        viewModel.consumePendingScreen()
    }

    // Dialog flags & edit targets
    var showAddFuelDialog by remember { mutableStateOf(false) }
    var fuelToEdit by remember { mutableStateOf<FuelEntry?>(null) }

    var showUpdateMileageDialog by remember { mutableStateOf(false) }
    var showOdometerHistoryDialog by remember { mutableStateOf(false) }

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }

    var showAddServiceDialog by remember { mutableStateOf(false) }
    var servicePreSelectedRule by remember { mutableStateOf<MaintenanceRule?>(null) }
    var serviceToEdit by remember { mutableStateOf<ServiceRecord?>(null) }

    var showRuleDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<MaintenanceRule?>(null) }

    var showAddDocumentDialog by remember { mutableStateOf(false) }
    var documentToEdit by remember { mutableStateOf<VehicleDocument?>(null) }
    var documentToRenew by remember { mutableStateOf<VehicleDocument?>(null) }

    var showVehicleEditDialog by remember { mutableStateOf(false) }
    var vehicleToEdit by remember { mutableStateOf<Vehicle?>(null) }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInputText by remember { mutableStateOf("") }

    // Export/Restore backup states
    var exportPasswordDialog by remember { mutableStateOf(false) }
    var restorePasswordDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingBackupPreview by remember { mutableStateOf<BackupPreview?>(null) }
    var exportPasswordText by remember { mutableStateOf("") }
    var restorePasswordText by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // SAF file pickers
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val pw = if (exportPasswordText.isBlank()) null else exportPasswordText.toCharArray()
        exportPasswordText = ""
        if (uri != null) {
            scope.launch {
                try {
                    viewModel.exportBackup(uri.toString(), pw)
                    snackbarHostState.showSnackbar("Backup exported successfully (.carlens)")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Export failed: ${e.message}")
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            restorePasswordText = ""
            restorePasswordDialog = true
        }
    }

    val exportFuelCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && selectedVehicle != null) {
            scope.launch {
                try {
                    viewModel.backupRepo.exportFuelCsv(selectedVehicle!!.id, uri.toString())
                    snackbarHostState.showSnackbar("Fuel CSV exported successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("CSV export failed: ${e.message}")
                }
            }
        }
    }

    val exportExpensesCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && selectedVehicle != null) {
            scope.launch {
                try {
                    viewModel.backupRepo.exportExpensesCsv(selectedVehicle!!.id, uri.toString())
                    snackbarHostState.showSnackbar("Expenses CSV exported successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("CSV export failed: ${e.message}")
                }
            }
        }
    }

    // Snackbar handling for info & undo
    LaunchedEffect(message) {
        when (val msg = message) {
            is UiMessage.Info -> {
                snackbarHostState.showSnackbar(msg.text)
                viewModel.clearMessage()
            }
            is UiMessage.Error -> {
                snackbarHostState.showSnackbar(msg.text)
                viewModel.clearMessage()
            }
            is UiMessage.UndoFuel -> {
                val result = snackbarHostState.showSnackbar(
                    message = "Fuel entry deleted",
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.restoreFuelEntry(msg.entry)
                }
                viewModel.clearMessage()
            }
            is UiMessage.UndoService -> {
                val result = snackbarHostState.showSnackbar(
                    message = "Service record deleted",
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.restoreServiceRecord(msg.record)
                }
                viewModel.clearMessage()
            }
            is UiMessage.UndoExpense -> {
                val result = snackbarHostState.showSnackbar(
                    message = "Expense deleted",
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.restoreExpense(msg.expense)
                }
                viewModel.clearMessage()
            }
            null -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CarLensScaffold(
            currentScreen = currentScreen,
            onScreenSelected = { currentScreen = it },
            onAddFuelClick = {
                fuelToEdit = null
                showAddFuelDialog = true
            },
            onAddServiceClick = {
                servicePreSelectedRule = null
                serviceToEdit = null
                showAddServiceDialog = true
            },
            onAddExpenseClick = {
                expenseToEdit = null
                showAddExpenseDialog = true
            },
            onUpdateMileageClick = { showUpdateMileageDialog = true }
        ) {
            when (currentScreen) {
                Screen.HOME -> HomeScreen(
                    selectedVehicle = selectedVehicle,
                    allVehicles = vehicles,
                    mileage = mileage,
                    monthlyCost = currentMonthCost,
                    fuelEconomy = fuelEconomy,
                    nextMaintenance = nextMaintenance,
                    nextRenewal = nextRenewal,
                    settings = settings,
                    onAddFuelClick = {
                        fuelToEdit = null
                        showAddFuelDialog = true
                    },
                    onUpdateMileageClick = { showUpdateMileageDialog = true },
                    onViewOdometerHistoryClick = { showOdometerHistoryDialog = true },
                    onSelectVehicleClick = { currentScreen = Screen.GARAGE },
                    onNavigateToCare = { currentScreen = Screen.CARE },
                    onNavigateToCosts = { currentScreen = Screen.COSTS },
                    onNavigateToFuel = { currentScreen = Screen.FUEL },
                    onNavigateToGarage = { currentScreen = Screen.GARAGE },
                    onRenewDocumentClick = { doc -> documentToRenew = doc }
                )

                Screen.FUEL -> FuelScreen(
                    fuelEntries = fuelEntries,
                    fuelEconomy = fuelEconomy,
                    settings = settings,
                    attachmentCounts = attachmentCounts,
                    onEditEntry = { entry ->
                        fuelToEdit = entry
                        showAddFuelDialog = true
                    },
                    onDeleteEntry = { viewModel.deleteFuelEntry(it) },
                    onAddFuelClick = {
                        fuelToEdit = null
                        showAddFuelDialog = true
                    }
                )

                Screen.CARE -> CareScreen(
                    maintenanceStatuses = maintenanceStatuses,
                    serviceRecords = serviceRecords,
                    attachmentCounts = attachmentCounts,
                    settings = settings,
                    onMarkCompleted = { rule ->
                        servicePreSelectedRule = rule
                        serviceToEdit = null
                        showAddServiceDialog = true
                    },
                    onSnoozeRule = { rule, days ->
                        viewModel.snoozeMaintenance(rule, days)
                    },
                    onAddRuleClick = {
                        ruleToEdit = null
                        showRuleDialog = true
                    },
                    onEditRule = { rule ->
                        ruleToEdit = rule
                        showRuleDialog = true
                    },
                    onEditServiceRecord = { record ->
                        servicePreSelectedRule = null
                        serviceToEdit = record
                        showAddServiceDialog = true
                    },
                    onDeleteServiceRecord = { viewModel.deleteServiceRecord(it) },
                    onAddServiceClick = {
                        servicePreSelectedRule = null
                        serviceToEdit = null
                        showAddServiceDialog = true
                    }
                )

                Screen.COSTS -> CostsScreen(
                    monthlyCost = monthlyCost,
                    monthlyTrend = monthlyTrend,
                    selectedMonth = selectedMonth,
                    allExpenses = allExpenses,
                    currentView = costView,
                    settings = settings,
                    attachmentCounts = attachmentCounts,
                    onSelectMonth = { viewModel.selectMonth(it) },
                    onViewChanged = { viewModel.setCostView(it) },
                    onEditExpense = { expense ->
                        when {
                            expense.fuelEntryId != null -> {
                                val linkedFuel = fuelEntries.find { it.id == expense.fuelEntryId }
                                if (linkedFuel != null) {
                                    fuelToEdit = linkedFuel
                                    showAddFuelDialog = true
                                }
                            }
                            expense.serviceRecordId != null -> {
                                val linkedService = serviceRecords.find { it.id == expense.serviceRecordId }
                                if (linkedService != null) {
                                    servicePreSelectedRule = null
                                    serviceToEdit = linkedService
                                    showAddServiceDialog = true
                                }
                            }
                            else -> {
                                expenseToEdit = expense
                                showAddExpenseDialog = true
                            }
                        }
                    },
                    onDeleteExpense = { viewModel.deleteExpense(it) },
                    onAddExpenseClick = {
                        expenseToEdit = null
                        showAddExpenseDialog = true
                    },
                    onEditBudgetClick = {
                        val currentSen = settings.monthlyBudgetSen
                        budgetInputText = if (currentSen != null && currentSen > 0L) {
                            String.format(java.util.Locale.US, "%.2f", currentSen / 100.0)
                        } else ""
                        showBudgetDialog = true
                    }
                )

                Screen.GARAGE -> GarageScreen(
                    vehicles = vehicles,
                    allVehicles = allVehicles,
                    selectedVehicle = selectedVehicle,
                    documents = documents,
                    settings = settings,
                    attachmentCounts = attachmentCounts,
                    hasRecoveryBackup = hasRecoveryBackup,
                    onSelectVehicle = { viewModel.selectVehicle(it) },
                    onAddVehicleClick = {
                        vehicleToEdit = null
                        showVehicleEditDialog = true
                    },
                    onEditVehicleClick = {
                        vehicleToEdit = it
                        showVehicleEditDialog = true
                    },
                    onToggleArchiveVehicle = { vehicle, archived ->
                        viewModel.setVehicleArchived(vehicle, archived)
                    },
                    onAddDocumentClick = {
                        documentToEdit = null
                        showAddDocumentDialog = true
                    },
                    onEditDocument = { doc ->
                        documentToEdit = doc
                        showAddDocumentDialog = true
                    },
                    onRenewDocument = { doc ->
                        documentToRenew = doc
                    },
                    onDeleteDocument = { viewModel.deleteDocument(it) },
                    onExportBackupClick = {
                        exportPasswordText = ""
                        exportPasswordDialog = true
                    },
                    onRestoreBackupClick = { restoreBackupLauncher.launch(arrayOf("*/*")) },
                    onRestoreRecoveryClick = {
                        scope.launch {
                            try {
                                val file = viewModel.backupRepo.latestRecoveryBackup()
                                if (file != null) {
                                    val preview = viewModel.restoreBackup.inspect(Uri.fromFile(file).toString(), null)
                                    pendingBackupPreview = preview
                                } else {
                                    snackbarHostState.showSnackbar("No recovery backup found")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Recovery inspect failed: ${e.message}")
                            }
                        }
                    },
                    onExportFuelCsvClick = {
                        exportFuelCsvLauncher.launch("carlens_fuel_${selectedVehicle?.name ?: "car"}.csv")
                    },
                    onExportExpensesCsvClick = {
                        exportExpensesCsvLauncher.launch("carlens_expenses_${selectedVehicle?.name ?: "car"}.csv")
                    },
                    onEditBudgetClick = {
                        val currentSen = settings.monthlyBudgetSen
                        budgetInputText = if (currentSen != null && currentSen > 0L) {
                            String.format(java.util.Locale.US, "%.2f", currentSen / 100.0)
                        } else ""
                        showBudgetDialog = true
                    },
                    onThemeModeChanged = { viewModel.setThemeMode(it) },
                    onHidePlateChanged = { viewModel.setHidePlate(it) },
                    onHideAmountsChanged = { viewModel.setHideAmounts(it) },
                    onBiometricLockChanged = { viewModel.setBiometricLock(it) },
                    onHideNotifDetailsChanged = { viewModel.setHideNotifDetails(it) }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Dialogs
        if (showAddFuelDialog && selectedVehicle != null) {
            AddFuelDialog(
                vehicleId = selectedVehicle!!.id,
                lastMileageKm = mileage.currentKm,
                defaultStation = settings.lastStation,
                defaultFuelType = settings.lastFuelType.ifBlank { selectedVehicle!!.fuelType },
                entryToEdit = fuelToEdit,
                stationSuggestions = stationSuggestions,
                saveFuelEntry = viewModel.saveFuelEntry,
                attachmentRepo = viewModel.attachmentRepo,
                onDismiss = {
                    showAddFuelDialog = false
                    fuelToEdit = null
                },
                onSuccess = {
                    showAddFuelDialog = false
                    fuelToEdit = null
                }
            )
        }

        if (showUpdateMileageDialog && selectedVehicle != null) {
            UpdateMileageDialog(
                vehicleId = selectedVehicle!!.id,
                lastMileageKm = mileage.currentKm,
                recordMileage = viewModel.recordMileage,
                onDismiss = { showUpdateMileageDialog = false },
                onSuccess = { showUpdateMileageDialog = false },
                onViewHistoryClick = {
                    showUpdateMileageDialog = false
                    showOdometerHistoryDialog = true
                }
            )
        }

        if (showOdometerHistoryDialog) {
            OdometerHistoryDialog(
                readings = odometerReadings,
                onDeleteManualReading = { viewModel.deleteManualReading(it) },
                onDismiss = { showOdometerHistoryDialog = false }
            )
        }

        if (showAddExpenseDialog && selectedVehicle != null) {
            AddExpenseDialog(
                vehicleId = selectedVehicle!!.id,
                expenseToEdit = expenseToEdit,
                merchantSuggestions = merchantSuggestions,
                saveExpense = viewModel.saveExpense,
                attachmentRepo = viewModel.attachmentRepo,
                onDismiss = {
                    showAddExpenseDialog = false
                    expenseToEdit = null
                },
                onSuccess = {
                    showAddExpenseDialog = false
                    expenseToEdit = null
                }
            )
        }

        if (showAddServiceDialog && selectedVehicle != null) {
            AddServiceDialog(
                vehicleId = selectedVehicle!!.id,
                lastMileageKm = mileage.currentKm,
                availableRules = maintenanceStatuses.map { it.rule },
                initialRule = servicePreSelectedRule,
                recordToEdit = serviceToEdit,
                workshopSuggestions = workshopSuggestions,
                taskSuggestions = taskSuggestions,
                recordService = viewModel.recordService,
                attachmentRepo = viewModel.attachmentRepo,
                onDismiss = {
                    showAddServiceDialog = false
                    servicePreSelectedRule = null
                    serviceToEdit = null
                },
                onSuccess = {
                    showAddServiceDialog = false
                    servicePreSelectedRule = null
                    serviceToEdit = null
                }
            )
        }

        if (showRuleDialog && selectedVehicle != null) {
            AddEditMaintenanceRuleDialog(
                vehicleId = selectedVehicle!!.id,
                currentMileageKm = mileage.currentKm,
                ruleToEdit = ruleToEdit,
                saveMaintenanceRule = viewModel.saveMaintenanceRule,
                onDeleteRule = if (ruleToEdit != null) {
                    { rule ->
                        viewModel.deleteMaintenanceRule(rule)
                        showRuleDialog = false
                        ruleToEdit = null
                    }
                } else null,
                onDismiss = {
                    showRuleDialog = false
                    ruleToEdit = null
                },
                onSuccess = {
                    showRuleDialog = false
                    ruleToEdit = null
                }
            )
        }

        if (showAddDocumentDialog && selectedVehicle != null) {
            AddDocumentDialog(
                vehicleId = selectedVehicle!!.id,
                docToEdit = documentToEdit,
                saveDocument = viewModel.saveDocument,
                attachmentRepo = viewModel.attachmentRepo,
                onDismiss = {
                    showAddDocumentDialog = false
                    documentToEdit = null
                },
                onSuccess = {
                    showAddDocumentDialog = false
                    documentToEdit = null
                }
            )
        }

        val docToRenew = documentToRenew
        if (docToRenew != null) {
            RenewDocumentDialog(
                document = docToRenew,
                renewDocument = viewModel.renewDocument,
                onDismiss = { documentToRenew = null },
                onSuccess = { documentToRenew = null }
            )
        }

        if (showVehicleEditDialog) {
            val vEdit = vehicleToEdit
            VehicleEditDialog(
                vehicle = vEdit,
                vehicleRepo = viewModel.vehicleRepo,
                maintenanceRepo = viewModel.maintenanceRepo,
                onToggleArchive = if (vEdit != null && (vEdit.archived || vehicles.size > 1)) {
                    { v, archived ->
                        viewModel.setVehicleArchived(v, archived)
                        showVehicleEditDialog = false
                        vehicleToEdit = null
                    }
                } else null,
                onDismiss = {
                    showVehicleEditDialog = false
                    vehicleToEdit = null
                },
                onSuccess = {
                    showVehicleEditDialog = false
                    vehicleToEdit = null
                    viewModel.selectVehicle(it)
                }
            )
        }

        if (showBudgetDialog) {
            AlertDialog(
                onDismissRequest = { showBudgetDialog = false },
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
                        viewModel.setMonthlyBudgetSen(if (sen != null && sen > 0L) sen else null)
                        showBudgetDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Row {
                        if ((settings.monthlyBudgetSen ?: 0L) > 0L) {
                            TextButton(onClick = {
                                viewModel.setMonthlyBudgetSen(null)
                                showBudgetDialog = false
                            }) {
                                Text("Clear", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        TextButton(onClick = { showBudgetDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        // Export password prompt
        if (exportPasswordDialog) {
            AlertDialog(
                onDismissRequest = {
                    exportPasswordDialog = false
                    exportPasswordText = ""
                },
                title = { Text("Export CarLens Backup") },
                text = {
                    OutlinedTextField(
                        value = exportPasswordText,
                        onValueChange = { exportPasswordText = it },
                        label = { Text("Password (optional)") },
                        supportingText = { Text("Leave blank for unencrypted .carlens archive") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        exportPasswordDialog = false
                        exportBackupLauncher.launch("carlens_backup_${System.currentTimeMillis()}.carlens")
                    }) {
                        Text("Export")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        exportPasswordDialog = false
                        exportPasswordText = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Step 1: Restore password prompt (inspects archive first)
        if (restorePasswordDialog && pendingRestoreUri != null) {
            AlertDialog(
                onDismissRequest = {
                    restorePasswordDialog = false
                    pendingRestoreUri = null
                    restorePasswordText = ""
                },
                title = { Text("Inspect Backup Archive") },
                text = {
                    OutlinedTextField(
                        value = restorePasswordText,
                        onValueChange = { restorePasswordText = it },
                        label = { Text("Password (if encrypted)") },
                        supportingText = { Text("Next step will preview the archive contents before restoring.") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val uri = pendingRestoreUri!!
                        val pw = if (restorePasswordText.isBlank()) null else restorePasswordText.toCharArray()
                        restorePasswordText = ""
                        restorePasswordDialog = false
                        pendingRestoreUri = null
                        scope.launch {
                            try {
                                val preview = viewModel.restoreBackup.inspect(uri.toString(), pw)
                                pendingBackupPreview = preview
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Inspect failed: ${e.message}")
                            }
                        }
                    }) {
                        Text("Inspect Archive")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        restorePasswordDialog = false
                        pendingRestoreUri = null
                        restorePasswordText = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Step 2: Backup Preview & Confirmation Dialog
        val preview = pendingBackupPreview
        if (preview != null) {
            val exportedDate = remember(preview.createdAt) {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(preview.createdAt))
            }
            AlertDialog(
                onDismissRequest = {
                    val token = preview.token
                    pendingBackupPreview = null
                    scope.launch { viewModel.restoreBackup.discard(token) }
                },
                title = { Text("Confirm Backup Restore") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Exported: $exportedDate",
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
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "A safety recovery backup of your current data will be saved automatically before replacing.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val token = preview.token
                        val vehicleCount = preview.vehicleNames.size
                        pendingBackupPreview = null
                        scope.launch {
                            try {
                                viewModel.restoreBackup.restore(token)
                                viewModel.refreshRecoveryBackupStatus()
                                snackbarHostState.showSnackbar("Restored backup successfully ($vehicleCount vehicles)")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Restore error: ${e.message}")
                            }
                        }
                    }) {
                        Text("Restore Now", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        val token = preview.token
                        pendingBackupPreview = null
                        scope.launch { viewModel.restoreBackup.discard(token) }
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
