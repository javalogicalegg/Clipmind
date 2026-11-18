package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BuyMultiplier
import com.example.model.GeneratorTier
import com.example.model.MAX_GENERATOR_COUNT
import com.example.model.MAX_MARKETING_LEVEL
import com.example.model.SimulationState
import com.example.ui.theme.AppIcons
import com.example.ui.theme.LocalClipThemePreset
import com.example.util.MAX_SAFE_DOUBLE
import com.example.util.NumberFormatter
import com.example.util.NumberNotation
import com.example.util.safeProgressRatio
import com.example.util.sanitize
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun FactoryScreen(
    state: SimulationState,
    buyMultiplier: BuyMultiplier,
    notation: NumberNotation,
    onMakeClip: (Int) -> Unit,
    onAdjustPrice: (Double) -> Unit,
    onToggleAutoPricer: (Boolean) -> Unit,
    onUpgradeMarketing: () -> Unit,
    onBuyWire: () -> Unit,
    onToggleAutoWire: (Boolean) -> Unit,
    onSelectBuyMultiplier: (BuyMultiplier) -> Unit,
    onBuyGenerator: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val preset = LocalClipThemePreset.current
    val cardBorder = BorderStroke(
        width = if (preset.isHighContrastBorder) 1.5.dp else 1.dp,
        color = MaterialTheme.colorScheme.outline
    )

    val currentOnMakeClip by rememberUpdatedState(onMakeClip)
    var isPressingForge by remember { mutableStateOf(false) }
    val forgeScale by animateFloatAsState(
        targetValue = if (isPressingForge) 0.96f else 1.0f,
        animationSpec = spring(stiffness = 600f),
        label = "forge_scale"
    )

    LaunchedEffect(isPressingForge) {
        if (isPressingForge) {
            delay(220L)
            while (isPressingForge) {
                currentOnMakeClip(2)
                delay(75L)
            }
        }
    }

    // Stabilize remember key on unlocked tier count + generator list so it does not rebuild 10 times/sec
    val unlockedGeneratorCount = state.generators.count { gen ->
        state.unlimitedResources ||
            state.totalClipsEver >= gen.unlockClipsRequired * 0.25 ||
            gen.count > 0 ||
            gen.tierIndex <= 2
    }

    val visibleGenerators = remember(state.generators, unlockedGeneratorCount, state.unlimitedResources) {
        val unlocked = mutableListOf<GeneratorTier>()
        var addedNextLocked = false
        for (gen in state.generators) {
            val isUnlocked = state.unlimitedResources ||
                state.totalClipsEver >= gen.unlockClipsRequired * 0.25 ||
                gen.count > 0 ||
                gen.tierIndex <= 2
            if (isUnlocked) {
                unlocked.add(gen)
            } else if (!addedNextLocked) {
                unlocked.add(gen)
                addedNextLocked = true
            }
        }
        unlocked
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("factory_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Primary Manual & Rapid-Pulse Extruder Actuator
        item(key = "manual_actuator") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 84.dp)
                    .scale(forgeScale)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = "Make Paperclip"
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressingForge = true
                                currentOnMakeClip(1)
                                tryAwaitRelease()
                                isPressingForge = false
                            }
                        )
                    }
                    .testTag("make_paperclip_button"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = "ACTUATE WIRE EXTRUDER",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "MAKE PAPERCLIP",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Tap or hold to extrude (+${NumberFormatter.formatCount(state.manualClipPower, notation, 1)}/tap)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = AppIcons.PrecisionManufacturing,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // 2. Market Economics & Pricing Console
        item(key = "market_console") {
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = AppIcons.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "GLOBAL MARKET & PRICING",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sales: ${NumberFormatter.formatCount(state.salesPerSecond, notation, 1)}/s",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1
                        )
                    }

                    // Price Adjustment Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Price: ${NumberFormatter.formatPrice(state.pricePerClip)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "Public Demand: ${NumberFormatter.formatCount(state.publicDemandPercent, notation, 0)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onAdjustPrice(-0.01) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("decrease_price_button")
                            ) {
                                Icon(
                                    imageVector = AppIcons.Remove,
                                    contentDescription = "Lower Clip Price",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("-$0.01", maxLines = 1, softWrap = false)
                            }
                            OutlinedButton(
                                onClick = { onAdjustPrice(0.01) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("increase_price_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Raise Clip Price",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+$0.01", maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    if (state.autoPricerUnlocked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = AppIcons.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "Dynamic Auto-Pricer AI",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Switch(
                                checked = state.autoPricerEnabled,
                                onCheckedChange = onToggleAutoPricer,
                                modifier = Modifier.testTag("auto_pricer_switch")
                            )
                        }
                    }

                    // Marketing Campaign Button
                    val isMarketingMaxed = state.marketingLevel >= MAX_MARKETING_LEVEL
                    val canAffordMarketing = !isMarketingMaxed && (state.unlimitedResources || state.funds >= state.marketingUpgradeCost)
                    Button(
                        onClick = onUpgradeMarketing,
                        enabled = canAffordMarketing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("upgrade_marketing_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = AppIcons.Campaign,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isMarketingMaxed) {
                                "Algorithmic Marketing Lv.${state.marketingLevel} (MAX)"
                            } else {
                                "Algorithmic Marketing Lv.${state.marketingLevel} (+28% Demand) — ${
                                    if (state.unlimitedResources) "FREE" else NumberFormatter.formatCurrency(state.marketingUpgradeCost, notation)
                                }"
                            },
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // 3. Wire Supply & Extrusion Logistics
        item(key = "wire_console") {
            val wireQuote = state.calculateWireSpoolQuote(buyMultiplier)
            val spoolLabel = if (wireQuote.spoolsToBuy > 1) {
                "Buy ×${wireQuote.spoolsToBuy} Wire Spools"
            } else {
                "Buy Wire Spool"
            }

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
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = AppIcons.Cable,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "WIRE LOGISTICS & EXTRUSION",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.2f", state.wirePerClipRatio)} in/clip",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Extruded: +${NumberFormatter.formatCount(state.wireExtrudedPerSec, notation, 1)}/s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Consumed: -${NumberFormatter.formatCount(state.wireConsumedPerSec, notation, 1)}/s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onBuyWire,
                        enabled = wireQuote.canAfford,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("buy_wire_button")
                    ) {
                        Text(
                            text = "$spoolLabel (+${NumberFormatter.formatCount(wireQuote.wireGained, notation, 0)} in) — ${
                                if (state.unlimitedResources) "FREE" else NumberFormatter.formatCurrency(wireQuote.totalCost, notation)
                            }",
                            maxLines = 2
                        )
                    }

                    if (state.autoWireUnlocked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Autonomous Wire Procurement",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = state.autoWireEnabled,
                                onCheckedChange = onToggleAutoWire,
                                modifier = Modifier.testTag("auto_wire_switch")
                            )
                        }
                    }
                }
            }
        }

        // 4. Bulk Buy Multiplier Selector + Manufacturing Tiers Header
        item(key = "generators_header") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "MANUFACTURING & EXTRACTION TIERS",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Buy Qty:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    BuyMultiplier.entries.forEach { mult ->
                        FilterChip(
                            selected = buyMultiplier == mult,
                            onClick = { onSelectBuyMultiplier(mult) },
                            label = { Text(mult.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false) },
                            modifier = Modifier.testTag("buy_mult_${mult.name}")
                        )
                    }
                }
            }
        }

        // 5. Generator Cards
        items(items = visibleGenerators, key = { it.id }) { gen ->
            GeneratorTierCard(
                generator = gen,
                state = state,
                buyMultiplier = buyMultiplier,
                notation = notation,
                border = cardBorder,
                onBuy = { onBuyGenerator(gen.id) }
            )
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GeneratorTierCard(
    generator: GeneratorTier,
    state: SimulationState,
    buyMultiplier: BuyMultiplier,
    notation: NumberNotation,
    border: BorderStroke,
    onBuy: () -> Unit
) {
    val isUnlocked = state.unlimitedResources ||
        state.totalClipsEver >= generator.unlockClipsRequired * 0.25 ||
        generator.count > 0 ||
        generator.tierIndex <= 2

    if (!isUnlocked) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = border
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked Tier",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NEXT TIER ${generator.tierIndex}: ${generator.name}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Unlocks at ${NumberFormatter.formatCount(generator.unlockClipsRequired * 0.25, notation, 0)} total paperclips",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    val isMaxedCount = generator.count >= MAX_GENERATOR_COUNT
    val remainingCap = (MAX_GENERATOR_COUNT - generator.count).coerceAtLeast(1)
    val qty = when (buyMultiplier) {
        BuyMultiplier.X1 -> 1
        BuyMultiplier.X10 -> 10.coerceAtMost(remainingCap)
        BuyMultiplier.X100 -> 100.coerceAtMost(remainingCap)
        BuyMultiplier.MAX -> (if (state.unlimitedResources) 50 else generator.maxAffordable(state.funds).coerceAtLeast(1)).coerceAtMost(remainingCap)
    }
    val cost = generator.bulkCost(qty)
    val canAfford = !isMaxedCount && (state.unlimitedResources || (cost < MAX_SAFE_DOUBLE * 0.99 && state.funds >= cost))

    val unitCps = (generator.baseCps * state.globalProductionMultiplier).sanitize(0.0)
    val totalTierCps = (unitCps * generator.count).sanitize(0.0)
    val shareOfTotalCps = safeProgressRatio(totalTierCps, state.potentialCps)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("generator_card_${generator.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "T${generator.tierIndex}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = generator.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = generator.roleSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "×${generator.count}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Output & Purchase Row
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
                        text = "+${NumberFormatter.formatCount(totalTierCps, notation, 1)} clips/s" +
                            if (generator.baseWirePerSec > 0) {
                                "\n+${NumberFormatter.formatCount(generator.baseWirePerSec * generator.count * state.extruderMultiplier, notation, 1)} wire/s"
                            } else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (generator.count > 0) {
                        Spacer(modifier = Modifier.height(5.dp))
                        LinearProgressIndicator(
                            progress = { shareOfTotalCps },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(5.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onBuy,
                    enabled = canAfford,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("buy_generator_${generator.id}")
                ) {
                    Text(
                        text = if (isMaxedCount) {
                            "MAX (${generator.count})"
                        } else {
                            "Buy ×$qty\n${if (state.unlimitedResources) "FREE" else NumberFormatter.formatCurrency(cost, notation)}"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
