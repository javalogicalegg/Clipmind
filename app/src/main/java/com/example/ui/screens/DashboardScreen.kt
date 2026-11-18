package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.SimulationState
import com.example.model.TelemetryPoint
import com.example.ui.theme.AppIcons
import com.example.ui.theme.LocalClipThemePreset
import com.example.util.NumberFormatter
import com.example.util.NumberNotation
import com.example.util.safeProgressRatio
import com.example.util.sanitize
import com.example.util.sanitizeProgress
import java.util.Locale
import kotlin.math.max

@Composable
fun DashboardScreen(
    state: SimulationState,
    telemetryHistory: List<TelemetryPoint>,
    notation: NumberNotation,
    modifier: Modifier = Modifier
) {
    val preset = LocalClipThemePreset.current
    val cardBorder = BorderStroke(
        width = if (preset.isHighContrastBorder) 1.5.dp else 1.dp,
        color = MaterialTheme.colorScheme.outline
    )

    val cpsPoints = remember(telemetryHistory) {
        telemetryHistory.map { it.cps.sanitize(0.0) }
    }
    val revenuePoints = remember(telemetryHistory) {
        telemetryHistory.map { it.revenuePerSec.sanitize(0.0) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "PRODUCTION & RESOURCE TELEMETRY",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Real-time throughput diagnostics, bottleneck detection, and tier share",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. Bottleneck & System Health Diagnostic Banner
        item(key = "bottleneck_diagnostic") {
            val isWireStarved = !state.unlimitedResources &&
                state.wireRemaining < state.potentialCps * state.wirePerClipRatio &&
                state.wireExtrudedPerSec < state.wireConsumedPerSec
            val isInventoryBloated = state.unsoldInventory > max(500.0, state.clipsPerSecond * 15.0)

            val statusTitle = when {
                isWireStarved -> "BOTTLENECK: WIRE SUPPLY DEFICIT"
                isInventoryBloated -> "MARKET SATURATION: INVENTORY ACCUMULATING"
                else -> "OPTIMAL THROUGHPUT: PIPELINE BALANCED"
            }
            val statusDetail = when {
                isWireStarved -> "Wire consumption (${NumberFormatter.formatCount(state.wireConsumedPerSec, notation)}/s) exceeds extrusion (${NumberFormatter.formatCount(state.wireExtrudedPerSec, notation)}/s). Commission Scrap Wire Extruders or enable Auto-Wire."
                isInventoryBloated -> "Production exceeds sales velocity (${NumberFormatter.formatCount(state.salesPerSecond, notation)}/s). Lower clip price or upgrade Algorithmic Marketing."
                else -> "Wire supply, manufacturing stamping, and market demand are balanced."
            }
            val statusColor = when {
                isWireStarved -> MaterialTheme.colorScheme.error
                isInventoryBloated -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = statusColor.copy(alpha = 0.12f),
                border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isWireStarved || isInventoryBloated) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = statusColor
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor
                        )
                        Text(
                            text = statusDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 2. Real-Time Sparkline Charts (CPS & Revenue Velocity)
        item(key = "cps_chart_card") {
            TelemetrySparklineCard(
                title = "CLIP MANUFACTURING VELOCITY (CPS)",
                primaryStat = "${NumberFormatter.formatCount(state.clipsPerSecond, notation, 2)} clips/s",
                secondaryStat = "Peak Potential: ${NumberFormatter.formatCount(state.potentialCps, notation, 2)}/s",
                points = cpsPoints,
                strokeColor = MaterialTheme.colorScheme.primary,
                border = cardBorder
            )
        }

        item(key = "revenue_chart_card") {
            TelemetrySparklineCard(
                title = "REVENUE & MARKET LIQUIDITY ($/SEC)",
                primaryStat = NumberFormatter.formatRate(state.revenuePerSecond, notation, "$/s"),
                secondaryStat = "Price: ${NumberFormatter.formatPrice(state.pricePerClip)} • Demand: ${NumberFormatter.formatCount(state.publicDemandPercent, notation, 0)}%",
                points = revenuePoints,
                strokeColor = MaterialTheme.colorScheme.secondary,
                border = cardBorder
            )
        }

        // 3. Resource Acquisition & Wire Balance Matrix
        item(key = "resource_balance_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = cardBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "RESOURCE ACQUISITION & EFFICIENCY",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    StatKeyValueRow(
                        label = "Autonomous Wire Extrusion",
                        value = "+${NumberFormatter.formatCount(state.wireExtrudedPerSec, notation, 2)} in/s"
                    )
                    StatKeyValueRow(
                        label = "Factory Wire Consumption",
                        value = "-${NumberFormatter.formatCount(state.wireConsumedPerSec, notation, 2)} in/s"
                    )
                    StatKeyValueRow(
                        label = "Wire Efficiency Ratio",
                        value = "${String.format(Locale.US, "%.2f", state.wirePerClipRatio)} in/clip"
                    )
                    StatKeyValueRow(
                        label = "Wire Spools Acquired",
                        value = "${state.wireSpoolsPurchased} spools"
                    )
                    StatKeyValueRow(
                        label = "Global Output Multiplier",
                        value = "×${NumberFormatter.formatCount(state.globalProductionMultiplier, notation, 2)}"
                    )
                    StatKeyValueRow(
                        label = "Compute Throughput",
                        value = "+${NumberFormatter.formatCount(state.opsPerSecond, notation, 1)} Ops/s"
                    )
                    StatKeyValueRow(
                        label = "Creativity Generation",
                        value = "+${NumberFormatter.formatCount(state.creativityPerSecond, notation, 1)} Cr/s"
                    )
                }
            }
        }

        // 4. Hardware Tier Contribution Breakdown
        item(key = "tier_breakdown_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = cardBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "MANUFACTURING TIER SHARE",
                        style = MaterialTheme.typography.titleMedium
                    )

                    val activeGens = state.generators.filter { it.count > 0 }
                    if (activeGens.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.PrecisionManufacturing,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Commission AutoClippers or Wire Extruders on the Factory tab to track per-tier output share.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        activeGens.forEach { gen ->
                            val tierCps = (gen.count * gen.baseCps * state.globalProductionMultiplier).sanitize(0.0)
                            val ratio = safeProgressRatio(tierCps, state.potentialCps)

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${gen.name} (×${gen.count})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "${NumberFormatter.formatCount(tierCps, notation, 1)}/s (${String.format(Locale.US, "%.1f%%", ratio * 100f)})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TelemetrySparklineCard(
    title: String,
    primaryStat: String,
    secondaryStat: String,
    points: List<Double>,
    strokeColor: Color,
    border: BorderStroke
) {
    // Reuse Path and Brush instances across 10 Hz recompositions to avoid GC churn
    val linePath = remember { Path() }
    val fillPath = remember { Path() }
    val fillBrush = remember(strokeColor) {
        Brush.verticalGradient(
            colors = listOf(
                strokeColor.copy(alpha = 0.30f),
                strokeColor.copy(alpha = 0.0f)
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = primaryStat,
                    style = MaterialTheme.typography.headlineMedium,
                    color = strokeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = secondaryStat,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            ) {
                val gridColor = MaterialTheme.colorScheme.outlineVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    for (i in 1..3) {
                        val y = h * (i / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    val count = points.size
                    val maxVal = (points.maxOrNull() ?: 1.0).sanitize(1.0, 1.0)
                    val minVal = (points.minOrNull() ?: 0.0).sanitize(0.0, 0.0, maxVal * 0.8)
                    val range = (maxVal - minVal).sanitize(1.0, 1.0)

                    linePath.rewind()
                    fillPath.rewind()

                    if (count >= 2) {
                        for (index in 0 until count) {
                            val v = points[index]
                            val x = (index.toFloat() / (count - 1).coerceAtLeast(1)) * w
                            val normalized = safeProgressRatio(v - minVal, range).sanitizeProgress()
                            val y = h - (normalized * (h * 0.82f)) - (h * 0.08f)
                            if (index == 0) {
                                linePath.moveTo(x, y)
                                fillPath.moveTo(x, h)
                                fillPath.lineTo(x, y)
                            } else {
                                linePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                        }
                    } else {
                        val v = points.firstOrNull() ?: 0.0
                        val normalized = safeProgressRatio(v - minVal, range).sanitizeProgress()
                        val y = h - (normalized * (h * 0.82f)) - (h * 0.08f)
                        linePath.moveTo(0f, h * 0.92f)
                        linePath.lineTo(w, y)
                        fillPath.moveTo(0f, h)
                        fillPath.lineTo(0f, h * 0.92f)
                        fillPath.lineTo(w, y)
                    }

                    fillPath.lineTo(w, h)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = fillBrush
                    )

                    drawPath(
                        path = linePath,
                        color = strokeColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
