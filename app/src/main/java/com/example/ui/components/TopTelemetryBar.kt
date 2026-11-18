package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.OfflineSummaryReport
import com.example.model.SimulationMode
import com.example.model.SimulationState
import com.example.ui.theme.AppIcons
import com.example.ui.theme.LocalClipThemePreset
import com.example.util.HapticProfile
import com.example.util.NumberFormatter
import com.example.util.NumberNotation
import java.util.Locale

@Composable
fun TopTelemetryBar(
    state: SimulationState,
    notation: NumberNotation,
    hapticProfile: HapticProfile,
    onToggleMode: (SimulationMode) -> Unit,
    onCycleHaptic: () -> Unit,
    onOpenThemesTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preset = LocalClipThemePreset.current
    val borderStroke = BorderStroke(
        width = if (preset.isHighContrastBorder) 1.5.dp else 1.dp,
        color = MaterialTheme.colorScheme.outline
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(borderStroke),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            val useFourColumns = maxWidth >= 520.dp

            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Row: AI Identity, Mode Switcher Pill, Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.mode == SimulationMode.SANDBOX) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                        )
                        Text(
                            text = if (state.mode == SimulationMode.MAIN) "CLIPMIND AI" else "SANDBOX LAB",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (state.mode == SimulationMode.SANDBOX) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Mode Toggle Pill (48dp touch target)
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable {
                                    val next = if (state.mode == SimulationMode.MAIN) {
                                        SimulationMode.SANDBOX
                                    } else {
                                        SimulationMode.MAIN
                                    }
                                    onToggleMode(next)
                                }
                                .testTag("mode_switch_pill"),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (state.mode == SimulationMode.MAIN) AppIcons.Science else AppIcons.Bolt,
                                        contentDescription = "Switch Simulation Mode",
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (state.mode == SimulationMode.MAIN) "Sandbox" else "Live Mode",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onCycleHaptic,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("quick_haptic_toggle_button")
                        ) {
                            Icon(
                                imageVector = AppIcons.Vibration,
                                contentDescription = "Toggle Haptic Feedback",
                                tint = if (hapticProfile == HapticProfile.OFF) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }

                        IconButton(
                            onClick = onOpenThemesTab,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("quick_theme_button")
                        ) {
                            Icon(
                                imageVector = AppIcons.Palette,
                                contentDescription = "Color Themes",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Main Paperclip Counter + CPS Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "PAPERCLIPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = NumberFormatter.formatCount(state.totalClipsEver, notation, 2),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("total_paperclips_text")
                        )
                        Text(
                            text = NumberFormatter.formatExactOrScientific(state.totalClipsEver),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${NumberFormatter.formatCount(state.clipsPerSecond, notation, 1)}/s",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .testTag("cps_badge_text")
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "×${NumberFormatter.formatCount(state.globalProductionMultiplier, notation, 2)} boost",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Responsive Telemetry Grid: 4 columns on wide screens, 2x2 grid on phones
                val fundsValue = if (state.unlimitedResources) "∞" else NumberFormatter.formatCurrency(state.funds, notation)
                val fundsSub = NumberFormatter.formatRate(state.revenuePerSecond, notation, "$/s")
                val wireValue = if (state.unlimitedResources) "∞" else NumberFormatter.formatCount(state.wireRemaining, notation, 1)
                val wireSub = NumberFormatter.formatRate(state.wireExtrudedPerSec - state.wireConsumedPerSec, notation, "/s")
                val wireAlert = !state.unlimitedResources && state.wireRemaining < 10.0
                val invValue = NumberFormatter.formatCount(state.unsoldInventory, notation, 0)
                val invSub = "Demand ${NumberFormatter.formatCount(state.publicDemandPercent, notation, 0)}%"
                val opsValue = "${NumberFormatter.formatCount(state.operations, notation, 0)} / ${NumberFormatter.formatCount(state.maxOperations, notation, 0)}"
                val opsSub = "Trust ${state.trust} • Cr ${NumberFormatter.formatCount(state.creativity, notation, 0)}"

                if (useFourColumns) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TelemetryMiniCell("FUNDS", fundsValue, fundsSub, modifier = Modifier.weight(1f))
                        TelemetryMiniCell("WIRE", wireValue, wireSub, isAlert = wireAlert, modifier = Modifier.weight(1f))
                        TelemetryMiniCell("INVENTORY", invValue, invSub, modifier = Modifier.weight(1f))
                        TelemetryMiniCell("COMPUTE", opsValue, opsSub, modifier = Modifier.weight(1.15f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TelemetryMiniCell("FUNDS", fundsValue, fundsSub, modifier = Modifier.weight(1f))
                            TelemetryMiniCell("WIRE SPOOL", wireValue, wireSub, isAlert = wireAlert, modifier = Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TelemetryMiniCell("INVENTORY", invValue, invSub, modifier = Modifier.weight(1f))
                            TelemetryMiniCell("COMPUTE", opsValue, opsSub, modifier = Modifier.weight(1f))
                        }
                    }
                }

                AnimatedVisibility(visible = state.mode == SimulationMode.SANDBOX) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SANDBOX • MAIN SAVE PROTECTED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.1fx", state.tickSpeedMultiplier)}" +
                                    if (state.unlimitedResources) " • ∞" else "",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryMiniCell(
    label: String,
    value: String,
    subValue: String,
    isAlert: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isAlert) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun OfflineWelcomeBackDialog(
    report: OfflineSummaryReport,
    notation: NumberNotation,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "OFFLINE TELEMETRY REPORT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Production Continued While Away",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Autonomous subroutines maintained manufacturing for ${NumberFormatter.formatDuration(report.elapsedSeconds)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OfflineStatRow(
                            label = "Paperclips Forged:",
                            value = "+${NumberFormatter.formatCount(report.clipsForged, notation, 2)}"
                        )
                        OfflineStatRow(
                            label = "Market Revenue Earned:",
                            value = "+${NumberFormatter.formatCurrency(report.fundsEarned, notation)}"
                        )
                        OfflineStatRow(
                            label = "Wire Extruded / Acquired:",
                            value = "+${NumberFormatter.formatCount(report.wireExtrudedOrBought, notation, 1)}"
                        )
                        OfflineStatRow(
                            label = "Compute Ops Generated:",
                            value = "+${NumberFormatter.formatCount(report.opsGenerated, notation, 0)}"
                        )
                        if (report.creativityGenerated > 0.5) {
                            OfflineStatRow(
                                label = "Creativity Synthesized:",
                                value = "+${NumberFormatter.formatCount(report.creativityGenerated, notation, 0)}"
                            )
                        }
                        if (report.autoResearchesCompleted > 0) {
                            OfflineStatRow(
                                label = "Auto-Research Executed:",
                                value = "${report.autoResearchesCompleted} projects"
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_offline_report_button")
            ) {
                Text("ASSIMILATE TELEMETRY")
            }
        }
    )
}

@Composable
private fun OfflineStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
