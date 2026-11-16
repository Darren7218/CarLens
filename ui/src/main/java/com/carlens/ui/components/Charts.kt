package com.carlens.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlens.domain.calc.CategoryTotal
import com.carlens.domain.calc.EconomyInterval
import com.carlens.domain.calc.Units
import com.carlens.domain.model.ExpenseCategory
import com.carlens.ui.theme.CategoryAccessoriesColor
import com.carlens.ui.theme.CategoryFuelColor
import com.carlens.ui.theme.CategoryInsuranceColor
import com.carlens.ui.theme.CategoryOtherColor
import com.carlens.ui.theme.CategoryParkingColor
import com.carlens.ui.theme.CategoryRepairsColor
import com.carlens.ui.theme.CategoryRoadTaxColor
import com.carlens.ui.theme.CategoryServiceColor
import com.carlens.ui.theme.CategoryTollColor
import com.carlens.ui.theme.StatusAmber
import com.carlens.ui.theme.StatusGreen
import com.carlens.ui.theme.StatusRed
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

fun expenseCategoryColor(category: ExpenseCategory): Color = when (category) {
    ExpenseCategory.FUEL -> CategoryFuelColor
    ExpenseCategory.MAINTENANCE -> CategoryServiceColor
    ExpenseCategory.REPAIR -> CategoryRepairsColor
    ExpenseCategory.TOLL -> CategoryTollColor
    ExpenseCategory.PARKING -> CategoryParkingColor
    ExpenseCategory.INSURANCE -> CategoryInsuranceColor
    ExpenseCategory.ROAD_TAX -> CategoryRoadTaxColor
    ExpenseCategory.CLEANING -> Color(0xFF26A69A)
    ExpenseCategory.ACCESSORIES -> CategoryAccessoriesColor
    ExpenseCategory.FINES -> StatusRed
    ExpenseCategory.LOAN_PAYMENT -> Color(0xFF5C6BC0)
    ExpenseCategory.OTHER -> CategoryOtherColor
}

fun expenseCategoryIcon(category: ExpenseCategory): ImageVector = when (category) {
    ExpenseCategory.FUEL -> Icons.Rounded.LocalGasStation
    ExpenseCategory.MAINTENANCE -> Icons.Rounded.Build
    ExpenseCategory.REPAIR -> Icons.Rounded.Handyman
    ExpenseCategory.TOLL -> Icons.Rounded.DirectionsCar
    ExpenseCategory.PARKING -> Icons.Rounded.LocalParking
    ExpenseCategory.INSURANCE -> Icons.Rounded.Shield
    ExpenseCategory.ROAD_TAX -> Icons.Rounded.Description
    ExpenseCategory.CLEANING -> Icons.Rounded.CleaningServices
    ExpenseCategory.ACCESSORIES -> Icons.Rounded.ShoppingBag
    ExpenseCategory.FINES -> Icons.Rounded.Warning
    ExpenseCategory.LOAN_PAYMENT -> Icons.Rounded.AccountBalance
    ExpenseCategory.OTHER -> Icons.Rounded.Category
}

