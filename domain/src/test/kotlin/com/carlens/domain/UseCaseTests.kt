package com.carlens.domain

import com.carlens.domain.calc.DueState
import com.carlens.domain.model.AppSettings
import com.carlens.domain.model.DocumentType
import com.carlens.domain.model.EventTime
import com.carlens.domain.model.Expense
import com.carlens.domain.model.ExpenseCategory
import com.carlens.domain.model.FuelEntry
import com.carlens.domain.model.FuelEntryInput
import com.carlens.domain.model.MaintenanceCompletion
import com.carlens.domain.model.MaintenanceRule
import com.carlens.domain.model.OdometerReading
import com.carlens.domain.model.OdometerSource
import com.carlens.domain.model.ReminderTargetType
import com.carlens.domain.model.ServiceItem
import com.carlens.domain.model.ServiceItemInput
import com.carlens.domain.model.ServiceRecord
import com.carlens.domain.model.ServiceRecordInput
import com.carlens.domain.model.TankStatus
import com.carlens.domain.model.ThemeMode
import com.carlens.domain.model.Vehicle
import com.carlens.domain.model.VehicleDocument
import com.carlens.domain.repository.DocumentRepository
import com.carlens.domain.repository.ExpenseRepository
import com.carlens.domain.repository.FuelRepository
import com.carlens.domain.repository.MaintenanceRepository
import com.carlens.domain.repository.OdometerRepository
import com.carlens.domain.repository.ServiceRepository
import com.carlens.domain.repository.SettingsRepository
import com.carlens.domain.repository.VehicleRepository
import com.carlens.domain.usecase.CollectDueReminders
import com.carlens.domain.usecase.DefaultMaintenanceRules
import com.carlens.domain.usecase.EvaluateMaintenance
import com.carlens.domain.usecase.EvaluateRenewals
import com.carlens.domain.usecase.RecordMileage
import com.carlens.domain.usecase.RecordService
import com.carlens.domain.usecase.RenewDocument
import com.carlens.domain.usecase.SaveDocument
import com.carlens.domain.usecase.SaveExpense
import com.carlens.domain.usecase.SaveFuelEntry
import com.carlens.domain.usecase.SaveMaintenanceRule
import com.carlens.domain.usecase.SaveResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

private val TEST_ZONE = ZoneId.of("Asia/Kuala_Lumpur")
private val BASE_DATE = LocalDate.of(2026, 3, 15)
private val FIXED_CLOCK: Clock = Clock.fixed(
    BASE_DATE.atStartOfDay(TEST_ZONE).toInstant(),
    TEST_ZONE
)

private class FakeOdometerRepo : OdometerRepository {
    val readings = mutableListOf<OdometerReading>()
    override fun observeReadings(vehicleId: String): Flow<List<OdometerReading>> =
        MutableStateFlow(readings.filter { it.vehicleId == vehicleId })
    override suspend fun getReadings(vehicleId: String): List<OdometerReading> =
        readings.filter { it.vehicleId == vehicleId }
    override suspend fun addManualReading(
        vehicleId: String,
        time: EventTime,
        km: Long,
        startsNewSegment: Boolean
    ): String {
        val id = "odo_${readings.size + 1}"
        readings += OdometerReading(id, vehicleId, time, km, OdometerSource.MANUAL, startsNewSegment, 0, 0)
        return id
    }
    override suspend fun deleteManualReading(id: String) {
        readings.removeAll { it.id == id }
    }
}

private class FakeExpenseRepo : ExpenseRepository {
    val expenses = mutableListOf<Expense>()
    override fun observeExpenses(vehicleId: String): Flow<List<Expense>> =
        MutableStateFlow(expenses.filter { it.vehicleId == vehicleId })
    override fun observeAllExpenses(): Flow<List<Expense>> = MutableStateFlow(expenses.toList())
    override suspend fun getExpenses(vehicleId: String): List<Expense> =
        expenses.filter { it.vehicleId == vehicleId }
    override suspend fun getExpense(id: String): Expense? = expenses.find { it.id == id }
    override suspend fun save(expense: Expense) {
        expenses.removeAll { it.id == expense.id }
        expenses += expense
    }
    override suspend fun delete(id: String): Expense? {
        val existing = getExpense(id) ?: return null
        expenses.remove(existing)
        return existing
    }
    override suspend fun restore(expense: Expense) = save(expense)
    override fun observeMerchants(): Flow<List<String>> = MutableStateFlow(emptyList())
}

