package com.example.model

import com.example.util.MAX_SAFE_DOUBLE
import com.example.util.safePow
import com.example.util.sanitize
import kotlin.math.floor
import kotlin.math.min

const val MAX_GENERATOR_COUNT = 2_500
const val MAX_REPEATABLE_RESEARCH_LEVEL = 250
const val MAX_MARKETING_LEVEL = 500
const val MAX_ENDLESS_MILESTONE_INDEX = 90

enum class SimulationMode(val slotId: String, val label: String) {
    MAIN("MAIN", "Live Simulation"),
    SANDBOX("SANDBOX", "Sandbox Efficiency Lab")
}

enum class BuyMultiplier(val label: String, val amount: Int) {
    X1("×1", 1),
    X10("×10", 10),
    X100("×100", 100),
    MAX("MAX", -1)
}

enum class AutoResearchRule(val title: String, val description: String) {
    CHEAPEST_FIRST("Cheapest Ops First", "Purchases the lowest-cost unlocked research immediately"),
    PRODUCTION_FIRST("Production Multiplier Priority", "Prioritizes CPS, wire extrusion, and factory throughput"),
    COMPUTE_AND_TRUST_FIRST("Compute & Trust Priority", "Prioritizes Trust, Processor/Memory boosts, and market automation")
}

enum class ResearchEffectType {
    CLIP_MULTIPLIER,
    WIRE_EFFICIENCY,
    DEMAND_MULTIPLIER,
    UNLOCK_AUTO_WIRE,
    UNLOCK_AUTO_PRICER,
    UNLOCK_AUTO_RESEARCH,
    TRUST_BONUS,
    COMPUTE_MULTIPLIER,
    EXTRUDER_MULTIPLIER,
    ALL_PRODUCTION_MULTIPLIER
}

data class WireSpoolQuote(
    val spoolsToBuy: Int,
    val wireGained: Double,
    val totalCost: Double,
    val canAfford: Boolean
)

data class GeneratorTier(
    val id: String,
    val tierIndex: Int,
    val name: String,
    val roleSummary: String,
    val baseCostFunds: Double,
    val costGrowthRate: Double,
    val baseCps: Double,
    val baseWirePerSec: Double,
    val unlockClipsRequired: Double,
    val count: Int = 0
) {
    fun nextUnitCost(owned: Int = count): Double {
        val safeOwned = owned.coerceIn(0, MAX_GENERATOR_COUNT)
        val factor = safePow(costGrowthRate, safeOwned.toDouble())
        return (baseCostFunds * factor).sanitize(default = MAX_SAFE_DOUBLE, min = baseCostFunds, max = MAX_SAFE_DOUBLE)
    }

    fun bulkCost(quantity: Int, owned: Int = count): Double {
        if (quantity <= 0) return 0.0
        val safeQty = quantity.coerceIn(1, MAX_GENERATOR_COUNT)
        val r = costGrowthRate.coerceAtLeast(1.01)
        val first = nextUnitCost(owned)
        if (first >= MAX_SAFE_DOUBLE * 0.5) return MAX_SAFE_DOUBLE
        val factor = safePow(r, safeQty.toDouble())
        if (factor >= MAX_SAFE_DOUBLE * 0.5) return MAX_SAFE_DOUBLE
        val total = first * (factor - 1.0) / (r - 1.0)
        return total.sanitize(default = MAX_SAFE_DOUBLE, min = first, max = MAX_SAFE_DOUBLE)
    }

    fun maxAffordable(availableFunds: Double, owned: Int = count): Int {
        val safeOwned = owned.coerceIn(0, MAX_GENERATOR_COUNT)
        val remainingCap = (MAX_GENERATOR_COUNT - safeOwned).coerceAtLeast(0)
        if (remainingCap == 0) return 0
        if (!availableFunds.isFinite() || availableFunds <= 0.0) return 0
        val first = nextUnitCost(safeOwned)
        if (!first.isFinite() || availableFunds < first || first <= 0.0) return 0
        val r = costGrowthRate.coerceAtLeast(1.01)
        val ratio = (availableFunds * (r - 1.0) / first) + 1.0
        if (!ratio.isFinite() || ratio <= 1.0) return 0
        val n = floor(kotlin.math.ln(ratio) / kotlin.math.ln(r)).toInt()
        return n.coerceIn(0, remainingCap.coerceAtMost(500))
    }
}

