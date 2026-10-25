package com.carlens.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carlens.domain.model.Expense
import com.carlens.domain.model.FuelEntry
import com.carlens.domain.model.MaintenanceRule
import com.carlens.domain.model.ServiceRecord
import com.carlens.domain.model.Vehicle
import com.carlens.domain.model.VehicleDocument
import com.carlens.ui.care.CareScreen
import com.carlens.ui.components.CarLensScaffold
import com.carlens.ui.components.Screen
import com.carlens.ui.costs.CostsScreen
import com.carlens.ui.dialogs.AddDocumentDialog
import com.carlens.ui.dialogs.AddEditMaintenanceRuleDialog
import com.carlens.ui.dialogs.AddExpenseDialog
import com.carlens.ui.dialogs.AddFuelDialog
import com.carlens.ui.dialogs.AddServiceDialog
import com.carlens.ui.dialogs.ConfirmRestoreDialog
import com.carlens.ui.dialogs.ExportBackupDialog
import com.carlens.ui.dialogs.InspectBackupDialog
import com.carlens.ui.dialogs.MonthlyBudgetDialog
import com.carlens.ui.dialogs.OdometerHistoryDialog
import com.carlens.ui.dialogs.RenewDocumentDialog
import com.carlens.ui.dialogs.UpdateMileageDialog
import com.carlens.ui.dialogs.VehicleEditDialog
import com.carlens.ui.fuel.FuelScreen
import com.carlens.ui.garage.GarageScreen
import com.carlens.ui.home.HomeScreen

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
    val pendingBackupPreview by viewModel.pendingBackupPreview.collectAsStateWithLifecycle()
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

    // Export/Restore backup states
    var exportPasswordDialog by remember { mutableStateOf(false) }
    var restorePasswordDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingExportPassword by remember { mutableStateOf<CharArray?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // SAF file pickers
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val pw = pendingExportPassword
        pendingExportPassword = null
        if (uri != null) {
            viewModel.exportBackupArchive(uri.toString(), pw)
        } else {
            pw?.fill('\u0000')
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            restorePasswordDialog = true
        }
    }

    val exportFuelCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportFuelCsv(uri.toString())
        }
    }

    val exportExpensesCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportExpensesCsv(uri.toString())
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
                    onEditBudgetClick = { showBudgetDialog = true }
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
                    onExportBackupClick = { exportPasswordDialog = true },
                    onRestoreBackupClick = { restoreBackupLauncher.launch(arrayOf("*/*")) },
                    onRestoreRecoveryClick = { viewModel.inspectLatestRecoveryBackup() },
                    onExportFuelCsvClick = {
                        exportFuelCsvLauncher.launch("carlens_fuel_${selectedVehicle?.name ?: "car"}.csv")
                    },
                    onExportExpensesCsvClick = {
                        exportExpensesCsvLauncher.launch("carlens_expenses_${selectedVehicle?.name ?: "car"}.csv")
                    },
                    onEditBudgetClick = { showBudgetDialog = true },
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
        val activeVehicle = selectedVehicle
        if (showAddFuelDialog && activeVehicle != null) {
            AddFuelDialog(
                vehicleId = activeVehicle.id,
                lastMileageKm = mileage.currentKm,
                defaultStation = settings.lastStation,
                defaultFuelType = settings.lastFuelType.ifBlank { activeVehicle.fuelType },
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

        if (showUpdateMileageDialog && activeVehicle != null) {
            UpdateMileageDialog(
                vehicleId = activeVehicle.id,
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

        if (showAddExpenseDialog && activeVehicle != null) {
            AddExpenseDialog(
                vehicleId = activeVehicle.id,
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

        if (showAddServiceDialog && activeVehicle != null) {
            AddServiceDialog(
                vehicleId = activeVehicle.id,
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

        if (showRuleDialog && activeVehicle != null) {
            AddEditMaintenanceRuleDialog(
                vehicleId = activeVehicle.id,
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

        if (showAddDocumentDialog && activeVehicle != null) {
            AddDocumentDialog(
                vehicleId = activeVehicle.id,
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
            MonthlyBudgetDialog(
                currentBudgetSen = settings.monthlyBudgetSen,
                onSaveBudgetSen = { viewModel.setMonthlyBudgetSen(it) },
                onDismiss = { showBudgetDialog = false }
            )
        }

        if (exportPasswordDialog) {
            ExportBackupDialog(
                onConfirm = { pwText ->
                    exportPasswordDialog = false
                    pendingExportPassword = if (pwText.isBlank()) null else pwText.toCharArray()
                    exportBackupLauncher.launch("carlens_backup_${viewModel.currentTimestampMillis()}.carlens")
                },
                onDismiss = { exportPasswordDialog = false }
            )
        }

        val restoreUri = pendingRestoreUri
        if (restorePasswordDialog && restoreUri != null) {
            InspectBackupDialog(
                onConfirm = { pw ->
                    restorePasswordDialog = false
                    pendingRestoreUri = null
                    viewModel.inspectBackupArchive(restoreUri.toString(), pw)
                },
                onDismiss = {
                    restorePasswordDialog = false
                    pendingRestoreUri = null
                }
            )
        }

        val preview = pendingBackupPreview
        if (preview != null) {
            ConfirmRestoreDialog(
                preview = preview,
                onConfirm = { viewModel.confirmBackupRestore() },
                onCancel = { viewModel.discardBackupPreview() }
            )
        }
    }
}
