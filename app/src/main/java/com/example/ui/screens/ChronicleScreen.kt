package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SimulationCatalog
import com.example.model.ChronicleCategory
import com.example.model.ChronicleEntry
import com.example.model.MAX_ENDLESS_MILESTONE_INDEX
import com.example.model.SimulationState
import com.example.ui.theme.AppIcons
import com.example.ui.theme.LocalClipThemePreset
import com.example.util.NumberFormatter
import com.example.util.NumberNotation
import com.example.util.safePow
import com.example.util.safeProgressRatio
import com.example.util.sanitize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sharedLogTimeFormat = ThreadLocal.withInitial {
    SimpleDateFormat("HH:mm:ss", Locale.US)
}

@Composable
fun ChronicleScreen(
    state: SimulationState,
    chronicleEntries: List<ChronicleEntry>,
    notation: NumberNotation,
    modifier: Modifier = Modifier
) {
    val preset = LocalClipThemePreset.current
    val cardBorder = BorderStroke(
        width = if (preset.isHighContrastBorder) 1.5.dp else 1.dp,
        color = MaterialTheme.colorScheme.outline
    )

    var selectedCategory by remember { mutableStateOf(ChronicleCategory.ALL) }

    val filteredLogs = remember(chronicleEntries, selectedCategory) {
        if (selectedCategory == ChronicleCategory.ALL) {
            chronicleEntries
        } else {
            chronicleEntries.filter { it.category == selectedCategory }
        }
    }

    val nextMilestone = remember(state.unlockedMilestoneIds) {
        SimulationCatalog.dominationMilestones.firstOrNull {
            !state.unlockedMilestoneIds.contains(it.id)
        }
    }

    val assimilationText = when {
        state.globalControlPercent < 100.0 -> String.format(Locale.US, "%.4f%%", state.globalControlPercent)
        state.globalControlPercent < 10_000.0 -> String.format(Locale.US, "%.1f%%", state.globalControlPercent)
        else -> "${NumberFormatter.formatCount(state.globalControlPercent, notation, 1)}%"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("chronicle_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Orbital Dyson Banner + Global/Cosmic Assimilation Status
        item(key = "domination_hero_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = cardBorder
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_ai_core_banner_1794713566093),
                            contentDescription = "Orbital Paperclip Array Visualization",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "ASSIMILATION: $assimilationText",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "GLOBAL & COSMIC DOMINATION PROGRESS",
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (nextMilestone != null) {
                            val progress = safeProgressRatio(state.totalClipsEver, nextMilestone.clipsThreshold)
                            Text(
                                text = "Next Target: ${nextMilestone.title} (${nextMilestone.regionAssimilated})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Requires ${NumberFormatter.formatCount(nextMilestone.clipsThreshold, notation, 1)} clips • Reward: +${nextMilestone.trustReward} Trust & ×${nextMilestone.productionMultiplierBonus} Output",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        } else {
                            val safeIdx = state.endlessMilestoneIndex.coerceIn(0, MAX_ENDLESS_MILESTONE_INDEX)
                            val nextEndless = (1.0e24 * safePow(1000.0, safeIdx.toDouble())).sanitize(1.0e24)
                            val progress = safeProgressRatio(state.totalClipsEver, nextEndless)
                            Text(
                                text = "Next Target: Multiversal Manifold #${safeIdx + 13}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Requires ${NumberFormatter.formatCount(nextEndless, notation, 2)} clips • Reward: +4 Trust & ×2.5 Output",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Category Filter Chips
        item(key = "chronicle_filter_row") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "DOMINATION LOG (${filteredLogs.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChronicleCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("chronicle_filter_${cat.name}")
                        )
                    }
                }
            }
        }

        // 3. Log Feed Items or Styled Empty State
        if (filteredLogs.isEmpty()) {
            item(key = "chronicle_empty_state") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chronicle_empty_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = cardBorder
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "No ${selectedCategory.label} Recorded Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Continue manufacturing paperclips, commissioning hardware tiers, or executing research breakthroughs to populate this telemetry feed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedCategory != ChronicleCategory.ALL) {
                            OutlinedButton(
                                onClick = { selectedCategory = ChronicleCategory.ALL },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("chronicle_reset_filter_button")
                            ) {
                                Text("Show All Logs")
                            }
                        }
                    }
                }
            }
        } else {
            items(items = filteredLogs, key = { "${it.id}_${it.timestampMs}" }) { entry ->
                ChronicleLogCard(
                    entry = entry,
                    notation = notation,
                    border = cardBorder
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChronicleLogCard(
    entry: ChronicleEntry,
    notation: NumberNotation,
    border: BorderStroke
) {
    val timeStr = remember(entry.timestampMs) {
        sharedLogTimeFormat.get()?.format(Date(entry.timestampMs)) ?: ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = entry.category.label.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "[$timeStr] • ${NumberFormatter.formatCount(entry.clipsAtEvent, notation, 1)} clips",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = entry.narrative,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