private class FakeFuelRepo(
    private val odoRepo: FakeOdometerRepo,
    private val expRepo: FakeExpenseRepo
) : FuelRepository {
    val entries = mutableListOf<FuelEntry>()
    override fun observeEntries(vehicleId: String): Flow<List<FuelEntry>> =
        MutableStateFlow(entries.filter { it.vehicleId == vehicleId })
    override suspend fun getEntries(vehicleId: String): List<FuelEntry> =
        entries.filter { it.vehicleId == vehicleId }
    override suspend fun getEntry(id: String): FuelEntry? = entries.find { it.id == id }
    override suspend fun save(input: FuelEntryInput): String {
        val id = input.id ?: "fuel_${entries.size + 1}"
        val existing = getEntry(id)
        val odoId = existing?.odometerReadingId ?: "odo_$id"
        val expId = existing?.expenseId ?: "exp_$id"

        odoRepo.readings.removeAll { it.id == odoId }
        odoRepo.readings += OdometerReading(
            id = odoId,
            vehicleId = input.vehicleId,
            time = input.time,
            km = input.odometerKm,
            source = OdometerSource.FUEL,
            startsNewSegment = input.startsNewSegment,
            createdAt = 0,
            updatedAt = 0
        )

        expRepo.expenses.removeAll { it.id == expId }
        expRepo.expenses += Expense(
            id = expId,
            vehicleId = input.vehicleId,
            time = input.time,
            amountSen = input.totalSen,
            category = ExpenseCategory.FUEL,
            merchant = input.station,
            notes = input.notes,
            fuelEntryId = id,
            createdAt = 0,
            updatedAt = 0
        )

        entries.removeAll { it.id == id }
        entries += FuelEntry(
            id = id,
            vehicleId = input.vehicleId,
            time = input.time,
            odometerReadingId = odoId,
            odometerKm = input.odometerKm,
            startsNewSegment = input.startsNewSegment,
            volumeMl = input.volumeMl,
            unitPriceMilliSen = input.unitPriceMilliSen,
            tankStatus = input.tankStatus,
            missedPrevious = input.missedPrevious,
            station = input.station,
            fuelType = input.fuelType,
            notes = input.notes,
            expenseId = expId,
            totalSen = input.totalSen,
            createdAt = 0,
            updatedAt = 0
        )
        return id
    }
    override suspend fun delete(id: String): FuelEntry? = null
    override suspend fun restore(entry: FuelEntry) {}
    override fun observeStations(): Flow<List<String>> = MutableStateFlow(emptyList())
}

private class FakeServiceRepo(private val expRepo: FakeExpenseRepo) : ServiceRepository {
    val records = mutableListOf<ServiceRecord>()
    override fun observeRecords(vehicleId: String): Flow<List<ServiceRecord>> =
        MutableStateFlow(records.filter { it.vehicleId == vehicleId })
    override suspend fun getRecord(id: String): ServiceRecord? = records.find { it.id == id }
    override suspend fun save(input: ServiceRecordInput): String {
        val id = input.id ?: "srv_${records.size + 1}"
        val expId = "exp_$id"
        expRepo.expenses.removeAll { it.id == expId }
        expRepo.expenses += Expense(
            id = expId,
            vehicleId = input.vehicleId,
            time = input.time,
            amountSen = input.totalSen,
            category = input.category,
            merchant = input.workshop,
            notes = input.notes,
            serviceRecordId = id,
            createdAt = 0,
            updatedAt = 0
        )
        records.removeAll { it.id == id }
        records += ServiceRecord(
            id = id,
            vehicleId = input.vehicleId,
            time = input.time,
            odometerReadingId = null,
            odometerKm = input.odometerKm,
            title = input.title,
            workshop = input.workshop,
            notes = input.notes,
            category = input.category,
            expenseId = expId,
            totalSen = input.totalSen,
            items = input.items.mapIndexed { idx, item ->
                ServiceItem(item.id ?: "item_$idx", id, item.task, item.part, item.quantity, item.costSen, item.maintenanceRuleId)
            },
            createdAt = 0,
            updatedAt = 0
        )
        return id
    }
    override suspend fun delete(id: String): ServiceRecord? = null
    override suspend fun restore(record: ServiceRecord) {}
    override fun observeWorkshops(): Flow<List<String>> = MutableStateFlow(emptyList())
    override fun observeCommonTasks(): Flow<List<String>> = MutableStateFlow(emptyList())
}

