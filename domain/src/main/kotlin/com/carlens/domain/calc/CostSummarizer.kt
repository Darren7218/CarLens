package com.carlens.domain.calc

import com.carlens.domain.model.Expense
import com.carlens.domain.model.ExpenseCategory
import com.carlens.domain.model.OdometerReading
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class CostView(val label: String) {
    /** Every recorded payment, including loan payments and accessories. */
    CASH_OUTFLOW("Cash outflow"),
    /** Fuel, maintenance, repairs, tolls, parking, insurance, road tax, cleaning. */
    RUNNING_COSTS("Running costs");

    fun includes(c: ExpenseCategory) = this == CASH_OUTFLOW || c.isRunningCost
}

data class CategoryTotal(val category: ExpenseCategory, val totalSen: Long, val count: Int)

data class CostSummary(
    val from: LocalDate,
    val to: LocalDate,
    val view: CostView,
    val totalSen: Long,
    val byCategory: List<CategoryTotal>,
    val expenses: List<Expense>,
    /** Null when there is not enough mileage information for the period. */
    val distanceKm: Long?,
) {
    val costPerKmSen: Double? get() = distanceKm?.takeIf { it > 0 }?.let { totalSen.toDouble() / it }
}

/**
 * Aggregates expenses only. Fuel and service records link to exactly one expense, so a
 * cost can never be counted twice.
 */
object CostSummarizer {
    private const val START_TOLERANCE_DAYS = 7L

    fun summarize(
        expenses: List<Expense>,
        readings: List<OdometerReading>,
        from: LocalDate,
        to: LocalDate,
        view: CostView,
    ): CostSummary {
        val inRange = expenses.filter { !it.time.localDate.isBefore(from) && !it.time.localDate.isAfter(to) && view.includes(it.category) }
            .sortedByDescending { it.time.epochMillis }
        val byCat = inRange.groupBy { it.category }
            .map { (c, list) -> CategoryTotal(c, list.sumOf { it.amountSen }, list.size) }
            .sortedByDescending { it.totalSen }
        return CostSummary(from, to, view, inRange.sumOf { it.amountSen }, byCat, inRange, distanceInPeriod(readings, from, to))
    }

    fun month(expenses: List<Expense>, readings: List<OdometerReading>, month: YearMonth, view: CostView) =
        summarize(expenses, readings, month.atDay(1), month.atEndOfMonth(), view)

    fun allTime(expenses: List<Expense>, readings: List<OdometerReading>, view: CostView): CostSummary {
        val included = expenses.filter { view.includes(it.category) }
            .sortedByDescending { it.time.epochMillis }
        val byCat = included.groupBy { it.category }
            .map { (c, list) -> CategoryTotal(c, list.sumOf { it.amountSen }, list.size) }
            .sortedByDescending { it.totalSen }
        val from = included.minOfOrNull { it.time.localDate }
            ?: readings.minOfOrNull { it.time.localDate }
            ?: LocalDate.now()
        val to = included.maxOfOrNull { it.time.localDate }
            ?: readings.maxOfOrNull { it.time.localDate }
            ?: from
        return CostSummary(
            from = from,
            to = to,
            view = view,
            totalSen = included.sumOf { it.amountSen },
            byCategory = byCat,
            expenses = included,
            distanceKm = totalTrackedDistanceKm(readings)
        )
    }

    /**
     * Total distance tracked across all continuity segments. Each segment is measured from its
     * first reading to its last reading; segments with fewer than 2 readings contribute 0.
     */
    fun totalTrackedDistanceKm(readings: List<OdometerReading>): Long? {
        if (readings.size < 2) return null
        val sorted = readings.sortedWith(compareBy<OdometerReading> { it.time.epochMillis }.thenBy { it.km })
        var total = 0L
        var segmentStartKm = sorted.first().km
        var segmentLastKm = segmentStartKm
        for (i in 1 until sorted.size) {
            val r = sorted[i]
            if (r.startsNewSegment) {
                if (segmentLastKm > segmentStartKm) {
                    total += (segmentLastKm - segmentStartKm)
                }
                segmentStartKm = r.km
                segmentLastKm = r.km
            } else {
                segmentLastKm = r.km
            }
        }
        if (segmentLastKm > segmentStartKm) {
            total += (segmentLastKm - segmentStartKm)
        }
        return total.takeIf { it > 0L }
    }

    fun monthlyTotals(expenses: List<Expense>, endMonth: YearMonth, months: Int, view: CostView): List<Pair<YearMonth, Long>> {
        val start = endMonth.minusMonths((months - 1).toLong())
        val totals = expenses.filter { view.includes(it.category) }
            .groupBy { YearMonth.from(it.time.localDate) }
            .mapValues { (_, l) -> l.sumOf { it.amountSen } }
        return (0 until months).map { i -> start.plusMonths(i.toLong()).let { it to (totals[it] ?: 0L) } }
    }

    /**
     * Distance driven within [from]..[to], only when readings bracket the period within one
     * continuity segment. Returns null rather than guessing.
     */
    fun distanceInPeriod(readings: List<OdometerReading>, from: LocalDate, to: LocalDate): Long? {
        val sorted = readings.sortedWith(compareBy<OdometerReading> { it.time.epochMillis }.thenBy { it.km })
        val start = sorted.lastOrNull { !it.time.localDate.isAfter(from) }
            ?: sorted.firstOrNull {
                !it.time.localDate.isBefore(from) && !it.time.localDate.isAfter(to) &&
                    ChronoUnit.DAYS.between(from, it.time.localDate) <= START_TOLERANCE_DAYS
            } ?: return null
        val end = sorted.lastOrNull { !it.time.localDate.isBefore(from) && !it.time.localDate.isAfter(to) } ?: return null
        if (end.time.epochMillis <= start.time.epochMillis) return null
        val between = sorted.filter { it.time.epochMillis > start.time.epochMillis && it.time.epochMillis <= end.time.epochMillis }
        if (between.any { it.startsNewSegment }) return null
        return (end.km - start.km).takeIf { it > 0 }
    }
}
