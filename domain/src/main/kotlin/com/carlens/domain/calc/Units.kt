package com.carlens.domain.calc

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Exact conversions between user input and stored integer units.
 * Money is stored in sen, volume in millilitres, unit price in milli-sen per litre.
 * Rounding only happens at the display edge.
 */
object Units {
    fun parseMoneyToSen(text: String): Long? = parseScaled(text, 2)
    fun parseLitresToMl(text: String): Long? = parseScaled(text, 3)

    /** RM per litre, e.g. "2.05" → 205_000 milli-sen. */
    fun parseUnitPriceToMilliSen(text: String): Long? = parseScaled(text, 5)

    fun parseKm(text: String): Long? = text.trim().replace(",", "").replace(" ", "")
        .takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()
        ?.setScale(0, RoundingMode.HALF_UP)?.toLong()?.takeIf { it >= 0 }

    private fun parseScaled(text: String, scale: Int): Long? {
        val clean = text.trim().replace(",", "").removePrefix("RM").trim()
        if (clean.isEmpty()) return null
        val bd = clean.toBigDecimalOrNull() ?: return null
        if (bd.signum() < 0) return null
        return bd.movePointRight(scale).setScale(0, RoundingMode.HALF_UP).toLong()
    }

    /** Price per litre derived from what was actually paid. */
    fun unitPriceMilliSen(totalSen: Long, volumeMl: Long): Long? {
        if (volumeMl <= 0) return null
        // milli-sen/L = sen * 1000 * 1000 mL/L / mL
        return BigDecimal(totalSen).multiply(BigDecimal(1_000_000))
            .divide(BigDecimal(volumeMl), 0, RoundingMode.HALF_UP).toLong()
    }

    /** Suggested total when litres and unit price are known; receipt amount still takes precedence. */
    fun suggestedTotalSen(volumeMl: Long, unitPriceMilliSen: Long): Long =
        BigDecimal(volumeMl).multiply(BigDecimal(unitPriceMilliSen))
            .divide(BigDecimal(1_000_000), 0, RoundingMode.HALF_UP).toLong()

    fun senToPlain(sen: Long): String = BigDecimal(sen).movePointLeft(2).setScale(2).toPlainString()

    fun formatRm(sen: Long, hidden: Boolean = false): String {
        if (hidden) return "RM •••"
        val bd = BigDecimal(sen).movePointLeft(2).setScale(2)
        return "RM " + String.format(java.util.Locale.US, "%,.2f", bd)
    }

    fun formatRmCompact(sen: Long, hidden: Boolean = false): String {
        if (hidden) return "RM •••"
        val rm = sen / 100.0
        return if (rm >= 10_000) "RM " + String.format(java.util.Locale.US, "%,.0f", rm)
        else formatRm(sen)
    }

    /** Cost per kilometre; [senPerKm] is fractional sen, rounded half-up to whole sen for display. */
    fun formatRmPerKm(senPerKm: Double, hidden: Boolean = false): String {
        if (hidden) return "RM •••/km"
        val rm = BigDecimal(senPerKm).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
        return "RM ${rm.toPlainString()}/km"
    }

    fun mlToPlain(ml: Long): String = BigDecimal(ml).movePointLeft(3).stripTrailingZeros().let {
        if (it.scale() < 0) it.setScale(0) else it
    }.toPlainString()

    fun formatLitres(ml: Long): String = String.format(java.util.Locale.US, "%.2f L", ml / 1000.0)

    fun unitPriceToPlain(milliSen: Long): String =
        BigDecimal(milliSen).movePointLeft(5).setScale(2, RoundingMode.HALF_UP).toPlainString()

    fun formatUnitPrice(milliSen: Long): String =
        "RM " + BigDecimal(milliSen).movePointLeft(5).setScale(2, RoundingMode.HALF_UP).toPlainString() + "/L"

    fun formatKm(km: Long): String = String.format(java.util.Locale.US, "%,d km", km)

    fun formatKmPerL(v: Double): String = String.format(java.util.Locale.US, "%.2f", v)
    fun formatLPer100(v: Double): String = String.format(java.util.Locale.US, "%.2f", v)
}
