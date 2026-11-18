package com.example.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

const val MAX_SAFE_DOUBLE = 1.0e300

fun Double.sanitize(
    default: Double = 0.0,
    min: Double = 0.0,
    max: Double = MAX_SAFE_DOUBLE
): Double {
    if (this.isNaN()) return default.coerceIn(min, max)
    if (this == Double.POSITIVE_INFINITY) return max
    if (this == Double.NEGATIVE_INFINITY) return min
    return this.coerceIn(min, max)
}

fun Float.sanitizeProgress(): Float {
    if (!this.isFinite()) return 0f
    return this.coerceIn(0f, 1f)
}

fun safePow(base: Double, exponent: Double, max: Double = MAX_SAFE_DOUBLE): Double {
    if (!base.isFinite() || !exponent.isFinite() || base <= 0.0) return 1.0
    val result = base.pow(exponent)
    if (result.isNaN()) return 1.0
    if (result == Double.POSITIVE_INFINITY) return max
    return result.coerceIn(0.0, max)
}

fun safeProgressRatio(numerator: Double, denominator: Double): Float {
    if (numerator.isNaN() || denominator.isNaN() || denominator <= 0.0 || numerator <= 0.0) {
        return 0f
    }
    if (numerator.isInfinite() && denominator.isInfinite()) {
        return 1f
    }
    val n = numerator.coerceIn(0.0, MAX_SAFE_DOUBLE)
    val d = denominator.coerceIn(1e-9, MAX_SAFE_DOUBLE)
    return (n / d).toFloat().sanitizeProgress()
}

enum class NumberNotation(val label: String) {
    STANDARD("Standard Suffixes (M, B, T, Qa...)"),
    SCIENTIFIC("Scientific (1.42e18)"),
    ENGINEERING("Engineering (142.0e15)")
}

object NumberFormatter {

    private val SUFFIXES = arrayOf(
        "", "K", "M", "B", "T",
        "Qa", "Qi", "Sx", "Sp", "Oc", "No",
        "Dc", "UDc", "DDc", "TDc", "QaDc", "QiDc", "SxDc", "SpDc", "OcDc", "NoDc",
        "Vg", "UVg", "DVg", "TVg", "QaVg", "QiVg", "SxVg", "SpVg", "OcVg", "NoVg",
        "Tg", "UTg", "DTg"
    )

    fun formatCount(
        value: Double,
        notation: NumberNotation = NumberNotation.STANDARD,
        decimals: Int = 2
    ): String {
        if (value.isNaN()) return "0"
        if (value.isInfinite()) return "∞"
        val sign = if (value < 0) "-" else ""
        val absVal = abs(value).coerceAtMost(MAX_SAFE_DOUBLE)

        if (absVal < 1000.0) {
            return if (absVal == floor(absVal)) {
                "$sign${absVal.toLong()}"
            } else {
                String.format(Locale.US, "%s%.${decimals}f", sign, absVal)
            }
        }

        val exp = floor(log10(absVal)).toInt().coerceAtLeast(0)
        return when (notation) {
            NumberNotation.SCIENTIFIC -> {
                val scale = 10.0.pow(exp.toDouble())
                val mantissa = if (scale.isFinite() && scale > 0.0) absVal / scale else 1.0
                String.format(Locale.US, "%s%.2fe%d", sign, mantissa.sanitize(1.0, 0.0, 10.0), exp)
            }
            NumberNotation.ENGINEERING -> {
                val engExp = (exp / 3) * 3
                val scale = 10.0.pow(engExp.toDouble())
                val mantissa = if (scale.isFinite() && scale > 0.0) absVal / scale else 1.0
                String.format(Locale.US, "%s%.2fe%d", sign, mantissa.sanitize(1.0, 0.0, 1000.0), engExp)
            }
            NumberNotation.STANDARD -> {
                val tier = exp / 3
                if (tier in 1 until SUFFIXES.size) {
                    val scale = 10.0.pow((tier * 3).toDouble())
                    val scaled = if (scale.isFinite() && scale > 0.0) absVal / scale else 1.0
                    String.format(Locale.US, "%s%.${decimals}f%s", sign, scaled.sanitize(1.0, 0.0, 1000.0), SUFFIXES[tier])
                } else {
                    val scale = 10.0.pow(exp.toDouble())
                    val mantissa = if (scale.isFinite() && scale > 0.0) absVal / scale else 1.0
                    String.format(Locale.US, "%s%.2fe%d", sign, mantissa.sanitize(1.0, 0.0, 10.0), exp)
                }
            }
        }
    }

    fun formatExactOrScientific(value: Double): String {
        if (value.isNaN() || value <= 0.0) return "0"
        if (value.isInfinite()) return "∞"
        val safeVal = value.coerceAtMost(MAX_SAFE_DOUBLE)
        if (safeVal < 1_000_000_000_000.0) {
            return String.format(Locale.US, "%,.0f", floor(safeVal))
        }
        val exp = floor(log10(abs(safeVal))).toInt().coerceAtLeast(0)
        val scale = 10.0.pow(exp.toDouble())
        val mantissa = if (scale.isFinite() && scale > 0.0) safeVal / scale else 1.0
        return String.format(Locale.US, "%.4f × 10^%d", mantissa.sanitize(1.0, 0.0, 10.0), exp)
    }

    fun formatCurrency(
        value: Double,
        notation: NumberNotation = NumberNotation.STANDARD
    ): String {
        if (value.isNaN()) return "$0.00"
        if (value.isInfinite()) return "$∞"
        val sign = if (value < 0) "-" else ""
        val absVal = abs(value).coerceAtMost(MAX_SAFE_DOUBLE)
        return if (absVal < 1000.0) {
            String.format(Locale.US, "%s$%.2f", sign, absVal)
        } else {
            "$sign$${formatCount(absVal, notation, 2)}"
        }
    }

    fun formatPrice(value: Double): String {
        val safe = value.sanitize(default = 0.25, min = 0.01, max = 1000.0)
        return String.format(Locale.US, "$%.2f", safe)
    }

    fun formatRate(
        value: Double,
        notation: NumberNotation = NumberNotation.STANDARD,
        unit: String = "/s"
    ): String {
        if (value.isNaN() || abs(value) < 0.005) return "0$unit"
        val safe = value.sanitize(default = 0.0, min = -MAX_SAFE_DOUBLE, max = MAX_SAFE_DOUBLE)
        val prefix = if (safe > 0) "+" else ""
        return if (abs(safe) < 100.0) {
            String.format(Locale.US, "%s%.1f%s", prefix, safe, unit)
        } else {
            "$prefix${formatCount(safe, notation, 2)}$unit"
        }
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0L) return "0s"
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (mins > 0) append("${mins}m ")
            if (secs > 0 || isEmpty()) append("${secs}s")
        }.trim()
    }
}