private class FakeSettingsRepo : SettingsRepository {
    private val state = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = state
    override suspend fun current(): AppSettings = state.value
    override suspend fun setThemeMode(mode: ThemeMode) {}
    override suspend fun setHidePlate(v: Boolean) {}
    override suspend fun setHideAmounts(v: Boolean) {}
    override suspend fun setBiometricLock(v: Boolean) {}
    override suspend fun setHideNotificationDetails(v: Boolean) {}
    override suspend fun setMonthlyBudgetSen(v: Long?) {}
    override suspend fun setSelectedVehicle(id: String?) {}
    override suspend fun rememberFuelChoice(station: String, fuelType: String) {
        state.value = state.value.copy(lastStation = station, lastFuelType = fuelType)
    }
    override suspend fun saveDraft(key: String, json: String?) {}
    override suspend fun loadDraft(key: String): String? = null
}

private class FakeMaintenanceRepo : MaintenanceRepository {
    val rules = mutableListOf<MaintenanceRule>()
    val completions = mutableListOf<MaintenanceCompletion>()
    override fun observeRules(vehicleId: String): Flow<List<MaintenanceRule>> =
        MutableStateFlow(rules.filter { it.vehicleId == vehicleId })
    override suspend fun getRules(vehicleId: String): List<MaintenanceRule> =
        rules.filter { it.vehicleId == vehicleId }
    override suspend fun getRule(id: String): MaintenanceRule? = rules.find { it.id == id }
    override suspend fun saveRule(rule: MaintenanceRule) {
        rules.removeAll { it.id == rule.id }
        rules += rule
    }
    override suspend fun deleteRule(id: String) {
        rules.removeAll { it.id == id }
    }
    override suspend fun snooze(ruleId: String, until: LocalDate?) {}
    override fun observeCompletions(vehicleId: String): Flow<List<MaintenanceCompletion>> =
        MutableStateFlow(completions)
    override suspend fun getCompletions(vehicleId: String): List<MaintenanceCompletion> = completions
}

private class FakeDocumentRepo : DocumentRepository {
    val docs = mutableListOf<VehicleDocument>()
    override fun observeDocuments(vehicleId: String): Flow<List<VehicleDocument>> =
        MutableStateFlow(docs.filter { it.vehicleId == vehicleId })
    override suspend fun getDocuments(vehicleId: String): List<VehicleDocument> =
        docs.filter { it.vehicleId == vehicleId }
    override suspend fun getDocument(id: String): VehicleDocument? = docs.find { it.id == id }
    override suspend fun save(doc: VehicleDocument) {
        docs.removeAll { it.id == doc.id }
        docs += doc
    }
    override suspend fun delete(id: String) {
        docs.removeAll { it.id == id }
    }
}

private class FakeVehicleRepo : VehicleRepository {
    val vehicles = mutableListOf<Vehicle>()
    override fun observeVehicles(includeArchived: Boolean): Flow<List<Vehicle>> =
        MutableStateFlow(vehicles.filter { includeArchived || !it.archived })
    override fun observeVehicle(id: String): Flow<Vehicle?> =
        MutableStateFlow(vehicles.find { it.id == id })
    override suspend fun getVehicle(id: String): Vehicle? = vehicles.find { it.id == id }
    override suspend fun getVehicles(): List<Vehicle> = vehicles.toList()
    override suspend fun upsert(vehicle: Vehicle) {
        vehicles.removeAll { it.id == vehicle.id }
        vehicles += vehicle
    }
    override suspend fun setArchived(id: String, archived: Boolean) {}
}

class UseCaseTests {

    @Test
    fun `SaveFuelEntry validates required positive fields`() = runBlocking {
        val odoRepo = FakeOdometerRepo()
        val expRepo = FakeExpenseRepo()
        val fuelRepo = FakeFuelRepo(odoRepo, expRepo)
        val settingsRepo = FakeSettingsRepo()
        val saveFuel = SaveFuelEntry(fuelRepo, expRepo, odoRepo, settingsRepo)

        val invalidInput = FuelEntryInput(
            vehicleId = "v1",
            time = EventTime.ofDate(BASE_DATE, TEST_ZONE),
            odometerKm = 0,
            volumeMl = -500,
            totalSen = 0,
            unitPriceMilliSen = null,
            tankStatus = TankStatus.FULL,
            missedPrevious = false,
            station = "Petronas",
            fuelType = "RON95",
            notes = ""
        )

        val result = saveFuel(invalidInput, confirmed = false)
        assertTrue(result is SaveResult.Invalid)
        assertEquals(3, (result as SaveResult.Invalid).errors.size)
    }

