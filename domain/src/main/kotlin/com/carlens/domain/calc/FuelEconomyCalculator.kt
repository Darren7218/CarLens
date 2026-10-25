package com.carlens.domain.calc

import com.carlens.domain.model.FuelEntry
import com.carlens.domain.model.TankStatus
import java.time.LocalDate

/** A completed full-to-full interval. */
data class EconomyInterval(
    val baselineEntryId: String,
    val closingEntryId: String,
    val contributingEntryIds: List<String>,
    val startKm: Long,
    val endKm: Long,
    val volumeMl: Long,
    /** Sum of money for the contributing fill-ups (baseline excluded); null if any amount missing. */
    val costSen: Long?,
    val endDate: LocalDate,
    /** Outside plausible range — shown for correction, excluded from overall figures. */
    val suspicious: Boolean,
) {
    val distanceKm: Long get() = endKm - startKm
    val kmPerL: Double get() = distanceKm / (volumeMl / 1000.0)
    val lPer100Km: Double get() = 100.0 * (volumeMl / 1000.0) / distanceKm
}

enum class EntryRole { BASELINE, CONTRIBUTES, CLOSES_INTERVAL, EXCLUDED }

enum class AnomalyType { ODOMETER_NOT_INCREASING, IMPLAUSIBLE_ECONOMY }

data class FuelAnomaly(val entryId: String, val type: AnomalyType, val message: String)

enum class TrendDirection { INCREASED, DECREASED, STABLE }

data class FuelTrend(
    val direction: TrendDirection,
    val recentLPer100: Double,
    val baselineLPer100: Double,
    val recentIntervals: Int,
    val baselineIntervals: Int,
) {
    val changePercent: Double get() = (recentLPer100 - baselineLPer100) / baselineLPer100 * 100.0
    val message: String
        get() = when (direction) {
            TrendDirection.INCREASED -> "Consumption increased across your recent recorded intervals"
            TrendDirection.DECREASED -> "Consumption decreased across your recent recorded intervals"
            TrendDirection.STABLE -> "Consumption is consistent with your usual intervals"
        }
}

data class FuelEconomyReport(
    val intervals: List<EconomyInterval>,
    val roles: Map<String, EntryRole>,
    /** Interval km/L keyed by the entry that closed it. */
    val economyByClosingEntry: Map<String, EconomyInterval>,
    val anomalies: List<FuelAnomaly>,
    val totalValidDistanceKm: Long,
    val totalValidVolumeMl: Long,
    val totalValidCostSen: Long?,
    val trend: FuelTrend?,
) {
    /** Total valid distance ÷ total valid litres — not an average of interval values. */
    val overallKmPerL: Double?
        get() = if (totalValidVolumeMl > 0 && totalValidDistanceKm > 0) totalValidDistanceKm / (totalValidVolumeMl / 1000.0) else null
    val overallLPer100: Double?
        get() = if (totalValidVolumeMl > 0 && totalValidDistanceKm > 0) 100.0 * (totalValidVolumeMl / 1000.0) / totalValidDistanceKm else null
    val costPerKmSen: Double?
        get() = totalValidCostSen?.takeIf { totalValidDistanceKm > 0 }?.let { it.toDouble() / totalValidDistanceKm }
    val latest: EconomyInterval? get() = intervals.lastOrNull { !it.suspicious }

    companion object {
        val EMPTY = FuelEconomyReport(emptyList(), emptyMap(), emptyMap(), emptyList(), 0, 0, null, null)
    }
}

/**
 * Full-to-full fuel economy.
 *
 * - The first full tank is a baseline; its litres are excluded.
 * - Every partial after the baseline plus the closing full fill-up are included.
 * - A missed fill-up or an odometer discontinuity drops the chain until the next full tank.
 * - Entries are processed in event-time order, so backdated inserts or edits recalculate naturally.
 */
object FuelEconomyCalculator {
    const val MIN_PLAUSIBLE_KM_PER_L = 3.0
    const val MAX_PLAUSIBLE_KM_PER_L = 40.0
    private const val TREND_RECENT = 3
    private const val TREND_MIN_BASELINE = 3
    private const val TREND_MAX_BASELINE = 10
    private const val TREND_THRESHOLD = 0.10

