package com.carlens.domain.usecase

import com.carlens.domain.calc.CostSummarizer
import com.carlens.domain.calc.CostSummary
import com.carlens.domain.calc.CostView
import com.carlens.domain.calc.DocumentStatus
import com.carlens.domain.calc.DueState
import com.carlens.domain.calc.FuelEconomyCalculator
import com.carlens.domain.calc.FuelEconomyReport
import com.carlens.domain.calc.MaintenanceEvaluator
import com.carlens.domain.calc.MaintenanceStatus
import com.carlens.domain.calc.MileageSnapshot
import com.carlens.domain.calc.Units
import com.carlens.domain.model.DocumentType
import com.carlens.domain.model.EventTime
import com.carlens.domain.model.Expense
import com.carlens.domain.model.ExpenseCategory
import com.carlens.domain.model.FuelEntry
import com.carlens.domain.model.FuelEntryInput
import com.carlens.domain.model.MaintenanceRule
import com.carlens.domain.model.ReminderTargetType
import com.carlens.domain.model.ServiceRecordInput
import com.carlens.domain.model.VehicleDocument
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

sealed interface SaveResult {
    data class Saved(val id: String) : SaveResult
    /** Something looks unusual; the user must confirm before saving. Nothing is changed silently. */
    data class NeedsConfirmation(val warnings: List<String>) : SaveResult
    data class Invalid(val errors: List<String>) : SaveResult
}

class SaveFuelEntry @Inject constructor(
    private val fuel: FuelRepository,
    private val expenses: ExpenseRepository,
    private val odometer: OdometerRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(input: FuelEntryInput, confirmed: Boolean): SaveResult {
        val errors = buildList {
            if (input.odometerKm <= 0) add("Enter the odometer reading.")
            if (input.volumeMl <= 0) add("Enter the litres pumped.")
            if (input.totalSen <= 0) add("Enter the total paid.")
        }
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors)

        if (!confirmed) {
            val warnings = warnings(input)
            if (warnings.isNotEmpty()) return SaveResult.NeedsConfirmation(warnings)
        }
        val id = fuel.save(input)
        settings.rememberFuelChoice(input.station, input.fuelType)
        return SaveResult.Saved(id)
    }

    private suspend fun warnings(input: FuelEntryInput): List<String> = buildList {
        // Ignore the reading that belongs to this entry when editing.
        val editingReadingId = input.id?.let { id -> fuel.getEntry(id)?.odometerReadingId }
        val readings = odometer.getReadings(input.vehicleId).filter { it.id != editingReadingId }
        val t = input.time.epochMillis
        if (!input.startsNewSegment) {
            val before = readings.filter { it.time.epochMillis < t }.maxByOrNull { it.time.epochMillis }
            val after = readings.filter { it.time.epochMillis > t }.minByOrNull { it.time.epochMillis }
            if (before != null && input.odometerKm < before.km)
                add("Odometer ${Units.formatKm(input.odometerKm)} is lower than an earlier reading (${Units.formatKm(before.km)}).")
            if (after != null && !after.startsNewSegment && input.odometerKm > after.km)
                add("Odometer ${Units.formatKm(input.odometerKm)} is higher than a later reading (${Units.formatKm(after.km)}).")
            if (before != null && input.odometerKm - before.km > 3000)
                add("That is ${Units.formatKm(input.odometerKm - before.km)} since the previous reading. Did you miss recording a fill-up?")
        }
        val pricePerL = Units.unitPriceMilliSen(input.totalSen, input.volumeMl)
        if (pricePerL != null && (pricePerL < 100_000 || pricePerL > 1_000_000))
            add("Price works out to ${Units.formatUnitPrice(pricePerL)}. Check the amount and litres.")
        val dup = expenses.getExpenses(input.vehicleId).any {
            it.fuelEntryId != null && it.fuelEntryId != input.id &&
                it.time.localDate == input.time.localDate && abs(it.amountSen - input.totalSen) <= input.totalSen / 100
        }
        if (dup) add("A similar fill-up on the same date with the same amount already exists.")
    }
}

class CalculateFuelEconomy @Inject constructor(private val fuel: FuelRepository) {
    fun observe(vehicleId: String): Flow<FuelEconomyReport> =
        fuel.observeEntries(vehicleId).map { FuelEconomyCalculator.calculate(it) }

    operator fun invoke(entries: List<FuelEntry>) = FuelEconomyCalculator.calculate(entries)
}