@Composable
fun FuelEconomyChart(
    intervals: List<EconomyInterval>,
    overallKmPerL: Double?,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
    onInspectClosingEntry: ((String) -> Unit)? = null,
) {
    val chartIntervals = remember(intervals) { intervals.takeLast(14) }
    val defaultIndex = remember(chartIntervals) {
        val lastValid = chartIntervals.indexOfLast { !it.suspicious }
        if (lastValid >= 0) lastValid else chartIntervals.lastIndex.coerceAtLeast(0)
    }
    var selectedIndex by remember(chartIntervals) { mutableIntStateOf(defaultIndex) }
    LaunchedEffect(chartIntervals) {
        selectedIndex = defaultIndex
    }

    if (chartIntervals.isEmpty()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = "Economy trend will appear here",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Record at least two Full tank fill-ups to plot completed km/L intervals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val validValues = chartIntervals.filter { !it.suspicious }.map { it.kmPerL }
    val sourceValues = if (validValues.isNotEmpty()) validValues else chartIntervals.map { it.kmPerL }
    val rawMin = min(sourceValues.minOrNull() ?: 5.0, overallKmPerL ?: Double.MAX_VALUE)
    val rawMax = max(sourceValues.maxOrNull() ?: 20.0, overallKmPerL ?: 0.0)
    val pad = ((rawMax - rawMin) * 0.25).coerceAtLeast(1.2)
    val minVal = (rawMin - pad).coerceAtLeast(0.0)
    val maxVal = rawMax + pad
    val range = (maxVal - minVal).coerceAtLeast(1.0)

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val avgLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

    val safeIndex = selectedIndex.coerceIn(0, chartIntervals.lastIndex)
    val selected = chartIntervals[safeIndex]
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US) }
    val shortDateFmt = remember { DateTimeFormatter.ofPattern("d/M", Locale.US) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Y-axis header + legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Last ${chartIntervals.size} intervals (tap a point)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val avg = overallKmPerL
            if (avg != null) {
                Text(
                    text = "Avg ${Units.formatKmPerL(avg)} km/L",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )
            }
        }

        // Interactive Canvas Chart
        val topLabel = String.format(Locale.US, "%.1f", maxVal)
        val bottomLabel = String.format(Locale.US, "%.1f", minVal)

        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .pointerInput(chartIntervals) {
                        detectTapGestures { offset ->
                            val leftPad = 14.dp.toPx()
                            val rightPad = 14.dp.toPx()
                            val usableW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                            val n = chartIntervals.size
                            if (n == 1) {
                                selectedIndex = 0
                            } else {
                                val stepX = usableW / (n - 1).toFloat()
                                val tapped = ((offset.x - leftPad) / stepX).toInt()
                                val candidateIndices = listOf(tapped, tapped + 1)
                                    .filter { it in 0 until n }
                                val closest = candidateIndices.minByOrNull { idx ->
                                    val px = leftPad + idx * stepX
                                    abs(px - offset.x)
                                }
                                if (closest != null) selectedIndex = closest
                            }
                        }
                    }
            ) {
                val leftPad = 14.dp.toPx()
                val rightPad = 14.dp.toPx()
                val topPad = 18.dp.toPx()
                val bottomPad = 18.dp.toPx()
                val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                val plotH = (size.height - topPad - bottomPad).coerceAtLeast(1f)

                fun xFor(idx: Int): Float =
                    if (chartIntervals.size == 1) leftPad + plotW / 2f
                    else leftPad + idx * (plotW / (chartIntervals.size - 1).toFloat())

                fun yFor(kmPerL: Double): Float {
                    val norm = ((kmPerL - minVal) / range).toFloat().coerceIn(0f, 1f)
                    return topPad + plotH * (1f - norm)
                }

                // Gridlines
                val gridFractions = listOf(0f, 0.5f, 1f)
                gridFractions.forEach { f ->
                    val y = topPad + plotH * f
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPad, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }

                // Dashed overall average line
                val avg = overallKmPerL
                if (avg != null) {
                    val avgY = yFor(avg)
                    drawLine(
                        color = avgLineColor,
                        start = Offset(leftPad, avgY),
                        end = Offset(size.width - rightPad, avgY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                }

                // Build points
                val points = chartIntervals.mapIndexed { idx, interval ->
                    val clampedVal = interval.kmPerL.coerceIn(minVal, maxVal)
                    Offset(xFor(idx), yFor(clampedVal))
                }

                // Gradient area fill & smooth curve (when >= 2 points)
                if (points.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.lastIndex) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(points.last().x, topPad + plotH)
                        lineTo(points.first().x, topPad + plotH)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.24f),
                                primaryColor.copy(alpha = 0.01f)
                            ),
                            startY = topPad,
                            endY = topPad + plotH
                        )
                    )

                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Selected vertical hairline
                val selPt = points[safeIndex]
                drawLine(
                    color = primaryColor.copy(alpha = 0.35f),
                    start = Offset(selPt.x, topPad),
                    end = Offset(selPt.x, topPad + plotH),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Data points
                points.forEachIndexed { idx, pt ->
                    val interval = chartIntervals[idx]
                    val isSel = idx == safeIndex
                    val nodeColor = if (interval.suspicious) StatusAmber else primaryColor
                    if (isSel) {
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.22f),
                            radius = 12.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = surfaceColor,
                            radius = 6.5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = nodeColor,
                            radius = 5.dp.toPx(),
                            center = pt
                        )
                    } else {
                        drawCircle(
                            color = surfaceColor,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = nodeColor,
                            radius = 3.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            // Subtle corner scale labels
            Text(
                text = "$topLabel km/L",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 4.dp)
            )
            Text(
                text = "$bottomLabel km/L",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 4.dp)
            )
            Text(
                text = chartIntervals.first().endDate.format(shortDateFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 74.dp),
                textAlign = TextAlign.Start
            )
            Text(
                text = chartIntervals.last().endDate.format(shortDateFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp)
            )
        }

        // Tapped Point Explicit Summary Underneath
        val avg = overallKmPerL
        val selectedCostSen = selected.costSen
        val delta = if (avg != null && !selected.suspicious) selected.kmPerL - avg else null

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (selected.suspicious) {
                StatusAmber.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
            },
            border = BorderStroke(
                1.dp,
                if (selected.suspicious) StatusAmber.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${Units.formatKmPerL(selected.kmPerL)} km/L",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selected.suspicious) StatusAmber else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${Units.formatLPer100(selected.lPer100Km)} L/100 km",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = buildString {
                            append(selected.endDate.format(dateFmt))
                            append(" · ")
                            append(Units.formatKm(selected.distanceKm))
                            append(" · ")
                            append(Units.formatLitres(selected.volumeMl))
                            if (selectedCostSen != null && selected.distanceKm > 0) {
                                append(" · ")
                                append(
                                    Units.formatRmPerKm(
                                        selectedCostSen.toDouble() / selected.distanceKm,
                                        hideAmounts
                                    )
                                )
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selected.suspicious) {
                        Text(
                            text = "Unusual interval — tap Inspect to verify odometer or litres",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (delta != null) {
                        val sign = if (delta >= 0) "+" else ""
                        val deltaColor = if (delta >= 0) StatusGreen else StatusAmber
                        Text(
                            text = "${sign}${String.format(Locale.US, "%.2f", delta)} km/L vs overall avg",
                            style = MaterialTheme.typography.labelSmall,
                            color = deltaColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (onInspectClosingEntry != null) {
                    TextButton(onClick = { onInspectClosingEntry(selected.closingEntryId) }) {
                        Text("Inspect")
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlySpendChart(
    trend: List<Pair<YearMonth, Long>>,
    selectedMonth: YearMonth,
    budgetSen: Long?,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
    onSelectMonth: (YearMonth) -> Unit,
) {
    if (trend.isEmpty()) return

    val monthShortFmt = remember { DateTimeFormatter.ofPattern("MMM", Locale.US) }
    val monthLongFmt = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.US) }
    val nonZeroMonths = trend.filter { it.second > 0L }
    val avgSen = if (nonZeroMonths.isNotEmpty()) nonZeroMonths.sumOf { it.second } / nonZeroMonths.size else 0L
    val selectedTotalSen = trend.firstOrNull { it.first == selectedMonth }?.second ?: 0L
    val maxVal = max(trend.maxOfOrNull { it.second } ?: 1L, budgetSen ?: 0L).coerceAtLeast(1L)

    val primaryColor = MaterialTheme.colorScheme.primary
    val mutedBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header hint
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tap any month bar to view breakdown",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val budget = budgetSen
            if (budget != null && budget > 0L) {
                Text(
                    text = "Budget ${Units.formatRmCompact(budget, hideAmounts)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusAmber,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Interactive Bar Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .pointerInput(trend) {
                    detectTapGestures { offset ->
                        val n = trend.size
                        if (n > 0) {
                            val colW = size.width.toFloat() / n.toFloat()
                            val idx = (offset.x / colW).toInt().coerceIn(0, n - 1)
                            onSelectMonth(trend[idx].first)
                        }
                    }
                }
        ) {
            val topPad = 14.dp.toPx()
            val bottomPad = 6.dp.toPx()
            val plotH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
            val colW = size.width / trend.size.toFloat()
            val barW = min(colW * 0.56f, 34.dp.toPx())
            val corner = CornerRadius(8.dp.toPx(), 8.dp.toPx())

            // Baseline
            drawLine(
                color = gridColor,
                start = Offset(0f, topPad + plotH),
                end = Offset(size.width, topPad + plotH),
                strokeWidth = 1.dp.toPx()
            )

            // Optional dashed budget line
            val budget = budgetSen
            if (budget != null && budget > 0L) {
                val bFrac = (budget.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val bY = topPad + plotH * (1f - bFrac)
                drawLine(
                    color = StatusAmber.copy(alpha = 0.8f),
                    start = Offset(0f, bY),
                    end = Offset(size.width, bY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }

            // Bars
            trend.forEachIndexed { idx, (month, amountSen) ->
                val isSel = month == selectedMonth
                val overBudget = budget != null && budget > 0L && amountSen > budget
                val barColor = when {
                    isSel && overBudget -> StatusRed
                    isSel -> primaryColor
                    else -> mutedBarColor
                }
                val frac = (amountSen.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val barH = max(frac * plotH, if (amountSen > 0L) 8.dp.toPx() else 3.dp.toPx())
                val left = idx * colW + (colW - barW) / 2f
                val top = topPad + plotH - barH

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = Size(barW, barH),
                    cornerRadius = corner
                )
            }
        }

        // X-axis Month Labels
        Row(modifier = Modifier.fillMaxWidth()) {
            trend.forEach { (month, _) ->
                val isSel = month == selectedMonth
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectMonth(month) }
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = month.format(monthShortFmt),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Explicit Text Summary Underneath
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${selectedMonth.format(monthLongFmt)} · ${Units.formatRm(selectedTotalSen, hideAmounts)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val summaryDetail = buildString {
                        if (avgSen > 0L) {
                            append("6-mo avg: ${Units.formatRmCompact(avgSen, hideAmounts)}")
                            val diffPct = ((selectedTotalSen - avgSen).toDouble() / avgSen.toDouble() * 100.0)
                            if (selectedTotalSen > 0L) {
                                val absPct = abs(diffPct).toInt()
                                append(if (diffPct >= 0) " (+$absPct%)" else " (-$absPct%)")
                            }
                        } else {
                            append("No recorded spending across the last 6 months")
                        }
                    }
                    Text(
                        text = summaryDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val budget = budgetSen
                if (budget != null && budget > 0L) {
                    val over = selectedTotalSen > budget
                    val diff = abs(selectedTotalSen - budget)
                    CategoryTag(
                        text = if (over) "+${Units.formatRmCompact(diff, hideAmounts)} over" else "${Units.formatRmCompact(diff, hideAmounts)} left",
                        color = if (over) StatusRed else StatusGreen
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDonutAndBreakdown(
    byCategory: List<CategoryTotal>,
    totalSen: Long,
    selectedCategory: ExpenseCategory?,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
    onSelectCategory: (ExpenseCategory?) -> Unit,
) {
    if (byCategory.isEmpty()) return

    val sweepProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 550),
        label = "donutSweep"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val effectiveTotal = totalSen.coerceAtLeast(1L)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Donut + center summary + quick legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier.size(144.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(144.dp)
                        .pointerInput(byCategory, effectiveTotal, selectedCategory) {
                            detectTapGestures { offset ->
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val dx = offset.x - cx
                                val dy = offset.y - cy
                                val radius = hypot(dx, dy)
                                val outerR = min(size.width, size.height).toFloat() / 2f
                                val innerR = outerR * 0.48f
                                if (radius < innerR) {
                                    onSelectCategory(null)
                                    return@detectTapGestures
                                }
                                var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                // Canvas startAngle -90f corresponds to 12 o'clock
                                deg = (deg + 90f + 360f) % 360f
                                var acc = 0f
                                for (item in byCategory) {
                                    val sweep = (item.totalSen.toFloat() / effectiveTotal.toFloat()) * 360f
                                    if (deg >= acc && deg <= acc + sweep) {
                                        val next = if (selectedCategory == item.category) null else item.category
                                        onSelectCategory(next)
                                        break
                                    }
                                    acc += sweep
                                }
                            }
                        }
                ) {
                    val baseStroke = 20.dp.toPx()
                    val selectedStroke = 26.dp.toPx()
                    val inset = selectedStroke / 2f + 2.dp.toPx()
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    val topLeft = Offset(inset, inset)

                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = baseStroke)
                    )

                    val gapDeg = if (byCategory.size > 1) 2.5f else 0f
                    var startDeg = -90f
                    byCategory.forEach { catTotal ->
                        val rawSweep = (catTotal.totalSen.toFloat() / effectiveTotal.toFloat()) * 360f * sweepProgress
                        val sweep = (rawSweep - gapDeg).coerceAtLeast(1.5f)
                        val isSelected = selectedCategory == catTotal.category
                        val isDimmed = selectedCategory != null && !isSelected
                        val baseColor = expenseCategoryColor(catTotal.category)
                        drawArc(
                            color = if (isDimmed) baseColor.copy(alpha = 0.28f) else baseColor,
                            startAngle = startDeg + gapDeg / 2f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = if (isSelected) selectedStroke else baseStroke,
                                cap = StrokeCap.Butt
                            )
                        )
                        startDeg += rawSweep
                    }
                }

                // Donut center metric
                val activeCatTotal = selectedCategory?.let { sel ->
                    byCategory.firstOrNull { it.category == sel }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    if (activeCatTotal != null) {
                        val pct = ((activeCatTotal.totalSen.toDouble() / effectiveTotal.toDouble()) * 100.0).toInt()
                        Text(
                            text = activeCatTotal.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = expenseCategoryColor(activeCatTotal.category),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = Units.formatRmCompact(activeCatTotal.totalSen, hideAmounts),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = "${byCategory.size} cats",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Units.formatRmCompact(totalSen, hideAmounts),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }

            // Compact top-3 breakdown summary beside donut
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (selectedCategory != null) {
                        "Filtering by ${selectedCategory.label}"
                    } else {
                        "Tap a slice or row to filter"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                byCategory.take(4).forEach { item ->
                    val pct = (item.totalSen.toDouble() / effectiveTotal.toDouble() * 100.0).toInt()
                    val color = expenseCategoryColor(item.category)
                    val isSelected = selectedCategory == item.category
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectCategory(if (isSelected) null else item.category)
                            }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Text(
                                text = item.category.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Full interactive category rows underneath
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            byCategory.forEach { catTotal ->
                val isSelected = selectedCategory == catTotal.category
                val color = expenseCategoryColor(catTotal.category)
                val fraction = (catTotal.totalSen.toFloat() / effectiveTotal.toFloat()).coerceIn(0f, 1f)
                val pct = (fraction * 100f).toInt()
                val rowBg by animateColorAsState(
                    targetValue = if (isSelected) {
                        color.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    label = "catRowBg"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onSelectCategory(if (isSelected) null else catTotal.category)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = rowBg,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) color.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = color.copy(alpha = 0.14f)
                                ) {
                                    Icon(
                                        imageVector = expenseCategoryIcon(catTotal.category),
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = catTotal.category.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${catTotal.count} ${if (catTotal.count == 1) "entry" else "entries"} · $pct%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = Units.formatRm(catTotal.totalSen, hideAmounts),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Thin category proportion track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}
