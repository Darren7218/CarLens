package com.carlens.domain

import com.carlens.domain.calc.AnomalyType
import com.carlens.domain.calc.CostSummarizer
import com.carlens.domain.calc.CostView
import com.carlens.domain.calc.DueState
import com.carlens.domain.calc.EntryRole
import com.carlens.domain.calc.FuelEconomyCalculator
import com.carlens.domain.calc.MaintenanceEvaluator
import com.carlens.domain.calc.MileageSnapshot
import com.carlens.domain.calc.TrendDirection
import com.carlens.domain.calc.Units
import com.carlens.domain.model.EventTime
import com.carlens.domain.model.Expense
import com.carlens.domain.model.ExpenseCategory
import com.carlens.domain.model.FuelEntry
import com.carlens.domain.model.MaintenanceCompletion
import com.carlens.domain.model.MaintenanceRule
import com.carlens.domain.model.OdometerReading
import com.carlens.domain.model.OdometerSource
import com.carlens.domain.model.TankStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private val KL = ZoneId.of("Asia/Kuala_Lumpur")
private val D0 = LocalDate.of(2026, 1, 1)

private fun fuel(
    id: String, day: Long, km: Long, litres: Double, status: TankStatus,
    missed: Boolean = false, newSegment: Boolean = false, sen: Long? = (litres * 205).toLong(),
) = FuelEntry(
    id = id, vehicleId = "v", time = EventTime.ofDate(D0.plusDays(day), KL), odometerReadingId = "r$id",
    odometerKm = km, startsNewSegment = newSegment, volumeMl = (litres * 1000).toLong(), unitPriceMilliSen = null,
    tankStatus = status, missedPrevious = missed, station = "", fuelType = "RON95", notes = "",
    expenseId = "e$id", totalSen = sen, createdAt = 0, updatedAt = 0,
)

private fun reading(day: Long, km: Long, newSeg: Boolean = false) = OdometerReading(
    "r$day-$km", "v", EventTime.ofDate(D0.plusDays(day), KL), km, OdometerSource.MANUAL, newSeg, 0, 0,
)

class FuelEconomyTest {
    @Test fun `plan example - full partial full gives 450 km over 35 L`() {
        val r = FuelEconomyCalculator.calculate(
            listOf(
                fuel("A", 0, 50_000, 30.0, TankStatus.FULL),
                fuel("B", 3, 50_200, 10.0, TankStatus.PARTIAL),
                fuel("C", 6, 50_450, 25.0, TankStatus.FULL),
            ),
        )
        assertEquals(1, r.intervals.size)
        val i = r.intervals.single()
        assertEquals(450, i.distanceKm)
        assertEquals(35_000, i.volumeMl)
        assertEquals(12.857, i.kmPerL, 0.001)
        assertEquals(EntryRole.BASELINE, r.roles["A"])
        assertEquals(EntryRole.CONTRIBUTES, r.roles["B"])
        assertEquals(EntryRole.CLOSES_INTERVAL, r.roles["C"])
    }

    @Test fun `partial alone does not produce an interval`() {
        val r = FuelEconomyCalculator.calculate(
            listOf(
                fuel("A", 0, 50_000, 30.0, TankStatus.FULL),
                fuel("B", 3, 50_200, 10.0, TankStatus.PARTIAL),
            ),
        )
        assertTrue(r.intervals.isEmpty())
        assertNull(r.overallKmPerL)
    }

    @Test fun `missed fill-up invalidates interval until new full baseline`() {
        val r = FuelEconomyCalculator.calculate(
            listOf(
                fuel("A", 0, 50_000, 30.0, TankStatus.FULL),
                fuel("B", 5, 50_800, 30.0, TankStatus.FULL, missed = true), // becomes new baseline
                fuel("C", 9, 51_200, 32.0, TankStatus.FULL),
            ),
        )
        assertEquals(1, r.intervals.size)
        assertEquals("B", r.intervals.single().baselineEntryId)
        assertEquals(400, r.intervals.single().distanceKm)
    }