    @Test
    fun `SaveFuelEntry warns on decreasing odometer and allows editing without self-conflict`() = runBlocking {
        val odoRepo = FakeOdometerRepo()
        val expRepo = FakeExpenseRepo()
        val fuelRepo = FakeFuelRepo(odoRepo, expRepo)
        val settingsRepo = FakeSettingsRepo()
        val saveFuel = SaveFuelEntry(fuelRepo, expRepo, odoRepo, settingsRepo)

        // 1. Save initial entry at 10,000 km on Day 0
        val first = FuelEntryInput(
            id = "f1",
            vehicleId = "v1",
            time = EventTime.ofDate(BASE_DATE, TEST_ZONE),
            odometerKm = 10_000,
            volumeMl = 30_000,
            totalSen = 6150,
            unitPriceMilliSen = 205_000,
            tankStatus = TankStatus.FULL,
            missedPrevious = false,
            station = "Shell",
            fuelType = "RON95",
            notes = ""
        )
        assertTrue(saveFuel(first, confirmed = false) is SaveResult.Saved)

        // 2. Save second entry at 10,450 km on Day 5
        val second = first.copy(
            id = "f2",
            time = EventTime.ofDate(BASE_DATE.plusDays(5), TEST_ZONE),
            odometerKm = 10_450
        )
        assertTrue(saveFuel(second, confirmed = false) is SaveResult.Saved)

        // 3. Edit first entry to 10,100 km (still < 10,450 km): should NOT trigger self-conflict
        val editedFirst = first.copy(odometerKm = 10_100, notes = "Edited")
        val editResult = saveFuel(editedFirst, confirmed = false)
        assertTrue("Editing valid odometer should save without warning", editResult is SaveResult.Saved)

        // 4. Edit first entry to 10,600 km (> later reading 10,450 km): SHOULD warn
        val badEdit = first.copy(odometerKm = 10_600)
        val warnResult = saveFuel(badEdit, confirmed = false)
        assertTrue(warnResult is SaveResult.NeedsConfirmation)
    }

    @Test
    fun `RecordService validates item allocation against bill total`() = runBlocking {
        val expRepo = FakeExpenseRepo()
        val srvRepo = FakeServiceRepo(expRepo)
        val recordService = RecordService(srvRepo, expRepo)

        val overAllocated = ServiceRecordInput(
            vehicleId = "v1",
            time = EventTime.ofDate(BASE_DATE, TEST_ZONE),
            odometerKm = 20_000,
            title = "Major Service",
            workshop = "SC",
            notes = "",
            category = ExpenseCategory.MAINTENANCE,
            totalSen = 20_000, // RM 200
            items = listOf(
                ServiceItemInput(task = "Engine Oil", part = "Synthetic", quantity = 1.0, costSen = 18_000, maintenanceRuleId = null),
                ServiceItemInput(task = "Oil Filter", part = "OEM", quantity = 1.0, costSen = 5_000, maintenanceRuleId = null)
            )
        )

        val result = recordService(overAllocated, confirmed = false)
        assertTrue(result is SaveResult.Invalid)
        assertTrue((result as SaveResult.Invalid).errors.single().contains("exceed the bill total"))
    }

    @Test
    fun `RecordMileage warns on lower reading unless marked as newSegment`() = runBlocking {
        val odoRepo = FakeOdometerRepo()
        val recordMileage = RecordMileage(odoRepo)

        recordMileage("v1", EventTime.ofDate(BASE_DATE, TEST_ZONE), 50_000, newSegment = false, confirmed = false)

        val lowerResult = recordMileage(
            "v1",
            EventTime.ofDate(BASE_DATE.plusDays(1), TEST_ZONE),
            49_500,
            newSegment = false,
            confirmed = false
        )
        assertTrue(lowerResult is SaveResult.NeedsConfirmation)

        val newSegResult = recordMileage(
            "v1",
            EventTime.ofDate(BASE_DATE.plusDays(1), TEST_ZONE),
            100,
            newSegment = true,
            confirmed = false
        )
        assertTrue(newSegResult is SaveResult.Saved)
    }

