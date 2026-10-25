package com.carlens.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlens.domain.calc.CostSummary
import com.carlens.domain.calc.CostView
import com.carlens.domain.calc.DocumentStatus
import com.carlens.domain.calc.FuelEconomyReport
import com.carlens.domain.calc.MaintenanceStatus
import com.carlens.domain.calc.MileageSnapshot
import com.carlens.domain.model.AppSettings
import com.carlens.domain.model.Expense
import com.carlens.domain.model.FuelEntry
import com.carlens.domain.model.MaintenanceRule
import com.carlens.domain.model.OdometerReading
import com.carlens.domain.model.ReminderTargetType
import com.carlens.domain.model.ServiceRecord
import com.carlens.domain.model.ThemeMode
import com.carlens.domain.model.Vehicle
import com.carlens.domain.model.VehicleDocument
import com.carlens.domain.repository.AttachmentRepository
import com.carlens.domain.repository.BackupPreview
import com.carlens.domain.repository.BackupRepository
import com.carlens.domain.repository.DocumentRepository
import com.carlens.domain.repository.ExpenseRepository
import com.carlens.domain.repository.FuelRepository
import com.carlens.domain.repository.MaintenanceRepository
import com.carlens.domain.repository.OdometerRepository
import com.carlens.domain.repository.ServiceRepository
import com.carlens.domain.repository.SettingsRepository
import com.carlens.domain.repository.VehicleRepository
import com.carlens.domain.usecase.CalculateFuelEconomy
import com.carlens.domain.usecase.DefaultMaintenanceRules
import com.carlens.domain.usecase.EvaluateMaintenance
import com.carlens.domain.usecase.EvaluateRenewals
import com.carlens.domain.usecase.ExportBackup
import com.carlens.domain.usecase.RecordMileage
import com.carlens.domain.usecase.RecordService
import com.carlens.domain.usecase.RenewDocument
import com.carlens.domain.usecase.RestoreBackup
import com.carlens.domain.usecase.SaveDocument
import com.carlens.domain.usecase.SaveExpense
import com.carlens.domain.usecase.SaveFuelEntry
import com.carlens.domain.usecase.SaveMaintenanceRule
import com.carlens.domain.usecase.SummarizeCosts
import com.carlens.ui.components.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