class RecordService @Inject constructor(
    private val services: ServiceRepository,
    private val expenses: ExpenseRepository,
) {
    suspend operator fun invoke(input: ServiceRecordInput, confirmed: Boolean): SaveResult {
        val allocated = input.items.sumOf { it.costSen ?: 0L }
        val errors = buildList {
            if (input.title.isBlank() && input.items.isEmpty()) add("Describe the work performed or add an item.")
            if (input.totalSen < 0) add("The bill total cannot be negative.")
            if (allocated > input.totalSen)
                add("Item costs (${Units.formatRm(allocated)}) exceed the bill total (${Units.formatRm(input.totalSen)}).")
            if (input.items.any { it.task.isBlank() }) add("Every item needs a task description.")
        }
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors)
        if (!confirmed && input.totalSen > 0) {
            val dup = expenses.getExpenses(input.vehicleId).any {
                it.serviceRecordId != null && it.serviceRecordId != input.id &&
                    it.time.localDate == input.time.localDate && it.amountSen == input.totalSen
            }
            if (dup) return SaveResult.NeedsConfirmation(listOf("A service record with the same date and amount already exists."))
        }
        return SaveResult.Saved(services.save(input))
    }
}

class RecordMileage @Inject constructor(private val odometer: OdometerRepository) {
    suspend operator fun invoke(vehicleId: String, time: EventTime, km: Long, newSegment: Boolean, confirmed: Boolean): SaveResult {
        if (km <= 0) return SaveResult.Invalid(listOf("Enter the odometer reading."))
        if (!confirmed && !newSegment) {
            val readings = odometer.getReadings(vehicleId)
            val before = readings.filter { it.time.epochMillis <= time.epochMillis }.maxByOrNull { it.time.epochMillis }
            if (before != null && km < before.km)
                return SaveResult.NeedsConfirmation(listOf("This is lower than the previous reading (${Units.formatKm(before.km)}). If the odometer was replaced, mark it as a new segment."))
        }
        return SaveResult.Saved(odometer.addManualReading(vehicleId, time, km, newSegment))
    }
}

class EvaluateMaintenance @Inject constructor(
    private val maintenance: MaintenanceRepository,
    private val odometer: OdometerRepository,
    private val clock: Clock,
) {
    fun observeMileage(vehicleId: String): Flow<MileageSnapshot> =
        odometer.observeReadings(vehicleId).map { MaintenanceEvaluator.mileageSnapshot(it, LocalDate.now(clock)) }

    fun observe(vehicleId: String): Flow<List<MaintenanceStatus>> = combine(
        maintenance.observeRules(vehicleId),
        maintenance.observeCompletions(vehicleId),
        odometer.observeReadings(vehicleId),
    ) { rules, completions, readings ->
        val today = LocalDate.now(clock)
        val mileage = MaintenanceEvaluator.mileageSnapshot(readings, today)
        rules.filter { it.active }
            .map { MaintenanceEvaluator.evaluate(it, completions, mileage, today, readings) }.sortedForDisplay()
    }

    suspend fun now(vehicleId: String): List<MaintenanceStatus> {
        val today = LocalDate.now(clock)
        val readings = odometer.getReadings(vehicleId)
        val mileage = MaintenanceEvaluator.mileageSnapshot(readings, today)
        val completions = maintenance.getCompletions(vehicleId)
        return maintenance.getRules(vehicleId).filter { it.active }
            .map { MaintenanceEvaluator.evaluate(it, completions, mileage, today, readings) }.sortedForDisplay()
    }
}

fun List<MaintenanceStatus>.sortedForDisplay() = sortedWith(
    compareByDescending<MaintenanceStatus> { it.state.ordinal }
        .thenBy { minOf(it.daysRemaining ?: Long.MAX_VALUE, it.kmRemaining?.let { km -> km / 30 } ?: Long.MAX_VALUE) },
)

class EvaluateRenewals @Inject constructor(
    private val documents: DocumentRepository,
    private val clock: Clock,
) {
    fun observe(vehicleId: String): Flow<List<DocumentStatus>> = documents.observeDocuments(vehicleId).map { docs ->
        val today = LocalDate.now(clock)
        docs.map { MaintenanceEvaluator.evaluateDocument(it, today) }
            .sortedWith(compareByDescending<DocumentStatus> { it.state.ordinal }.thenBy { it.daysRemaining ?: Long.MAX_VALUE })
    }

    suspend fun now(vehicleId: String): List<DocumentStatus> {
        val today = LocalDate.now(clock)
        return documents.getDocuments(vehicleId).map { MaintenanceEvaluator.evaluateDocument(it, today) }
    }
}