    @Test
    fun `SaveExpense blocks direct modification of linked expenses and warns on duplicates`() = runBlocking {
        val expRepo = FakeExpenseRepo()
        val saveExpense = SaveExpense(expRepo)

        val linked = Expense(
            id = "e1",
            vehicleId = "v1",
            time = EventTime.ofDate(BASE_DATE, TEST_ZONE),
            amountSen = 5000,
            category = ExpenseCategory.FUEL,
            merchant = "Shell",
            notes = "",
            fuelEntryId = "f1",
            createdAt = 0,
            updatedAt = 0
        )
        assertTrue(saveExpense(linked, confirmed = false) is SaveResult.Invalid)

        val standalone = linked.copy(id = "e2", category = ExpenseCategory.PARKING, fuelEntryId = null)
        assertTrue(saveExpense(standalone, confirmed = false) is SaveResult.Saved)

        val duplicate = standalone.copy(id = "e3")
        assertTrue(saveExpense(duplicate, confirmed = false) is SaveResult.NeedsConfirmation)
    }

    @Test
    fun `RenewDocument rolls over expiry and creates linked insurance expense`() = runBlocking {
        val docRepo = FakeDocumentRepo()
        val expRepo = FakeExpenseRepo()
        val renewDocument = RenewDocument(docRepo, expRepo, FIXED_CLOCK)

        val doc = VehicleDocument(
            id = "doc1",
            vehicleId = "v1",
            type = DocumentType.INSURANCE,
            title = "Comprehensive Motor Insurance",
            provider = "Etiqa",
            reference = "POL-123",
            startDate = BASE_DATE.minusYears(1),
            expiryDate = BASE_DATE,
            warnDays = 30,
            expectedRenewalSen = 120_000,
            notes = "",
            createdAt = 0,
            updatedAt = 0
        )
        docRepo.save(doc)

        val newExpiry = BASE_DATE.plusYears(1)
        val res = renewDocument(doc, newExpiry, paidSen = 115_000, paidOn = BASE_DATE)
        assertTrue(res is SaveResult.Saved)

        val updatedDoc = docRepo.getDocument("doc1")!!
        assertEquals(BASE_DATE, updatedDoc.startDate)
        assertEquals(newExpiry, updatedDoc.expiryDate)
        assertEquals(115_000L, updatedDoc.expectedRenewalSen)

        val createdExpense = expRepo.expenses.single()
        assertEquals(ExpenseCategory.INSURANCE, createdExpense.category)
        assertEquals(115_000L, createdExpense.amountSen)
        assertEquals("doc1", createdExpense.documentId)
    }

    @Test
    fun `CollectDueReminders skips archived vehicles and snoozed rules`() = runBlocking {
        val vehicleRepo = FakeVehicleRepo()
        val maintRepo = FakeMaintenanceRepo()
        val odoRepo = FakeOdometerRepo()
        val docRepo = FakeDocumentRepo()

        vehicleRepo.upsert(
            Vehicle("v_active", "My Vios", "Toyota", "Vios", 2024, "VXX 1234", "RON95", 40_000, archived = false, 0, 0)
        )
        vehicleRepo.upsert(
            Vehicle("v_arch", "Old Saga", "Proton", "Saga", 2015, "WXX 9999", "RON95", 40_000, archived = true, 0, 0)
        )

        val rules = DefaultMaintenanceRules.create("v_active", BASE_DATE.minusMonths(7), startKm = 10_000, now = 0)
        // First rule is 6-month oil change -> DUE
        maintRepo.saveRule(rules[0])
        // Second rule is 6-month tyre rotation, snoozed until next month -> skipped
        maintRepo.saveRule(rules[1].copy(snoozedUntil = BASE_DATE.plusDays(14)))

        docRepo.save(
            VehicleDocument(
                id = "doc_roadtax",
                vehicleId = "v_active",
                type = DocumentType.ROAD_TAX,
                title = "Road Tax",
                provider = "JPJ",
                reference = "",
                startDate = BASE_DATE.minusYears(1),
                expiryDate = BASE_DATE.plusDays(10),
                warnDays = 30,
                expectedRenewalSen = 9000,
                notes = "",
                createdAt = 0,
                updatedAt = 0
            )
        )

        val evalMaint = EvaluateMaintenance(maintRepo, odoRepo, FIXED_CLOCK)
        val evalRenewals = EvaluateRenewals(docRepo, FIXED_CLOCK)
        val collector = CollectDueReminders(vehicleRepo, evalMaint, evalRenewals)

        val candidates = collector()
        assertEquals(2, candidates.size)
        assertTrue(candidates.any { it.targetType == ReminderTargetType.MAINTENANCE && it.targetId == rules[0].id })
        assertTrue(candidates.any { it.targetType == ReminderTargetType.DOCUMENT && it.targetId == "doc_roadtax" && it.state == DueState.DUE_SOON })
    }
}
