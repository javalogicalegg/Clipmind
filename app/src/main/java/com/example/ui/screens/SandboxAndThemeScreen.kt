package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.SandboxSnapshot
import com.example.model.SimulationMode
import com.example.model.SimulationState
import com.example.ui.theme.AppIcons
import com.example.ui.theme.ClipThemePreset
import com.example.ui.theme.LocalClipThemePreset
import com.example.util.HapticProfile
import com.example.util.NumberFormatter
import com.example.util.NumberNotation
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SandboxAndThemeScreen(
    state: SimulationState,
    currentTheme: ClipThemePreset,
    currentHapticProfile: HapticProfile,
    currentNotation: NumberNotation,
    snapshots: List<SandboxSnapshot>,
    onSwitchMode: (SimulationMode) -> Unit,
    onSetTickSpeed: (Float) -> Unit,
    onTogglePause: () -> Unit,
    onStepSingleTick: () -> Unit,
    onToggleUnlimitedResources: (Boolean) -> Unit,
    onGrantResourceBurst: (Double) -> Unit,
    onJumpToTier: (Int) -> Unit,
    onToggleResearchInSandbox: (String) -> Unit,
    onCaptureSnapshot: () -> Unit,
    onSelectTheme: (ClipThemePreset) -> Unit,
    onSelectHapticProfile: (HapticProfile) -> Unit,
    onSelectNotation: (NumberNotation) -> Unit,
    onResetSlot: (SimulationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val preset = LocalClipThemePreset.current
    val cardBorder = BorderStroke(
        width = if (preset.isHighContrastBorder) 1.5.dp else 1.dp,
        color = MaterialTheme.colorScheme.outline
    )

    var pendingResetMode by remember { mutableStateOf<SimulationMode?>(null) }

    pendingResetMode?.let { modeToReset ->
        AlertDialog(
            onDismissRequest = { pendingResetMode = null },
            title = {
                Text(
                    text = if (modeToReset == SimulationMode.MAIN) "Reset Live Simulation?" else "Reset Sandbox Lab?",
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            text = {
                Text(
                    text = if (modeToReset == SimulationMode.MAIN) {
                        "This will permanently erase your Live Simulation progress, generators, research, and chronicle logs."
                    } else {
                        "This will reset the Sandbox Laboratory back to initial starting values."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetSlot(modeToReset)
                        pendingResetMode = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingResetMode = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("sandbox_and_theme_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Sandbox Mode Laboratory Card
        item(key = "sandbox_lab_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = cardBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Column {
                            Text(
                                text = "SANDBOX EFFICIENCY LAB",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Test production strategies safely in an isolated simulation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mode Switcher Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onSwitchMode(SimulationMode.SANDBOX) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("activate_sandbox_mode_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.mode == SimulationMode.SANDBOX) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (state.mode == SimulationMode.SANDBOX) {
                                    MaterialTheme.colorScheme.onTertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        ) {
                            Text("SANDBOX LAB", maxLines = 1)
                        }

                        Button(
                            onClick = { onSwitchMode(SimulationMode.MAIN) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("activate_main_mode_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.mode == SimulationMode.MAIN) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (state.mode == SimulationMode.MAIN) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        ) {
                            Text("LIVE MODE", maxLines = 1)
                        }
                    }

                    // Unlimited Resources Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        ) {
                            Text(
                                text = "Unlimited Resources (∞)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Zero-cost upgrades, infinite wire, and unlimited trust",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.unlimitedResources,
                            onCheckedChange = {
                                if (state.mode != SimulationMode.SANDBOX) {
                                    onSwitchMode(SimulationMode.SANDBOX)
                                }
                                onToggleUnlimitedResources(it)
                            },
                            modifier = Modifier.testTag("sandbox_unlimited_switch")
                        )
                    }

                    // Tick Speed Multiplier Slider (0.5x to 100x)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Simulation Tick Speed",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", state.tickSpeedMultiplier)}×",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        val safeSliderSpeed = state.tickSpeedMultiplier.takeIf { it.isFinite() }?.coerceIn(0.5f, 100f) ?: 1.0f
                        Slider(
                            value = safeSliderSpeed,
                            onValueChange = { rawValue ->
                                if (!rawValue.isFinite()) return@Slider
                                if (state.mode != SimulationMode.SANDBOX) {
                                    onSwitchMode(SimulationMode.SANDBOX)
                                }
                                val snapped = ((rawValue.coerceIn(0.5f, 100f) * 2f).roundToInt() / 2f).coerceIn(0.5f, 100f)
                                if (kotlin.math.abs(snapped - safeSliderSpeed) >= 0.4f) {
                                    onSetTickSpeed(snapped)
                                }
                            },
                            valueRange = 0.5f..100f,
                            modifier = Modifier.testTag("sandbox_speed_slider")
                        )

                        // Quick speed preset chips + Pause / Step
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(1f, 2f, 5f, 10f, 25f, 50f, 100f).forEach { speed ->
                                FilterChip(
                                    selected = kotlin.math.abs(state.tickSpeedMultiplier - speed) < 0.25f,
                                    onClick = {
                                        if (state.mode != SimulationMode.SANDBOX) {
                                            onSwitchMode(SimulationMode.SANDBOX)
                                        }
                                        onSetTickSpeed(speed)
                                    },
                                    label = { Text("${speed.toInt()}×") }
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (state.mode != SimulationMode.SANDBOX) {
                                        onSwitchMode(SimulationMode.SANDBOX)
                                    }
                                    onTogglePause()
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("sandbox_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (state.isPaused) Icons.Default.PlayArrow else AppIcons.Pause,
                                    contentDescription = "Pause or Resume Sandbox",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (state.isPaused) "Resume" else "Pause")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (state.mode != SimulationMode.SANDBOX) {
                                        onSwitchMode(SimulationMode.SANDBOX)
                                    }
                                    onStepSingleTick()
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("sandbox_step_button")
                            ) {
                                Icon(
                                    imageVector = AppIcons.SkipNext,
                                    contentDescription = "Step 1 Second",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+1s Step")
                            }
                        }
                    }

                    // Instant Resource Grants & Bidirectional Tier Jumping
                    Text(
                        text = "Instant Resource Grants & Tier Calibration",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (state.mode != SimulationMode.SANDBOX) onSwitchMode(SimulationMode.SANDBOX)
                                onGrantResourceBurst(10.0)
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("sandbox_grant_10x_button")
                        ) {
                            Icon(AppIcons.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+10× Resources")
                        }
                        OutlinedButton(
                            onClick = {
                                if (state.mode != SimulationMode.SANDBOX) onSwitchMode(SimulationMode.SANDBOX)
                                onGrantResourceBurst(1000.0)
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("sandbox_grant_1000x_button")
                        ) {
                            Icon(AppIcons.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+1,000× Resources")
                        }
                        listOf(1 to "Jump Tier 1", 3 to "Jump Tier 3", 5 to "Jump Tier 5", 8 to "Jump Tier 8").forEach { (tier, label) ->
                            OutlinedButton(
                                onClick = {
                                    if (state.mode != SimulationMode.SANDBOX) onSwitchMode(SimulationMode.SANDBOX)
                                    onJumpToTier(tier)
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("sandbox_jump_tier_$tier")
                            ) {
                                Text(label)
                            }
                        }
                    }

                    // Benchmark Snapshot Capture & Comparison
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Benchmarks (${snapshots.size})",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = onCaptureSnapshot,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("capture_snapshot_button")
                        ) {
                            Icon(AppIcons.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Record Benchmark")
                        }
                    }

                    snapshots.forEach { snap ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${snap.label} • ${snap.activeResearchCount} upgrades",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )
                                Text(
                                    text = "${NumberFormatter.formatRate(snap.revenuePerSec, currentNotation, "$/s")} • ×${NumberFormatter.formatCount(snap.globalMultiplier, currentNotation, 1)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Sandbox Free Research Upgrade Toggle Header + Lazy Keyed Items
        item(key = "sandbox_upgrade_inspector_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "RESEARCH UPGRADE TOGGLES",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Toggle upgrades on or off to benchmark throughput, trust, and wire demand.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(
            items = state.researchProjects,
            key = { "sandbox_res_${it.id}" }
        ) { proj ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (state.mode != SimulationMode.SANDBOX) {
                            onSwitchMode(SimulationMode.SANDBOX)
                        }
                        onToggleResearchInSandbox(proj.id)
                    },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = cardBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "[T${proj.tier}] ${proj.title}" + if (proj.level > 1) " (Lv.${proj.level})" else "",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (proj.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = proj.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Checkbox(
                        checked = proj.isActive,
                        onCheckedChange = {
                            if (state.mode != SimulationMode.SANDBOX) {
                                onSwitchMode(SimulationMode.SANDBOX)
                            }
                            onToggleResearchInSandbox(proj.id)
                        },
                        modifier = Modifier.testTag("sandbox_toggle_res_${proj.id}")
                    )
                }
            }
        }

        // 3. Eye-Strain-Reducing Color Themes Card (8 Presets)
        item(key = "theme_selector_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
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
                            imageVector = AppIcons.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "COLOR THEMES & DARK MODES",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Low-strain palettes tailored for extended sessions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    ClipThemePreset.entries.forEach { themeOption ->
                        val isSelected = themeOption == currentTheme
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectTheme(themeOption) }
                                .testTag("theme_preset_${themeOption.id}"),
                            shape = RoundedCornerShape(12.dp),
                            color = themeOption.surfaceColor,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) themeOption.primaryColor else themeOption.borderSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(themeOption.bgColor)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(themeOption.primaryColor)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(themeOption.secondaryColor)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = themeOption.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = themeOption.textPrimary
                                        )
                                        Text(
                                            text = themeOption.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = themeOption.textSecondary
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected Theme",
                                        tint = themeOption.primaryColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Tactile Haptic Feedback & Number Notation Card
        item(key = "haptics_and_notation_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
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
                            imageVector = AppIcons.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "TACTILE HAPTIC FEEDBACK",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Precision tactile responses for controls, sliders, and milestones",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HapticProfile.entries.forEach { profile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectHapticProfile(profile) }
                                .padding(vertical = 4.dp)
                                .testTag("haptic_profile_${profile.name}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentHapticProfile == profile,
                                onClick = { onSelectHapticProfile(profile) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = profile.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "TELEMETRY NUMBER NOTATION",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NumberNotation.entries.forEach { notation ->
                            FilterChip(
                                selected = currentNotation == notation,
                                onClick = { onSelectNotation(notation) },
                                label = { Text(notation.label, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("notation_${notation.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pendingResetMode = SimulationMode.SANDBOX },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("reset_sandbox_button")
                        ) {
                            Icon(AppIcons.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Sandbox", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { pendingResetMode = SimulationMode.MAIN },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("reset_main_button")
                        ) {
                            Icon(AppIcons.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Main Save", maxLines = 1)
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