class SummarizeCosts @Inject constructor(
    private val expenses: ExpenseRepository,
    private val odometer: OdometerRepository,
) {
    fun observeMonth(vehicleId: String, month: YearMonth, view: CostView): Flow<CostSummary> =
        combine(expenses.observeExpenses(vehicleId), odometer.observeReadings(vehicleId)) { e, r ->
            CostSummarizer.month(e, r, month, view)
        }

    fun observeAllTime(vehicleId: String, view: CostView): Flow<CostSummary> =
        combine(expenses.observeExpenses(vehicleId), odometer.observeReadings(vehicleId)) { e, r ->
            CostSummarizer.allTime(e, r, view)
        }

    fun observeTrend(vehicleId: String, endMonth: YearMonth, months: Int, view: CostView): Flow<List<Pair<YearMonth, Long>>> =
        expenses.observeExpenses(vehicleId).map { CostSummarizer.monthlyTotals(it, endMonth, months, view) }
}

class ExportBackup @Inject constructor(private val backup: BackupRepository) {
    suspend operator fun invoke(destinationUri: String, password: CharArray?) = backup.export(destinationUri, password)
}

class RestoreBackup @Inject constructor(private val backup: BackupRepository) {
    suspend fun inspect(sourceUri: String, password: CharArray?): BackupPreview = backup.inspect(sourceUri, password)
    suspend fun restore(token: String) = backup.restore(token)
    suspend fun discard(token: String) = backup.discardStaged(token)
}

class SaveExpense @Inject constructor(private val expenses: ExpenseRepository) {
    suspend operator fun invoke(expense: Expense, confirmed: Boolean): SaveResult {
        val errors = buildList {
            if (expense.amountSen <= 0) add("Enter an amount in RM.")
            if (expense.isLinked) add("This expense is managed by its fuel or service record. Edit that record instead.")
        }
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors)
        if (!confirmed) {
            val dup = expenses.getExpenses(expense.vehicleId).any {
                it.id != expense.id && !it.isLinked && it.category == expense.category &&
                    it.time.localDate == expense.time.localDate && it.amountSen == expense.amountSen
            }
            if (dup) return SaveResult.NeedsConfirmation(listOf("A ${expense.category.label.lowercase()} expense with the same date and amount already exists."))
        }
        expenses.save(expense)
        return SaveResult.Saved(expense.id)
    }
}

class SaveMaintenanceRule @Inject constructor(private val maintenance: MaintenanceRepository) {
    suspend operator fun invoke(rule: MaintenanceRule): SaveResult {
        val errors = buildList {
            if (rule.task.isBlank()) add("Name the maintenance task.")
            if ((rule.intervalMonths ?: 0) <= 0 && (rule.intervalKm ?: 0L) <= 0L)
                add("Set a month interval, a kilometre interval, or both.")
            if (rule.warnDays < 0 || rule.warnKm < 0) add("Warning thresholds cannot be negative.")
        }
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors)
        maintenance.saveRule(rule)
        return SaveResult.Saved(rule.id)
    }
}

class SaveDocument @Inject constructor(private val documents: DocumentRepository) {
    suspend operator fun invoke(doc: VehicleDocument): SaveResult {
        val errors = buildList {
            val start = doc.startDate
            val expiry = doc.expiryDate
            if (start != null && expiry != null && !expiry.isAfter(start)) add("The expiry date must be after the start date.")
            if (doc.warnDays < 0) add("Warning days cannot be negative.")
            if ((doc.expectedRenewalSen ?: 0L) < 0L) add("Renewal cost cannot be negative.")
        }
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors)
        documents.save(doc)
        return SaveResult.Saved(doc.id)
    }
}

/**
 * Rolls a document over to a new period and, optionally, records what the renewal cost
 * as an expense linked to the document.
 */
