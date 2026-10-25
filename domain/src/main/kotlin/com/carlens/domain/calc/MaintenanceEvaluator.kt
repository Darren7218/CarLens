package com.carlens.domain.calc

import com.carlens.domain.model.MaintenanceCompletion
import com.carlens.domain.model.MaintenanceRule
import com.carlens.domain.model.OdometerReading
import com.carlens.domain.model.VehicleDocument
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/** Ordered by severity (lowest first). */
enum class DueState(val label: String) {
    UP_TO_DATE("Up to date"),
    MILEAGE_UNKNOWN("Mileage unknown"),
    DUE_SOON("Due soon"),
    DUE("Due"),
    OVERDUE("Overdue"),
}

data class MileageSnapshot(
    val currentKm: Long?,
    val readingDate: LocalDate?,
    /** Average recent driving, used only for clearly-labelled estimates. */
    val kmPerDay: Double?,
) {
    companion object {
        val UNKNOWN = MileageSnapshot(null, null, null)
    }
}

data class MaintenanceStatus(
    val rule: MaintenanceRule,
    val state: DueState,
    val lastDoneDate: LocalDate,
    val lastDoneKm: Long?,
    val dueDate: LocalDate?,
    val dueKm: Long?,
    val daysRemaining: Long?,
    val kmRemaining: Long?,
    /** Estimated date the mileage threshold will be reached, from recorded driving. */
    val estimatedKmDueDate: LocalDate?,
    val mileageStale: Boolean,
    val snoozed: Boolean,
    /** Identifies this due cycle; changes whenever the task is completed. */
    val dueCycle: String,
) {
    val summary: String
        get() = buildList {
            daysRemaining?.let { add(if (it >= 0) "in $it days" else "${-it} days ago") }
            kmRemaining?.let { add(if (it >= 0) "in ${Units.formatKm(it)}" else "${Units.formatKm(-it)} over") }
        }.joinToString(" · ").ifEmpty { "No threshold set" }
}

data class DocumentStatus(
    val document: VehicleDocument,
    val state: DueState,
    val daysRemaining: Long?,
    /** Expected renewal ÷ remaining months. */
    val monthlySavingsSen: Long?,
)

/**
 * Date-or-mileage maintenance, whichever comes first.
 */
object MaintenanceEvaluator {
    /** Days past the due date before an item is considered overdue rather than due. */
    const val GRACE_DAYS = 14L
    /** Kilometres past the due mileage before an item is considered overdue rather than due. */
    const val GRACE_KM = 500L
    /** A reading older than this cannot be trusted for mileage reminders. */
    const val STALE_READING_DAYS = 60L
    private const val RATE_WINDOW_DAYS = 180L
    private const val MIN_RATE_SPAN_DAYS = 7L

    fun mileageSnapshot(readings: List<OdometerReading>, today: LocalDate): MileageSnapshot {
        if (readings.isEmpty()) return MileageSnapshot.UNKNOWN
        val sorted = readings.sortedWith(compareBy<OdometerReading> { it.time.epochMillis }.thenBy { it.km })
        val latest = sorted.last()
        // Rate only within the current continuity segment.
        val segStart = sorted.indexOfLast { it.startsNewSegment }.coerceAtLeast(0)
        val segment = sorted.subList(segStart, sorted.size)
            .filter { ChronoUnit.DAYS.between(it.time.localDate, today) <= RATE_WINDOW_DAYS }
        var rate: Double? = null
        if (segment.size >= 2) {
            val first = segment.first()
            val span = ChronoUnit.DAYS.between(first.time.localDate, latest.time.localDate)
            val dist = latest.km - first.km
            if (span >= MIN_RATE_SPAN_DAYS && dist > 0) rate = dist.toDouble() / span
        }
        return MileageSnapshot(latest.km, latest.time.localDate, rate)
    }