    fun calculate(entries: List<FuelEntry>): FuelEconomyReport {
        if (entries.isEmpty()) return FuelEconomyReport.EMPTY
        val sorted = entries.sortedWith(compareBy<FuelEntry> { it.time.epochMillis }.thenBy { it.odometerKm })
        val roles = LinkedHashMap<String, EntryRole>()
        val anomalies = mutableListOf<FuelAnomaly>()
        val intervals = mutableListOf<EconomyInterval>()

        var baseline: FuelEntry? = null
        val pending = mutableListOf<FuelEntry>()
        var previous: FuelEntry? = null

        fun resetChain() {
            pending.forEach { roles[it.id] = EntryRole.EXCLUDED }
            pending.clear()
            baseline = null
        }

        for (e in sorted) {
            val prev = previous
            if (e.missedPrevious || e.startsNewSegment) {
                resetChain()
            } else if (prev != null && e.odometerKm <= prev.odometerKm) {
                anomalies += FuelAnomaly(
                    e.id, AnomalyType.ODOMETER_NOT_INCREASING,
                    "Odometer ${e.odometerKm} km is not higher than the previous fill-up (${prev.odometerKm} km). Check the reading or date.",
                )
                resetChain()
            }
            previous = e

            val base = baseline
            if (base == null) {
                if (e.tankStatus == TankStatus.FULL) {
                    baseline = e
                    roles[e.id] = EntryRole.BASELINE
                } else {
                    roles[e.id] = EntryRole.EXCLUDED
                }
                continue
            }

            pending += e
            if (e.tankStatus == TankStatus.PARTIAL) {
                roles[e.id] = EntryRole.CONTRIBUTES
                continue
            }

            // Full tank closes the interval.
            val volume = pending.sumOf { it.volumeMl }
            val cost = if (pending.all { it.totalSen != null }) pending.sumOf { it.totalSen!! } else null
            val distance = e.odometerKm - base.odometerKm
            val kmPerL = if (volume > 0) distance / (volume / 1000.0) else 0.0
            val suspicious = volume <= 0 || kmPerL < MIN_PLAUSIBLE_KM_PER_L || kmPerL > MAX_PLAUSIBLE_KM_PER_L
            if (suspicious) {
                anomalies += FuelAnomaly(
                    e.id, AnomalyType.IMPLAUSIBLE_ECONOMY,
                    "This interval works out to ${Units.formatKmPerL(kmPerL)} km/L, which looks unlikely. Check odometer, litres, or tank status.",
                )
            }
            intervals += EconomyInterval(
                baselineEntryId = base.id,
                closingEntryId = e.id,
                contributingEntryIds = pending.map { it.id },
                startKm = base.odometerKm,
                endKm = e.odometerKm,
                volumeMl = volume,
                costSen = cost,
                endDate = e.time.localDate,
                suspicious = suspicious,
            )
            roles[e.id] = EntryRole.CLOSES_INTERVAL
            pending.clear()
            baseline = e
        }
        // Partials after the last full are still waiting for a closing full tank.
        pending.forEach { roles[it.id] = EntryRole.CONTRIBUTES }

        val valid = intervals.filter { !it.suspicious }
        val totalCost = if (valid.isNotEmpty() && valid.all { it.costSen != null }) valid.sumOf { it.costSen!! } else null
        return FuelEconomyReport(
            intervals = intervals,
            roles = roles,
            economyByClosingEntry = intervals.associateBy { it.closingEntryId },
            anomalies = anomalies,
            totalValidDistanceKm = valid.sumOf { it.distanceKm },
            totalValidVolumeMl = valid.sumOf { it.volumeMl },
            totalValidCostSen = totalCost,
            trend = trend(valid),
        )
    }

    fun trend(valid: List<EconomyInterval>): FuelTrend? {
        if (valid.size < TREND_RECENT + TREND_MIN_BASELINE) return null
        val recent = valid.takeLast(TREND_RECENT)
        val base = valid.dropLast(TREND_RECENT).takeLast(TREND_MAX_BASELINE)
        fun lPer100(list: List<EconomyInterval>) = 100.0 * (list.sumOf { it.volumeMl } / 1000.0) / list.sumOf { it.distanceKm }
        val r = lPer100(recent)
        val b = lPer100(base)
        val change = (r - b) / b
        val dir = when {
            change > TREND_THRESHOLD -> TrendDirection.INCREASED
            change < -TREND_THRESHOLD -> TrendDirection.DECREASED
            else -> TrendDirection.STABLE
        }
        return FuelTrend(dir, r, b, recent.size, base.size)
    }
}
