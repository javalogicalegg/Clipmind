package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AutoResearchRule
import com.example.model.ResearchProject
import com.example.model.SimulationState
import com.example.ui.theme.AppIcons
import com.example.ui.theme.LocalClipThemePreset
import com.example.util.NumberFormatter
import com.example.util.NumberNotation
import com.example.util.safeProgressRatio

@Composable
fun ResearchScreen(
    state: SimulationState,
    notation: NumberNotation,
    onAllocateTrust: (Boolean) -> Unit,
    onQuantumComputeBurst: () -> Unit,
    onToggleAutoResearch: (Boolean) -> Unit,
    onSelectAutoResearchRule: (AutoResearchRule) -> Unit,
    onPurchaseResearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val preset = LocalClipThemePreset.current
    val cardBorder = BorderStroke(
        width = if (preset.isHighContrastBorder) 1.5.dp else 1.dp,
        color = MaterialTheme.colorScheme.outline
    )

    var selectedTierFilter by remember { mutableIntStateOf(0) }

    val tierNames = remember {
        listOf(
            0 to "All Tiers",
            1 to "Tier I: Industrial",
            2 to "Tier II: Cognitive",
            3 to "Tier III: Planetary",
            4 to "Tier IV: Stellar",
            5 to "Tier V: Endless ∞"
        )
    }

    val filteredProjects = remember(state.researchProjects, selectedTierFilter) {
        if (selectedTierFilter == 0) {
            state.researchProjects
        } else {
            state.researchProjects.filter { it.tier == selectedTierFilter }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("research_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Neural Compute & Trust Allocation Matrix
        item(key = "compute_matrix") {
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.DeveloperBoard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "COMPUTE & TRUST MATRIX",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (state.unallocatedTrust > 0 || state.unlimitedResources) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = "Trust: ${if (state.unlimitedResources) "∞" else state.unallocatedTrust}/${state.trust}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Operations & Creativity progress bars
                    val opsProgress = safeProgressRatio(state.operations, state.maxOperations)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ops: ${NumberFormatter.formatCount(state.operations, notation, 0)} / ${NumberFormatter.formatCount(state.maxOperations, notation, 0)}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "+${NumberFormatter.formatCount(state.opsPerSecond, notation, 1)} Ops/s",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { opsProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Creativity: ${NumberFormatter.formatCount(state.creativity, notation, 0)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "+${NumberFormatter.formatCount(state.creativityPerSecond, notation, 1)} Cr/s",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Processors & Memory Trust Allocation Buttons
                    val canAllocate = state.unlimitedResources || state.unallocatedTrust > 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAllocateTrust(true) },
                            enabled = canAllocate,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 52.dp)
                                .testTag("allocate_processor_button")
                        ) {
                            Icon(
                                imageVector = AppIcons.DeveloperBoard,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Processors",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Lv.${state.processors} (+1)",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { onAllocateTrust(false) },
                            enabled = canAllocate,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 52.dp)
                                .testTag("allocate_memory_button")
                        ) {
                            Icon(
                                imageVector = AppIcons.Memory,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Memory",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Lv.${state.memory} (+1)",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Photonic Compute Pulse
                    Button(
                        onClick = onQuantumComputeBurst,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("quantum_compute_burst_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = AppIcons.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PULSE COMPUTE ARRAY (+Ops & Creativity)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // 2. Automated Research Scheduler Card
        item(key = "auto_research_scheduler") {
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.AutoMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Column {
                                Text(
                                    text = "AUTOMATED RESEARCH",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (state.autoResearchUnlocked || state.unlimitedResources) {
                                        "Executes affordable upgrades automatically"
                                    } else {
                                        "Unlock 'Recursive Research Scheduler' in Tier II"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = state.autoResearchEnabled,
                            onCheckedChange = onToggleAutoResearch,
                            enabled = state.autoResearchUnlocked || state.unlimitedResources,
                            modifier = Modifier.testTag("auto_research_switch")
                        )
                    }

                    if (state.autoResearchUnlocked || state.unlimitedResources) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AutoResearchRule.entries.forEach { rule ->
                                FilterChip(
                                    selected = state.autoResearchRule == rule,
                                    onClick = { onSelectAutoResearchRule(rule) },
                                    label = { Text(rule.title, style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.testTag("auto_research_rule_${rule.name}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Research Tier Filter Chips
        item(key = "tier_filter_row") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RESEARCH & OPTIMIZATION TIERS",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tierNames.forEach { (tierIdx, label) ->
                        FilterChip(
                            selected = selectedTierFilter == tierIdx,
                            onClick = { selectedTierFilter = tierIdx },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("research_tier_filter_$tierIdx")
                        )
                    }
                }
            }
        }

        // 4. Research Project Cards
        items(items = filteredProjects, key = { it.id }) { proj ->
            ResearchProjectCard(
                project = proj,
                state = state,
                notation = notation,
                border = cardBorder,
                onPurchase = { onPurchaseResearch(proj.id) }
            )
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResearchProjectCard(
    project: ResearchProject,
    state: SimulationState,
    notation: NumberNotation,
    border: BorderStroke,
    onPurchase: () -> Unit
) {
    val isCompletedNonRepeatable = project.isMaxedOut
    val opsCost = project.currentOpsCost()
    val creatCost = project.currentCreativityCost()
    val meetsClipThreshold = state.unlimitedResources || state.totalClipsEver >= project.unlockClipsRequired * 0.25
    val canAfford = !project.isMaxedOut && (state.unlimitedResources || (state.operations >= opsCost && state.creativity >= creatCost))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("research_card_${project.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompletedNonRepeatable) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (project.isRepeatable) {
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ) {
                            Text(
                                text = if (project.isRepeatable) "TIER V • Lv.${project.level}" else "TIER ${project.tier}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (project.isRepeatable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = project.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "\"${project.loreNote}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isCompletedNonRepeatable) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (project.isEnabledInSandbox) "ACTIVE" else "PAUSED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (!isCompletedNonRepeatable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp)
                    ) {
                        Text(
                            text = "Cost: ${NumberFormatter.formatCount(opsCost, notation, 0)} Ops" +
                                if (creatCost > 0) " • ${NumberFormatter.formatCount(creatCost, notation, 0)} Cr" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (!meetsClipThreshold) {
                            Text(
                                text = "Unlocks at ${NumberFormatter.formatCount(project.unlockClipsRequired * 0.25, notation, 0)} clips",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onPurchase,
                        enabled = meetsClipThreshold && canAfford,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("execute_research_${project.id}")
                    ) {
                        Icon(
                            imageVector = AppIcons.Science,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (project.isRepeatable) "Lv.${project.level + 1}" else "Execute",
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