    @Test fun `missed partial waits for next full`() {
        val r = FuelEconomyCalculator.calculate(
            listOf(
                fuel("A", 0, 50_000, 30.0, TankStatus.FULL),
                fuel("B", 5, 50_800, 10.0, TankStatus.PARTIAL, missed = true),
                fuel("C", 9, 51_200, 32.0, TankStatus.FULL),
                fuel("D", 12, 51_600, 31.0, TankStatus.FULL),
            ),
        )
        assertEquals(EntryRole.EXCLUDED, r.roles["B"])
        assertEquals(EntryRole.BASELINE, r.roles["C"])
        assertEquals(1, r.intervals.size)
        assertEquals("D", r.intervals.single().closingEntryId)
    }

    @Test fun `backdated insert recalculates intervals`() {
        val base = listOf(
            fuel("A", 0, 50_000, 30.0, TankStatus.FULL),
            fuel("C", 10, 50_600, 40.0, TankStatus.FULL),
        )
        assertEquals(15.0, FuelEconomyCalculator.calculate(base).intervals.single().kmPerL, 0.001)
        // A forgotten partial between A and C, entered later.
        val withBackdated = base + fuel("B", 5, 50_300, 10.0, TankStatus.PARTIAL)
        val i = FuelEconomyCalculator.calculate(withBackdated).intervals.single()
        assertEquals(50_000, i.volumeMl)
        assertEquals(12.0, i.kmPerL, 0.001)
    }

    @Test fun `odometer discontinuity starts a new segment`() {
        val r = FuelEconomyCalculator.calculate(
            listOf(
                fuel("A", 0, 50_000, 30.0, TankStatus.FULL),
                fuel("B", 5, 100, 30.0, TankStatus.FULL, newSegment = true),
                fuel("C", 9, 500, 32.0, TankStatus.FULL),
            ),
        )
        assertEquals(1, r.intervals.size)
        assertEquals(400, r.intervals.single().distanceKm)
        assertTrue(r.anomalies.isEmpty())
    }

    @Test fun `decreasing odometer is flagged not silently fixed`() {
        val r = FuelEconomyCalculator.calculate(
            listOf(
                fuel("A", 0, 50_000, 30.0, TankStatus.FULL),
                fuel("B", 5, 49_000, 30.0, TankStatus.FULL),
            ),
        )
        assertTrue(r.intervals.isEmpty())
        assertEquals(AnomalyType.ODOMETER_NOT_INCREASING, r.anomalies.single().type)
    }

    @Test fun `overall efficiency is total distance over total litres`() {
        val r = FuelEconomyCalculator.calculate(
            listOf(
                fuel("A", 0, 10_000, 30.0, TankStatus.FULL),
                fuel("B", 5, 10_100, 10.0, TankStatus.FULL), // 10 km/L
                fuel("C", 9, 10_700, 30.0, TankStatus.FULL), // 20 km/L
            ),
        )
        // Average of displayed values would be 15; correct overall is 700 / 40 = 17.5.
        assertEquals(17.5, r.overallKmPerL!!, 0.001)
    }

    @Test fun `implausible interval is flagged and excluded from overall`() {
        val r = FuelEconomyCalculator.calculate(
            listOf(
                fuel("A", 0, 10_000, 30.0, TankStatus.FULL),
                fuel("B", 5, 10_400, 30.0, TankStatus.FULL),
                fuel("C", 9, 12_400, 20.0, TankStatus.FULL), // 100 km/L — typo
            ),
        )
        assertTrue(r.intervals[1].suspicious)
        assertEquals(400, r.totalValidDistanceKm)
        assertEquals(AnomalyType.IMPLAUSIBLE_ECONOMY, r.anomalies.single().type)
    }