    fun evaluate(
        rule: MaintenanceRule,
        completions: List<MaintenanceCompletion>,
        mileage: MileageSnapshot,
        today: LocalDate,
        /** Used to infer a baseline odometer when none was recorded. */
        readings: List<OdometerReading> = emptyList(),
    ): MaintenanceStatus {
        val last = completions.filter { it.ruleId == rule.id }
            .maxWithOrNull(compareBy<MaintenanceCompletion> { it.date }.thenBy { it.odometerKm ?: -1 })
        val lastDate = last?.date ?: rule.startDate
        val recordedKm = if (last != null) last.odometerKm else rule.startKm
        val lastKm = recordedKm ?: firstKmOnOrAfter(lastDate, readings)

        val dueDate = rule.intervalMonths?.takeIf { it > 0 }?.let { lastDate.plusMonths(it.toLong()) }
        val dueKm = rule.intervalKm?.takeIf { it > 0 }?.let { iv -> lastKm?.let { it + iv } }

        val days = dueDate?.let { ChronoUnit.DAYS.between(today, it) }
        val dateState = days?.let {
            when {
                it < -GRACE_DAYS -> DueState.OVERDUE
                it <= 0 -> DueState.DUE
                it <= rule.warnDays -> DueState.DUE_SOON
                else -> DueState.UP_TO_DATE
            }
        }

        val stale = mileage.readingDate == null ||
            ChronoUnit.DAYS.between(mileage.readingDate, today) > STALE_READING_DAYS
        val kmRemaining = if (dueKm != null && mileage.currentKm != null) dueKm - mileage.currentKm else null
        val kmState: DueState? = when {
            rule.intervalKm == null || rule.intervalKm <= 0 -> null
            kmRemaining == null -> DueState.MILEAGE_UNKNOWN
            kmRemaining < -GRACE_KM -> DueState.OVERDUE
            kmRemaining <= 0 -> DueState.DUE
            kmRemaining <= rule.warnKm -> DueState.DUE_SOON
            stale -> DueState.MILEAGE_UNKNOWN
            else -> DueState.UP_TO_DATE
        }

        val state = listOfNotNull(dateState, kmState).maxByOrNull { it.ordinal } ?: DueState.UP_TO_DATE

        val estimate = if (kmRemaining != null && kmRemaining > 0 && mileage.kmPerDay != null && mileage.readingDate != null) {
            mileage.readingDate.plusDays(ceil(kmRemaining / mileage.kmPerDay).toLong())
        } else null

        return MaintenanceStatus(
            rule = rule,
            state = state,
            lastDoneDate = lastDate,
            lastDoneKm = lastKm,
            dueDate = dueDate,
            dueKm = dueKm,
            daysRemaining = days,
            kmRemaining = kmRemaining,
            estimatedKmDueDate = estimate,
            mileageStale = rule.intervalKm != null && stale,
            snoozed = rule.snoozedUntil?.let { today.isBefore(it) } ?: false,
            dueCycle = "${lastDate}|${lastKm ?: "-"}",
        )
    }

    /** Earliest reading on or after [date], ignoring readings before the latest segment break. */
    fun firstKmOnOrAfter(date: LocalDate, readings: List<OdometerReading>): Long? {
        if (readings.isEmpty()) return null
        val sorted = readings.sortedWith(compareBy<OdometerReading> { it.time.epochMillis }.thenBy { it.km })
        val segStart = sorted.indexOfLast { it.startsNewSegment }.coerceAtLeast(0)
        return sorted.subList(segStart, sorted.size).firstOrNull { !it.time.localDate.isBefore(date) }?.km
    }

    fun evaluateDocument(doc: VehicleDocument, today: LocalDate): DocumentStatus {
        val expiry = doc.expiryDate ?: return DocumentStatus(doc, DueState.UP_TO_DATE, null, null)
        val days = ChronoUnit.DAYS.between(today, expiry)
        val state = when {
            days < 0 -> DueState.OVERDUE
            days == 0L -> DueState.DUE
            days <= doc.warnDays -> DueState.DUE_SOON
            else -> DueState.UP_TO_DATE
        }
        val months = ChronoUnit.MONTHS.between(today.withDayOfMonth(1), expiry.withDayOfMonth(1)).coerceAtLeast(1)
        val savings = doc.expectedRenewalSen?.takeIf { days >= 0 }?.let { (it + months - 1) / months }
        return DocumentStatus(doc, state, days, savings)
    }
}