class RenewDocument @Inject constructor(
    private val documents: DocumentRepository,
    private val expenses: ExpenseRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(doc: VehicleDocument, newExpiry: LocalDate, paidSen: Long?, paidOn: LocalDate): SaveResult {
        val newStart = doc.expiryDate ?: LocalDate.now(clock)
        if (!newExpiry.isAfter(newStart) || !newExpiry.isAfter(LocalDate.now(clock)))
            return SaveResult.Invalid(listOf("The new expiry date must be in the future."))
        if (paidSen != null && paidSen <= 0) return SaveResult.Invalid(listOf("Enter the amount paid, or leave it blank."))
        val now = clock.millis()
        documents.save(
            doc.copy(
                startDate = newStart,
                expiryDate = newExpiry,
                expectedRenewalSen = paidSen ?: doc.expectedRenewalSen,
                updatedAt = now,
            ),
        )
        if (paidSen != null) {
            val category = when (doc.type) {
                DocumentType.INSURANCE -> ExpenseCategory.INSURANCE
                DocumentType.ROAD_TAX -> ExpenseCategory.ROAD_TAX
                else -> ExpenseCategory.OTHER
            }
            expenses.save(
                Expense(
                    id = UUID.randomUUID().toString(),
                    vehicleId = doc.vehicleId,
                    time = EventTime.ofDate(paidOn),
                    amountSen = paidSen,
                    category = category,
                    merchant = doc.provider,
                    notes = "Renewal: ${doc.title.ifBlank { doc.type.label }} until $newExpiry",
                    documentId = doc.id,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
        return SaveResult.Saved(doc.id)
    }
}

/** A sensible starting schedule for a new vehicle; baselines default to the first odometer reading. */
object DefaultMaintenanceRules {
    fun create(vehicleId: String, startDate: LocalDate, startKm: Long?, now: Long): List<MaintenanceRule> = listOf(
        Triple("Engine Oil & Filter Change", 6 to 10_000L, "Check the owner's manual for oil grade"),
        Triple("Tyre Rotation & Balancing", 6 to 10_000L, "Rotate front to rear and check pressure"),
        Triple("Air Filter Replacement", 12 to 20_000L, ""),
        Triple("Brake Fluid Replacement", 24 to 40_000L, "DOT 3/4 brake fluid flush"),
        Triple("Spark Plugs", 48 to 80_000L, ""),
    ).map { (task, interval, notes) ->
        MaintenanceRule(
            id = UUID.randomUUID().toString(),
            vehicleId = vehicleId,
            task = task,
            intervalMonths = interval.first,
            intervalKm = interval.second,
            startDate = startDate,
            startKm = startKm,
            warnDays = 30,
            warnKm = 1_000L,
            snoozedUntil = null,
            active = true,
            notes = notes,
            createdAt = now,
            updatedAt = now,
        )
    }
}

/** A reminder that should be shown, with a key that deduplicates by target, due cycle and stage. */
data class ReminderCandidate(
    val key: String,
    val targetType: ReminderTargetType,
    val targetId: String,
    val vehicleId: String,
    val vehicleName: String,
    val title: String,
    val detail: String,
    val state: DueState,
    val dueCycle: String,
    val stage: String,
)

class CollectDueReminders @Inject constructor(
    private val vehicles: VehicleRepository,
    private val maintenance: EvaluateMaintenance,
    private val renewals: EvaluateRenewals,
) {
    suspend operator fun invoke(): List<ReminderCandidate> = buildList {
        for (v in vehicles.getVehicles().filter { !it.archived }) {
            for (s in maintenance.now(v.id)) {
                if (s.snoozed) continue
                if (s.state != DueState.DUE_SOON && s.state != DueState.DUE && s.state != DueState.OVERDUE) continue
                val stage = s.state.name
                add(
                    ReminderCandidate(
                        key = "M:${s.rule.id}:${s.dueCycle}:$stage",
                        targetType = ReminderTargetType.MAINTENANCE, targetId = s.rule.id,
                        vehicleId = v.id, vehicleName = v.name,
                        title = "${s.rule.task} — ${s.state.label.lowercase()}",
                        detail = s.summary, state = s.state, dueCycle = s.dueCycle, stage = stage,
                    ),
                )
            }
            for (d in renewals.now(v.id)) {
                if (d.state == DueState.UP_TO_DATE || d.state == DueState.MILEAGE_UNKNOWN) continue
                val expiry = d.document.expiryDate ?: continue
                val stage = d.state.name
                val days = d.daysRemaining ?: 0
                add(
                    ReminderCandidate(
                        key = "D:${d.document.id}:$expiry:$stage",
                        targetType = ReminderTargetType.DOCUMENT, targetId = d.document.id,
                        vehicleId = v.id, vehicleName = v.name,
                        title = "${d.document.title.ifBlank { d.document.type.label }} " +
                            if (days < 0) "expired" else if (days == 0L) "expires today" else "expires in $days days",
                        detail = "Expiry: $expiry", state = d.state, dueCycle = expiry.toString(), stage = stage,
                    ),
                )
            }
        }
    }
}

/** Snooze presets in days. */
fun snoozeUntil(clock: Clock, days: Long): LocalDate = LocalDate.now(clock).plusDays(days)

fun daysBetween(a: LocalDate, b: LocalDate) = ChronoUnit.DAYS.between(a, b)