    @Test fun `trend detects increased consumption`() {
        val entries = mutableListOf(fuel("0", 0, 10_000, 30.0, TankStatus.FULL))
        var km = 10_000L
        for (i in 1..6) {
            km += 500
            val litres = if (i <= 3) 33.3 else 41.7 // ~15 km/L then ~12 km/L
            entries += fuel("$i", i * 5L, km, litres, TankStatus.FULL)
        }
        val trend = FuelEconomyCalculator.calculate(entries).trend!!
        assertEquals(TrendDirection.INCREASED, trend.direction)
    }
}

class UnitsTest {
    @Test fun `price per litre from total and litres`() {
        assertEquals(205_000L, Units.unitPriceMilliSen(totalSen = 6150, volumeMl = 30_000))
        assertEquals(6150L, Units.suggestedTotalSen(30_000, 205_000))
        assertEquals(1234L, Units.parseMoneyToSen("12.34"))
        assertEquals(30_512L, Units.parseLitresToMl("30.512"))
        assertEquals(205_000L, Units.parseUnitPriceToMilliSen("2.05"))
        assertNull(Units.parseMoneyToSen("abc"))
    }

    @Test fun `formatRmPerKm rounds half up and respects hidden flag`() {
        assertEquals("RM 0.16/km", Units.formatRmPerKm(15.6, hidden = false))
        assertEquals("RM 1.00/km", Units.formatRmPerKm(99.6, hidden = false))
        assertEquals("RM •••/km", Units.formatRmPerKm(15.6, hidden = true))
    }
}

class MaintenanceTest {
    private fun rule(months: Int?, km: Long?, startKm: Long? = 50_000) = MaintenanceRule(
        id = "oil", vehicleId = "v", task = "Engine oil", intervalMonths = months, intervalKm = km,
        startDate = D0, startKm = startKm, warnDays = 30, warnKm = 1_000, snoozedUntil = null,
        active = true, notes = "", createdAt = 0, updatedAt = 0,
    )

    private fun snap(km: Long, day: LocalDate) = MileageSnapshot(km, day, 30.0)

    @Test fun `mileage threshold reached first makes it due`() {
        val today = D0.plusMonths(2)
        val s = MaintenanceEvaluator.evaluate(rule(6, 10_000), emptyList(), snap(60_100, today), today)
        assertEquals(DueState.DUE, s.state)
    }

    @Test fun `date threshold reached first makes it due even with low mileage`() {
        val today = D0.plusMonths(6).plusDays(1)
        val s = MaintenanceEvaluator.evaluate(rule(6, 10_000), emptyList(), snap(52_000, today), today)
        assertEquals(DueState.DUE, s.state)
    }

    @Test fun `due soon inside warning window and overdue beyond grace`() {
        val today = D0.plusMonths(1)
        assertEquals(DueState.DUE_SOON, MaintenanceEvaluator.evaluate(rule(null, 10_000), emptyList(), snap(59_200, today), today).state)
        assertEquals(DueState.OVERDUE, MaintenanceEvaluator.evaluate(rule(null, 10_000), emptyList(), snap(61_000, today), today).state)
    }

    @Test fun `mileage unknown without recent reading`() {
        val today = D0.plusMonths(1)
        val s = MaintenanceEvaluator.evaluate(rule(null, 10_000), emptyList(), MileageSnapshot.UNKNOWN, today)
        assertEquals(DueState.MILEAGE_UNKNOWN, s.state)
        val stale = MaintenanceEvaluator.evaluate(rule(null, 10_000), emptyList(), snap(51_000, today.minusDays(90)), today)
        assertEquals(DueState.MILEAGE_UNKNOWN, stale.state)
    }