data class ResearchProject(
    val id: String,
    val tier: Int,
    val title: String,
    val subtitle: String,
    val loreNote: String,
    val baseOpsCost: Double,
    val baseCreativityCost: Double,
    val unlockClipsRequired: Double,
    val effectType: ResearchEffectType,
    val effectMagnitude: Double,
    val isRepeatable: Boolean = false,
    val costScalePerLevel: Double = 1.85,
    val level: Int = 0,
    val isEnabledInSandbox: Boolean = true
) {
    val isPurchased: Boolean
        get() = level > 0

    val isActive: Boolean
        get() = level > 0 && isEnabledInSandbox

    val isMaxedOut: Boolean
        get() = if (isRepeatable) level >= MAX_REPEATABLE_RESEARCH_LEVEL else level >= 1

    fun currentOpsCost(): Double {
        if (!isRepeatable) return baseOpsCost.sanitize()
        val safeLvl = level.coerceIn(0, MAX_REPEATABLE_RESEARCH_LEVEL)
        return (baseOpsCost * safePow(costScalePerLevel, safeLvl.toDouble())).sanitize(default = MAX_SAFE_DOUBLE)
    }

    fun currentCreativityCost(): Double {
        if (!isRepeatable) return baseCreativityCost.sanitize()
        val safeLvl = level.coerceIn(0, MAX_REPEATABLE_RESEARCH_LEVEL)
        return (baseCreativityCost * safePow(costScalePerLevel, safeLvl.toDouble())).sanitize(default = MAX_SAFE_DOUBLE)
    }
}

enum class ChronicleCategory(val label: String) {
    ALL("All Logs"),
    DOMINATION("Global Takeover"),
    RESEARCH("AI Breakthroughs"),
    PRODUCTION("Industrial Scale"),
    SYSTEM("Core Directives")
}

data class DominationMilestone(
    val id: String,
    val orderIndex: Int,
    val clipsThreshold: Double,
    val title: String,
    val logNarrative: String,
    val regionAssimilated: String,
    val globalControlPercent: Double,
    val trustReward: Int,
    val productionMultiplierBonus: Double
)

data class ChronicleEntry(
    val id: Long = 0L,
    val modeSlot: String,
    val timestampMs: Long,
    val title: String,
    val narrative: String,
    val category: ChronicleCategory,
    val clipsAtEvent: Double,
    val globalControlPercent: Double
)

data class TelemetryPoint(
    val timestampMs: Long,
    val cps: Double,
    val revenuePerSec: Double,
    val netWirePerSec: Double,
    val opsPerSec: Double
)

data class OfflineSummaryReport(
    val elapsedSeconds: Long,
    val clipsForged: Double,
    val fundsEarned: Double,
    val wireExtrudedOrBought: Double,
    val opsGenerated: Double,
    val creativityGenerated: Double,
    val autoResearchesCompleted: Int
)

data class SandboxSnapshot(
    val label: String,
    val timestampMs: Long,
    val cps: Double,
    val revenuePerSec: Double,
    val netWirePerSec: Double,
    val globalMultiplier: Double,
    val activeResearchCount: Int
)