sealed interface UiMessage {
    data class Info(val text: String) : UiMessage
    data class UndoFuel(val entry: FuelEntry) : UiMessage
    data class UndoService(val record: ServiceRecord) : UiMessage
    data class UndoExpense(val expense: Expense) : UiMessage
    data class Error(val text: String) : UiMessage
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    val vehicleRepo: VehicleRepository,
    private val fuelRepo: FuelRepository,
    private val serviceRepo: ServiceRepository,
    private val expenseRepo: ExpenseRepository,
    val maintenanceRepo: MaintenanceRepository,
    private val documentRepo: DocumentRepository,
    private val odometerRepo: OdometerRepository,
    val attachmentRepo: AttachmentRepository,
    private val settingsRepo: SettingsRepository,
    private val backupRepo: BackupRepository,
    val saveFuelEntry: SaveFuelEntry,
    val recordService: RecordService,
    val recordMileage: RecordMileage,
    val saveExpense: SaveExpense,
    val saveMaintenanceRule: SaveMaintenanceRule,
    val saveDocument: SaveDocument,
    val renewDocument: RenewDocument,
    private val calculateFuelEconomy: CalculateFuelEconomy,
    private val evaluateMaintenance: EvaluateMaintenance,
    private val evaluateRenewals: EvaluateRenewals,
    private val summarizeCosts: SummarizeCosts,
    private val exportBackup: ExportBackup,
    private val restoreBackup: RestoreBackup,
    private val clock: Clock
) : ViewModel() {

    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .onEach { _settingsLoaded.value = true }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            AppSettings()
        )

    val vehicles: StateFlow<List<Vehicle>> = vehicleRepo.observeVehicles(includeArchived = false).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allVehicles: StateFlow<List<Vehicle>> = vehicleRepo.observeVehicles(includeArchived = true).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val selectedVehicle: StateFlow<Vehicle?> = combine(vehicles, settings) { vList, s ->
        val found = vList.firstOrNull { it.id == s.selectedVehicleId }
        found ?: vList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val selectedVehicleId: StateFlow<String?> = selectedVehicle.map { it?.id }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val currentMileage: StateFlow<MileageSnapshot> = selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(MileageSnapshot.UNKNOWN)
        else evaluateMaintenance.observeMileage(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MileageSnapshot.UNKNOWN)

    val odometerReadings: StateFlow<List<OdometerReading>> = selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else odometerRepo.observeReadings(id).map { it.sortedByDescending { r -> r.time.epochMillis } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fuelEntries: StateFlow<List<FuelEntry>> = selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else fuelRepo.observeEntries(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fuelEconomy: StateFlow<FuelEconomyReport> = selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(FuelEconomyReport.EMPTY)
        else calculateFuelEconomy.observe(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FuelEconomyReport.EMPTY)

    val maintenanceStatuses: StateFlow<List<MaintenanceStatus>> = selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else evaluateMaintenance.observe(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serviceRecords: StateFlow<List<ServiceRecord>> = selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else serviceRepo.observeRecords(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<DocumentStatus>> = selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else evaluateRenewals.observe(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _costView = MutableStateFlow(CostView.RUNNING_COSTS)
    val costView = _costView.asStateFlow()

    private val _selectedMonth = MutableStateFlow(YearMonth.now(clock))
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    val monthlyCost: StateFlow<CostSummary?> = combine(selectedVehicleId, _selectedMonth, _costView) { id, month, view ->
        Triple(id, month, view)
    }.flatMapLatest { (id, month, view) ->
        if (id == null) flowOf(null)
        else summarizeCosts.observeMonth(id, month, view)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentMonthCost: StateFlow<CostSummary?> = combine(selectedVehicleId, _costView) { id, view ->
        id to view
    }.flatMapLatest { (id, view) ->
        if (id == null) flowOf(null)
        else summarizeCosts.observeMonth(id, YearMonth.now(clock), view)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val monthlyTrend: StateFlow<List<Pair<YearMonth, Long>>> = combine(selectedVehicleId, _selectedMonth, _costView) { id, month, view ->
        Triple(id, month, view)
    }.flatMapLatest { (id, month, view) ->
        if (id == null) flowOf(emptyList())
        else summarizeCosts.observeTrend(id, month, 6, view)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else expenseRepo.observeExpenses(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nextMaintenance: StateFlow<MaintenanceStatus?> = maintenanceStatuses.map { it.firstOrNull() }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val nextRenewal: StateFlow<DocumentStatus?> = documents.map { it.firstOrNull() }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    // Autocomplete & attachment counts
    val stationSuggestions: StateFlow<List<String>> = fuelRepo.observeStations().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val workshopSuggestions: StateFlow<List<String>> = serviceRepo.observeWorkshops().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val taskSuggestions: StateFlow<List<String>> = serviceRepo.observeCommonTasks().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val merchantSuggestions: StateFlow<List<String>> = expenseRepo.observeMerchants().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val attachmentCounts: StateFlow<Map<String, Int>> = attachmentRepo.observeCounts().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
    )

    private val _hasRecoveryBackup = MutableStateFlow(false)
    val hasRecoveryBackup: StateFlow<Boolean> = _hasRecoveryBackup.asStateFlow()

    private val _pendingBackupPreview = MutableStateFlow<BackupPreview?>(null)
    val pendingBackupPreview: StateFlow<BackupPreview?> = _pendingBackupPreview.asStateFlow()

    private val _pendingScreen = MutableStateFlow<Screen?>(null)
    val pendingScreen: StateFlow<Screen?> = _pendingScreen.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message = _message.asStateFlow()

    init {
        viewModelScope.launch {
            val list = vehicleRepo.getVehicles()
            if (list.isEmpty()) {
                val now = clock.millis()
                val defaultVios = Vehicle(
                    id = UUID.randomUUID().toString(),
                    name = "My Vios",
                    make = "Toyota",
                    model = "Vios",
                    year = 2024,
                    plate = "VXX 1234",
                    fuelType = "RON95",
                    tankCapacityMl = 40_000L,
                    archived = false,
                    createdAt = now,
                    updatedAt = now
                )
                vehicleRepo.upsert(defaultVios)
                settingsRepo.setSelectedVehicle(defaultVios.id)

                DefaultMaintenanceRules.create(
                    vehicleId = defaultVios.id,
                    startDate = LocalDate.now(clock),
                    startKm = null,
                    now = now
                ).forEach { maintenanceRepo.saveRule(it) }
            }
            attachmentRepo.pruneOrphans(24 * 3600 * 1000L)
            refreshRecoveryBackupStatus()
        }
    }

    fun currentTimestampMillis(): Long = clock.millis()

    fun refreshRecoveryBackupStatus() {
        viewModelScope.launch {
            _hasRecoveryBackup.value = backupRepo.latestRecoveryBackup() != null
        }
    }

    fun exportBackupArchive(destinationUri: String, password: CharArray?) {
        viewModelScope.launch {
            try {
                exportBackup(destinationUri, password)
                _message.value = UiMessage.Info("Backup exported successfully (.carlens)")
            } catch (e: Exception) {
                _message.value = UiMessage.Error("Export failed: ${e.message}")
            }
        }
    }

    fun inspectBackupArchive(sourceUri: String, password: CharArray?) {
        viewModelScope.launch {
            try {
                _pendingBackupPreview.value = restoreBackup.inspect(sourceUri, password)
            } catch (e: Exception) {
                _message.value = UiMessage.Error("Inspect failed: ${e.message}")
            }
        }
    }

    fun inspectLatestRecoveryBackup() {
        viewModelScope.launch {
            try {
                val file = backupRepo.latestRecoveryBackup()
                if (file != null) {
                    _pendingBackupPreview.value = restoreBackup.inspect(Uri.fromFile(file).toString(), null)
                } else {
                    _message.value = UiMessage.Info("No recovery backup found")
                }
            } catch (e: Exception) {
                _message.value = UiMessage.Error("Recovery inspect failed: ${e.message}")
            }
        }
    }

    fun confirmBackupRestore() {
        val preview = _pendingBackupPreview.value ?: return
        _pendingBackupPreview.value = null
        viewModelScope.launch {
            try {
                restoreBackup.restore(preview.token)
                refreshRecoveryBackupStatus()
                _message.value = UiMessage.Info("Restored backup successfully (${preview.vehicleNames.size} vehicles)")
            } catch (e: Exception) {
                _message.value = UiMessage.Error("Restore error: ${e.message}")
            }
        }
    }

    fun discardBackupPreview() {
        val preview = _pendingBackupPreview.value ?: return
        _pendingBackupPreview.value = null
        viewModelScope.launch {
            restoreBackup.discard(preview.token)
        }
    }

    fun exportFuelCsv(destinationUri: String) {
        val vehicleId = selectedVehicle.value?.id ?: return
        viewModelScope.launch {
            try {
                backupRepo.exportFuelCsv(vehicleId, destinationUri)
                _message.value = UiMessage.Info("Fuel CSV exported successfully")
            } catch (e: Exception) {
                _message.value = UiMessage.Error("CSV export failed: ${e.message}")
            }
        }
    }

    fun exportExpensesCsv(destinationUri: String) {
        val vehicleId = selectedVehicle.value?.id ?: return
        viewModelScope.launch {
            try {
                backupRepo.exportExpensesCsv(vehicleId, destinationUri)
                _message.value = UiMessage.Info("Expenses CSV exported successfully")
            } catch (e: Exception) {
                _message.value = UiMessage.Error("CSV export failed: ${e.message}")
            }
        }
    }

    fun handleNotificationDeepLink(vehicleId: String?, targetType: String?) {
        viewModelScope.launch {
            if (!vehicleId.isNullOrBlank()) {
                settingsRepo.setSelectedVehicle(vehicleId)
            }
            val dest = when (targetType) {
                ReminderTargetType.MAINTENANCE.name -> Screen.CARE
                ReminderTargetType.DOCUMENT.name -> Screen.GARAGE
                else -> null
            }
            if (dest != null) {
                _pendingScreen.value = dest
            }
        }
    }

    fun consumePendingScreen() {
        _pendingScreen.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    fun showInfo(text: String) {
        _message.value = UiMessage.Info(text)
    }

    fun setCostView(view: CostView) {
        _costView.value = view
    }

    fun selectMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun selectVehicle(v: Vehicle) {
        viewModelScope.launch {
            settingsRepo.setSelectedVehicle(v.id)
        }
    }

    fun setVehicleArchived(v: Vehicle, archived: Boolean) {
        viewModelScope.launch {
            vehicleRepo.setArchived(v.id, archived)
            _message.value = UiMessage.Info(
                if (archived) "Archived ${v.name}" else "Restored ${v.name} to active garage"
            )
        }
    }

    fun deleteFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            fuelRepo.delete(entry.id)
            _message.value = UiMessage.UndoFuel(entry)
        }
    }

    fun restoreFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            fuelRepo.restore(entry)
        }
    }

    fun deleteServiceRecord(record: ServiceRecord) {
        viewModelScope.launch {
            serviceRepo.delete(record.id)
            _message.value = UiMessage.UndoService(record)
        }
    }

    fun restoreServiceRecord(record: ServiceRecord) {
        viewModelScope.launch {
            serviceRepo.restore(record)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepo.delete(expense.id)
            _message.value = UiMessage.UndoExpense(expense)
        }
    }

    fun restoreExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepo.restore(expense)
        }
    }

    fun deleteDocument(doc: VehicleDocument) {
        viewModelScope.launch {
            documentRepo.delete(doc.id)
            _message.value = UiMessage.Info("Deleted ${doc.title}")
        }
    }

    fun deleteMaintenanceRule(rule: MaintenanceRule) {
        viewModelScope.launch {
            maintenanceRepo.deleteRule(rule.id)
            _message.value = UiMessage.Info("Deleted schedule: ${rule.task}")
        }
    }

    fun deleteManualReading(reading: OdometerReading) {
        viewModelScope.launch {
            odometerRepo.deleteManualReading(reading.id)
            _message.value = UiMessage.Info("Deleted manual reading (${reading.km} km)")
        }
    }

    fun snoozeMaintenance(rule: MaintenanceRule, days: Long?) {
        viewModelScope.launch {
            val until = days?.let { LocalDate.now(clock).plusDays(it) }
            maintenanceRepo.snooze(rule.id, until)
            _message.value = UiMessage.Info(
                if (days != null) "Snoozed ${rule.task} for $days days"
                else "Unsnoozed ${rule.task}"
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    fun setHidePlate(v: Boolean) {
        viewModelScope.launch { settingsRepo.setHidePlate(v) }
    }

    fun setHideAmounts(v: Boolean) {
        viewModelScope.launch { settingsRepo.setHideAmounts(v) }
    }

    fun setBiometricLock(v: Boolean) {
        viewModelScope.launch { settingsRepo.setBiometricLock(v) }
    }

    fun setHideNotifDetails(v: Boolean) {
        viewModelScope.launch { settingsRepo.setHideNotificationDetails(v) }
    }

    fun setMonthlyBudgetSen(v: Long?) {
        viewModelScope.launch {
            settingsRepo.setMonthlyBudgetSen(v)
            _message.value = UiMessage.Info(
                if (v != null) "Monthly budget updated" else "Monthly budget cleared"
            )
        }
    }
}