    @Test fun `completion resets only its own rule`() {
        val today = D0.plusMonths(7)
        val oil = rule(6, 10_000)
        val tyres = oil.copy(id = "tyres", task = "Tyre rotation")
        val completions = listOf(MaintenanceCompletion("c", "oil", "s", null, today.minusDays(1), 59_000))
        val m = snap(59_050, today)
        assertEquals(DueState.UP_TO_DATE, MaintenanceEvaluator.evaluate(oil, completions, m, today).state)
        assertEquals(DueState.OVERDUE, MaintenanceEvaluator.evaluate(tyres, completions, m, today).state)
    }

    @Test fun `estimates due date from recorded driving`() {
        val today = D0
        val s = MaintenanceEvaluator.evaluate(rule(null, 10_000), emptyList(), MileageSnapshot(50_000, today, 50.0), today)
        assertEquals(today.plusDays(200), s.estimatedKmDueDate)
    }

    @Test fun `infers baseline km from first odometer reading on or after startDate when startKm is null`() {
        val today = D0.plusMonths(2)
        val r = rule(months = 6, km = 10_000, startKm = null)
        val readings = listOf(
            reading(2, 40_000),
            reading(30, 49_500)
        )
        val s = MaintenanceEvaluator.evaluate(r, emptyList(), snap(49_500, today), today, readings)
        assertEquals(50_000L, s.dueKm)
        assertEquals(500L, s.kmRemaining)
        assertEquals(DueState.DUE_SOON, s.state)
    }
}

class CostTest {
    private fun exp(id: String, day: Long, sen: Long, cat: ExpenseCategory, fuelId: String? = null) = Expense(
        id = id, vehicleId = "v", time = EventTime.ofDate(D0.plusDays(day), KL), amountSen = sen, category = cat,
        merchant = "", notes = "", fuelEntryId = fuelId, createdAt = 0, updatedAt = 0,
    )

    @Test fun `totals count each expense once and views differ`() {
        val e = listOf(
            exp("1", 1, 6000, ExpenseCategory.FUEL, fuelId = "f1"),
            exp("2", 2, 25000, ExpenseCategory.MAINTENANCE),
            exp("3", 3, 80000, ExpenseCategory.LOAN_PAYMENT),
            exp("4", 40, 1000, ExpenseCategory.TOLL), // next month
        )
        val cash = CostSummarizer.month(e, emptyList(), YearMonth.from(D0), CostView.CASH_OUTFLOW)
        val running = CostSummarizer.month(e, emptyList(), YearMonth.from(D0), CostView.RUNNING_COSTS)
        assertEquals(111000, cash.totalSen)
        assertEquals(31000, running.totalSen)
        assertNull(cash.costPerKmSen) // no mileage information
    }

    @Test fun `distance needs readings bracketing the period`() {
        val readings = listOf(reading(-2, 10_000), reading(20, 10_800))
        assertEquals(800L, CostSummarizer.distanceInPeriod(readings, D0, D0.plusDays(30)))
        assertNull(CostSummarizer.distanceInPeriod(listOf(reading(20, 10_800)), D0, D0.plusDays(30)))
    }

    @Test fun `allTime sums across months and computes multi-segment total distance`() {
        val e = listOf(
            exp("1", 1, 6000, ExpenseCategory.FUEL, fuelId = "f1"),
            exp("2", 2, 25000, ExpenseCategory.MAINTENANCE),
            exp("3", 3, 80000, ExpenseCategory.LOAN_PAYMENT),
            exp("4", 40, 1000, ExpenseCategory.TOLL), // next month
        )
        val readings = listOf(
            reading(0, 10_000),
            reading(20, 10_500), // +500 km in segment 1
            reading(30, 100, newSeg = true),
            reading(50, 400) // +300 km in segment 2
        )
        assertEquals(800L, CostSummarizer.totalTrackedDistanceKm(readings))
        val runningAll = CostSummarizer.allTime(e, readings, CostView.RUNNING_COSTS)
        assertEquals(32000L, runningAll.totalSen)
        assertEquals(800L, runningAll.distanceKm)
        assertEquals(40.0, runningAll.costPerKmSen!!, 0.001)
    }
}