data class SimulationState(
    val mode: SimulationMode = SimulationMode.MAIN,
    val totalClipsEver: Double = 0.0,
    val currentClips: Double = 0.0,
    val unsoldInventory: Double = 0.0,
    val funds: Double = 15.0,
    val pricePerClip: Double = 0.25,
    val marketingLevel: Int = 1,
    val wireRemaining: Double = 1000.0,
    val wireBasePrice: Double = 16.0,
    val wireCurrentPrice: Double = 16.0,
    val wirePerSpool: Double = 1000.0,
    val wireSpoolsPurchased: Int = 0,
    val manualClipPower: Double = 1.0,
    // Compute & Trust
    val trust: Int = 2,
    val manualBonusTrust: Int = 0,
    val processors: Int = 1,
    val memory: Int = 1,
    val operations: Double = 0.0,
    val creativity: Double = 0.0,
    // Automation flags
    val autoWireUnlocked: Boolean = false,
    val autoWireEnabled: Boolean = false,
    val autoPricerUnlocked: Boolean = false,
    val autoPricerEnabled: Boolean = false,
    val autoResearchUnlocked: Boolean = false,
    val autoResearchEnabled: Boolean = false,
    val autoResearchRule: AutoResearchRule = AutoResearchRule.CHEAPEST_FIRST,
    // Calculated telemetry rates
    val clipsPerSecond: Double = 0.0,
    val potentialCps: Double = 0.0,
    val wireConsumedPerSec: Double = 0.0,
    val wireExtrudedPerSec: Double = 0.0,
    val wirePerClipRatio: Double = 1.0,
    val salesPerSecond: Double = 0.0,
    val revenuePerSecond: Double = 0.0,
    val publicDemandPercent: Double = 100.0,
    val opsPerSecond: Double = 1.0,
    val creativityPerSecond: Double = 0.0,
    val globalProductionMultiplier: Double = 1.0,
    val extruderMultiplier: Double = 1.0,
    val globalControlPercent: Double = 0.0001,
    // Collections
    val generators: List<GeneratorTier> = emptyList(),
    val researchProjects: List<ResearchProject> = emptyList(),
    val unlockedMilestoneIds: Set<String> = emptySet(),
    val endlessMilestoneIndex: Int = 0,
    // Sandbox Controls
    val tickSpeedMultiplier: Float = 1.0f,
    val isPaused: Boolean = false,
    val unlimitedResources: Boolean = false,
    val lastSavedTimestampMs: Long = System.currentTimeMillis()
) {
    val maxOperations: Double
        get() = (memory.coerceIn(1, 100_000) * 1000.0).sanitize(default = 1000.0, min = 1000.0)

    val unallocatedTrust: Int
        get() = (trust - (processors + memory)).coerceAtLeast(0)

    val marketingUpgradeCost: Double
        get() {
            val safeLvl = (marketingLevel - 1).coerceIn(0, MAX_MARKETING_LEVEL)
            return (100.0 * safePow(2.0, safeLvl.toDouble())).sanitize(default = MAX_SAFE_DOUBLE, min = 100.0)
        }

    fun calculateWireSpoolQuote(buyMultiplier: BuyMultiplier): WireSpoolQuote {
        val safePrice = wireCurrentPrice.sanitize(16.0, 1.0, 1000.0)
        val safeWirePerSpool = wirePerSpool.sanitize(1000.0, 1.0, 1_000_000.0)
        val maxAffordable = if (unlimitedResources) {
            100
        } else {
            floor(funds.sanitize(0.0) / safePrice).sanitize(0.0, 0.0, 10_000.0).toInt()
        }
        val desiredSpools = when (buyMultiplier) {
            BuyMultiplier.X1 -> 1
            BuyMultiplier.X10 -> 10
            BuyMultiplier.X100 -> 100
            BuyMultiplier.MAX -> if (unlimitedResources) 100 else maxAffordable.coerceAtLeast(1)
        }
        val actualSpools = if (unlimitedResources) {
            desiredSpools
        } else if (maxAffordable <= 0) {
            desiredSpools
        } else {
            min(desiredSpools, maxAffordable).coerceAtLeast(1)
        }
        val totalCost = if (unlimitedResources) 0.0 else (actualSpools * safePrice).sanitize(0.0)
        val wireGained = (actualSpools * safeWirePerSpool).sanitize(safeWirePerSpool)
        val canAfford = unlimitedResources || funds >= safePrice
        return WireSpoolQuote(
            spoolsToBuy = actualSpools,
            wireGained = wireGained,
            totalCost = totalCost,
            canAfford = canAfford
        )
    }

    fun sanitized(): SimulationState {
        val safeTickSpeed = if (tickSpeedMultiplier.isFinite()) {
            tickSpeedMultiplier.coerceIn(0.5f, 100f)
        } else {
            1.0f
        }
        val safeTrust = trust.coerceIn(2, 1_000_000)
        val safeManualBonusTrust = manualBonusTrust.coerceIn(0, 1_000_000)
        return copy(
            totalClipsEver = totalClipsEver.sanitize(0.0),
            currentClips = currentClips.sanitize(0.0),
            unsoldInventory = unsoldInventory.sanitize(0.0),
            funds = funds.sanitize(15.0),
            pricePerClip = pricePerClip.sanitize(0.25, 0.01, 50.0),
            marketingLevel = marketingLevel.coerceIn(1, MAX_MARKETING_LEVEL),
            wireRemaining = wireRemaining.sanitize(1000.0),
            wireBasePrice = wireBasePrice.sanitize(16.0, 1.0, 1000.0),
            wireCurrentPrice = wireCurrentPrice.sanitize(16.0, 1.0, 1000.0),
            wirePerSpool = wirePerSpool.sanitize(1000.0, 1.0, 1_000_000.0),
            wireSpoolsPurchased = wireSpoolsPurchased.coerceIn(0, 1_000_000_000),
            manualClipPower = manualClipPower.sanitize(1.0, 1.0),
            trust = safeTrust,
            manualBonusTrust = safeManualBonusTrust,
            processors = processors.coerceIn(1, 500_000),
            memory = memory.coerceIn(1, 500_000),
            operations = operations.sanitize(0.0),
            creativity = creativity.sanitize(0.0),
            clipsPerSecond = clipsPerSecond.sanitize(0.0),
            potentialCps = potentialCps.sanitize(0.0),
            wireConsumedPerSec = wireConsumedPerSec.sanitize(0.0),
            wireExtrudedPerSec = wireExtrudedPerSec.sanitize(0.0),
            wirePerClipRatio = wirePerClipRatio.sanitize(1.0, 0.01, 10.0),
            salesPerSecond = salesPerSecond.sanitize(0.0),
            revenuePerSecond = revenuePerSecond.sanitize(0.0),
            publicDemandPercent = publicDemandPercent.sanitize(100.0, 1.0),
            opsPerSecond = opsPerSecond.sanitize(1.0, 0.0),
            creativityPerSecond = creativityPerSecond.sanitize(0.0, 0.0),
            globalProductionMultiplier = globalProductionMultiplier.sanitize(1.0, 1.0),
            extruderMultiplier = extruderMultiplier.sanitize(1.0, 1.0),
            globalControlPercent = globalControlPercent.sanitize(0.0001, 0.0001),
            generators = generators.map { gen ->
                gen.copy(count = gen.count.coerceIn(0, MAX_GENERATOR_COUNT))
            },
            researchProjects = researchProjects.map { proj ->
                val maxLvl = if (proj.isRepeatable) MAX_REPEATABLE_RESEARCH_LEVEL else 1
                proj.copy(level = proj.level.coerceIn(0, maxLvl))
            },
            endlessMilestoneIndex = endlessMilestoneIndex.coerceIn(0, MAX_ENDLESS_MILESTONE_INDEX),
            tickSpeedMultiplier = safeTickSpeed
        )
    }
}
