package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SimulationCatalog
import com.example.data.local.AppPreferencesEntity
import com.example.data.local.ClipDatabase
import com.example.data.local.SimulationRepository
import com.example.model.AutoResearchRule
import com.example.model.BuyMultiplier
import com.example.model.ChronicleCategory
import com.example.model.ChronicleEntry
import com.example.model.MAX_ENDLESS_MILESTONE_INDEX
import com.example.model.MAX_GENERATOR_COUNT
import com.example.model.MAX_MARKETING_LEVEL
import com.example.model.MAX_REPEATABLE_RESEARCH_LEVEL
import com.example.model.OfflineSummaryReport
import com.example.model.ResearchEffectType
import com.example.model.ResearchProject
import com.example.model.SandboxSnapshot
import com.example.model.SimulationMode
import com.example.model.SimulationState
import com.example.model.TelemetryPoint
import com.example.ui.theme.ClipThemePreset
import com.example.util.ClipHapticManager
import com.example.util.HapticProfile
import com.example.util.MAX_SAFE_DOUBLE
import com.example.util.NumberFormatter
import com.example.util.NumberNotation
import com.example.util.safePow
import com.example.util.sanitize
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DerivedMultipliers(
    val clipMultiplier: Double,
    val wirePerClipRatio: Double,
    val demandMultiplier: Double,
    val extruderMultiplier: Double,
    val computeMultiplier: Double,
    val bonusTrust: Int,
    val autoWireUnlocked: Boolean,
    val autoPricerUnlocked: Boolean,
    val autoResearchUnlocked: Boolean,
    val globalControlPercent: Double
)

class SimulationViewModel(
    private val repository: SimulationRepository,
    val hapticManager: ClipHapticManager
) : ViewModel() {

    private val _activeMode = MutableStateFlow(SimulationMode.MAIN)
    val activeMode: StateFlow<SimulationMode> = _activeMode.asStateFlow()

    private val _mainState = MutableStateFlow(SimulationState(mode = SimulationMode.MAIN))
    private val _sandboxState = MutableStateFlow(SimulationState(mode = SimulationMode.SANDBOX))

    private val _uiState = MutableStateFlow(SimulationState(mode = SimulationMode.MAIN))
    val uiState: StateFlow<SimulationState> = _uiState.asStateFlow()

    private val _buyMultiplier = MutableStateFlow(BuyMultiplier.X1)
    val buyMultiplier: StateFlow<BuyMultiplier> = _buyMultiplier.asStateFlow()

    private val _themePreset = MutableStateFlow(ClipThemePreset.OLED_MIDNIGHT)
    val themePreset: StateFlow<ClipThemePreset> = _themePreset.asStateFlow()

    private val _hapticProfile = MutableStateFlow(HapticProfile.FLAGSHIP_CRISP)
    val hapticProfile: StateFlow<HapticProfile> = _hapticProfile.asStateFlow()

    private val _numberNotation = MutableStateFlow(NumberNotation.STANDARD)
    val numberNotation: StateFlow<NumberNotation> = _numberNotation.asStateFlow()

    private val _offlineReport = MutableStateFlow<OfflineSummaryReport?>(null)
    val offlineReport: StateFlow<OfflineSummaryReport?> = _offlineReport.asStateFlow()

    private val _telemetryHistory = MutableStateFlow<List<TelemetryPoint>>(emptyList())
    val telemetryHistory: StateFlow<List<TelemetryPoint>> = _telemetryHistory.asStateFlow()

    private val _sandboxSnapshots = MutableStateFlow<List<SandboxSnapshot>>(emptyList())
    val sandboxSnapshots: StateFlow<List<SandboxSnapshot>> = _sandboxSnapshots.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val chronicleEntries: StateFlow<List<ChronicleEntry>> = _activeMode
        .flatMapLatest { mode -> repository.observeChronicle(mode) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var isInitialized = false
    private var tickCounter = 0L
    private var simulationLoopJob: Job? = null
    private var isSandboxDirty = false

    init {
        viewModelScope.launch {
            repository.preferencesFlow.collect { prefs ->
                _themePreset.value = ClipThemePreset.fromId(prefs.themeId)
                _hapticProfile.value = runCatching { HapticProfile.valueOf(prefs.hapticProfileName) }
                    .getOrDefault(HapticProfile.FLAGSHIP_CRISP)
                _numberNotation.value = runCatching { NumberNotation.valueOf(prefs.notationName) }
                    .getOrDefault(NumberNotation.STANDARD)
            }
        }

        viewModelScope.launch {
            val savedPrefs = repository.getPreferencesOnce()
            val restoredMode = runCatching { SimulationMode.valueOf(savedPrefs.activeModeSlot) }
                .getOrDefault(SimulationMode.MAIN)
            _activeMode.value = restoredMode
            _sandboxSnapshots.value = repository.deserializeSnapshots(savedPrefs.sandboxSnapshotsJson)

            val loadedMain = recalculateDerivedRates(repository.loadStateOrCreateDefault(SimulationMode.MAIN))
            val loadedSandbox = recalculateDerivedRates(repository.loadStateOrCreateDefault(SimulationMode.SANDBOX))

            val now = System.currentTimeMillis()
            val elapsedSec = ((now - loadedMain.lastSavedTimestampMs) / 1000L).coerceAtLeast(0L)
            val caughtUpMain = if (elapsedSec >= 5L && loadedMain.potentialCps > 0.0) {
                applyOfflineCatchUp(loadedMain, elapsedSec)
            } else {
                loadedMain.copy(lastSavedTimestampMs = now).sanitized()
            }

            _mainState.value = caughtUpMain
            _sandboxState.value = loadedSandbox.copy(lastSavedTimestampMs = now).sanitized()
            _uiState.value = if (_activeMode.value == SimulationMode.MAIN) _mainState.value else _sandboxState.value
            isInitialized = true

            startSimulationLoop()
        }
    }

    private fun startSimulationLoop() {
        if (simulationLoopJob?.isActive == true) return
        simulationLoopJob = viewModelScope.launch {
            while (isActive) {
                delay(100L) // 10 ticks per second base cadence
                if (!isInitialized) continue

                tickCounter++
                val allowAutoResearch = (tickCounter % 10L == 0L)

                // Always advance MAIN simulation at 1.0x while in foreground
                _mainState.value = stepSimulation(
                    rawState = _mainState.value,
                    dtSeconds = 0.1,
                    allowAutoResearch = allowAutoResearch
                )

                // Advance SANDBOX simulation only when active so it never runs runaway loops unseen
                if (_activeMode.value == SimulationMode.SANDBOX) {
                    val currentSandbox = _sandboxState.value
                    _sandboxState.value = if (currentSandbox.isPaused) {
                        recalculateDerivedRates(currentSandbox)
                    } else {
                        isSandboxDirty = true
                        val safeSpeed = currentSandbox.tickSpeedMultiplier.takeIf { it.isFinite() }?.coerceIn(0.5f, 100f) ?: 1.0f
                        val scaledDt = 0.1 * safeSpeed.toDouble()
                        stepSimulation(
                            rawState = currentSandbox,
                            dtSeconds = scaledDt,
                            allowAutoResearch = allowAutoResearch
                        )
                    }
                }

                val currentActive = if (_activeMode.value == SimulationMode.MAIN) {
                    _mainState.value
                } else {
                    _sandboxState.value
                }
                _uiState.value = currentActive

                // Record telemetry snapshot every 5 ticks (0.5s) up to 48 points
                if (tickCounter % 5L == 0L) {
                    val point = TelemetryPoint(
                        timestampMs = System.currentTimeMillis(),
                        cps = currentActive.clipsPerSecond.sanitize(0.0),
                        revenuePerSec = currentActive.revenuePerSecond.sanitize(0.0),
                        netWirePerSec = (currentActive.wireExtrudedPerSec - currentActive.wireConsumedPerSec)
                            .sanitize(0.0, -MAX_SAFE_DOUBLE, MAX_SAFE_DOUBLE),
                        opsPerSec = currentActive.opsPerSecond.sanitize(0.0)
                    )
                    _telemetryHistory.update { list ->
                        (list + point).takeLast(48)
                    }
                }

                // Auto-save every 50 ticks (5 seconds)
                if (tickCounter % 50L == 0L) {
                    saveProfiles(forceBoth = false)
                }
            }
        }
    }

    fun onAppResumed() {
        if (!isInitialized) return
        val now = System.currentTimeMillis()
        val lastSaved = _mainState.value.lastSavedTimestampMs
        val elapsedSec = ((now - lastSaved) / 1000L).coerceAtLeast(0L)
        if (elapsedSec >= 5L && _mainState.value.potentialCps > 0.0) {
            val updated = applyOfflineCatchUp(_mainState.value, elapsedSec)
            _mainState.value = updated
            if (_activeMode.value == SimulationMode.MAIN) {
                _uiState.value = updated
            }
        } else {
            _mainState.update { it.copy(lastSavedTimestampMs = now).sanitized() }
        }
        startSimulationLoop()
    }

    fun onAppPaused() {
        simulationLoopJob?.cancel()
        simulationLoopJob = null
        if (!isInitialized) return
        viewModelScope.launch {
            saveProfiles(forceBoth = true)
        }
    }

    private suspend fun saveProfiles(forceBoth: Boolean) {
        val now = System.currentTimeMillis()
        _mainState.update { it.copy(lastSavedTimestampMs = now).sanitized() }
        repository.saveState(_mainState.value)

        if (forceBoth || isSandboxDirty || _activeMode.value == SimulationMode.SANDBOX) {
            _sandboxState.update { it.copy(lastSavedTimestampMs = now).sanitized() }
            repository.saveState(_sandboxState.value)
            isSandboxDirty = false
        }
    }

    internal fun applyOfflineCatchUp(rawState: SimulationState, elapsedSeconds: Long): SimulationState {
        val state = rawState.sanitized()
        val cappedSeconds = elapsedSeconds.coerceIn(0L, 86_400L * 7L) // Up to 7 days full offline catch-up
        val dt = cappedSeconds.toDouble()
        val derived = computeDerivedMultipliers(state)

        val rawWireGenerated = (state.wireExtrudedPerSec * dt).sanitize(0.0)
        var workingFunds = state.funds.sanitize(0.0)
        var workingWire = (state.wireRemaining + rawWireGenerated).sanitize(0.0)
        var extraWireBought = 0.0

        val desiredWire = (state.potentialCps * derived.wirePerClipRatio * dt).sanitize(0.0)
        if (workingWire < desiredWire && (state.autoWireEnabled || state.unlimitedResources)) {
            if (state.unlimitedResources) {
                extraWireBought = (desiredWire - workingWire).sanitize(0.0)
                workingWire = desiredWire
            } else {
                val safeWirePrice = state.wireCurrentPrice.sanitize(16.0, 1.0, 1000.0)
                val safeWirePerSpool = state.wirePerSpool.sanitize(1000.0, 1.0, 1_000_000.0)
                val spoolsNeeded = ceil((desiredWire - workingWire) / safeWirePerSpool).sanitize(0.0, 0.0, 1.0e12)
                val affordableSpools = floor(workingFunds / safeWirePrice).sanitize(0.0, 0.0, 1.0e12)
                val spoolsToBuy = min(spoolsNeeded, affordableSpools).coerceAtLeast(0.0)
                if (spoolsToBuy > 0.0) {
                    workingFunds = (workingFunds - spoolsToBuy * safeWirePrice).sanitize(0.0)
                    extraWireBought = (spoolsToBuy * safeWirePerSpool).sanitize(0.0)
                    workingWire = (workingWire + extraWireBought).sanitize(0.0)
                }
            }
        }

        val maxClipsFromWire = if (state.unlimitedResources) {
            (state.potentialCps * dt).sanitize(0.0)
        } else {
            (workingWire / derived.wirePerClipRatio).sanitize(0.0)
        }
        val clipsForged = min((state.potentialCps * dt).sanitize(0.0), maxClipsFromWire).sanitize(0.0)
        val wireUsed = if (state.unlimitedResources) 0.0 else (clipsForged * derived.wirePerClipRatio).sanitize(0.0)
        workingWire = (workingWire - wireUsed).sanitize(0.0)

        val totalSellable = (state.unsoldInventory + clipsForged).sanitize(0.0)
        val potentialSales = (state.salesPerSecond * dt).sanitize(0.0)
        val clipsSold = min(totalSellable, potentialSales).sanitize(0.0)
        val fundsEarned = (clipsSold * state.pricePerClip).sanitize(0.0)
        workingFunds = (workingFunds + fundsEarned).sanitize(0.0)

        val totalRawOpsGenerated = (state.opsPerSecond * dt).sanitize(0.0)
        val opsGained = totalRawOpsGenerated.coerceAtMost((state.maxOperations - state.operations).sanitize(0.0))
        val creativityGained = (state.creativityPerSecond * dt).sanitize(0.0)

        val newTotalClips = (state.totalClipsEver + clipsForged).sanitize(0.0)
        var workingOpsPool = (state.operations + totalRawOpsGenerated).sanitize(0.0)
        var workingCreativityPool = (state.creativity + creativityGained).sanitize(0.0)
        var workingResearch = state.researchProjects
        var autoResearchesCompleted = 0

        if (state.autoResearchUnlocked && state.autoResearchEnabled) {
            for (step in 0 until 25) {
                val chosen = selectNextAutoResearchCandidate(
                    researchProjects = workingResearch,
                    rule = state.autoResearchRule,
                    unlimitedResources = state.unlimitedResources,
                    totalClipsEver = newTotalClips,
                    availableOps = workingOpsPool,
                    availableCreativity = workingCreativityPool
                ) ?: break

                if (!state.unlimitedResources) {
                    workingOpsPool = (workingOpsPool - chosen.currentOpsCost()).sanitize(0.0)
                    workingCreativityPool = (workingCreativityPool - chosen.currentCreativityCost()).sanitize(0.0)
                }
                val maxLvl = if (chosen.isRepeatable) MAX_REPEATABLE_RESEARCH_LEVEL else 1
                val newLevel = (chosen.level + 1).coerceIn(1, maxLvl)
                workingResearch = workingResearch.map {
                    if (it.id == chosen.id) it.copy(level = newLevel, isEnabledInSandbox = true) else it
                }
                autoResearchesCompleted++
            }
        }

        val finalOps = workingOpsPool.coerceAtMost(state.maxOperations).sanitize(0.0, 0.0, state.maxOperations)

        val updatedState = recalculateDerivedRates(
            state.copy(
                totalClipsEver = newTotalClips,
                currentClips = (state.currentClips + clipsForged).sanitize(0.0),
                unsoldInventory = (totalSellable - clipsSold).sanitize(0.0),
                funds = workingFunds,
                wireRemaining = workingWire,
                operations = finalOps,
                creativity = workingCreativityPool,
                researchProjects = workingResearch,
                lastSavedTimestampMs = System.currentTimeMillis()
            )
        )

        if (clipsForged >= 1.0 || fundsEarned >= 1.0 || opsGained >= 10.0 || autoResearchesCompleted > 0) {
            _offlineReport.value = OfflineSummaryReport(
                elapsedSeconds = cappedSeconds,
                clipsForged = clipsForged,
                fundsEarned = fundsEarned,
                wireExtrudedOrBought = (rawWireGenerated + extraWireBought).sanitize(0.0),
                opsGenerated = opsGained,
                creativityGenerated = creativityGained,
                autoResearchesCompleted = autoResearchesCompleted
            )
        }

        return updatedState.sanitized()
    }

    fun dismissOfflineReport() {
        hapticManager.performClipSnap(_hapticProfile.value)
        _offlineReport.value = null
    }

    internal fun computeDerivedMultipliers(state: SimulationState): DerivedMultipliers {
        var clipMult = 1.0
        var wireRatio = 1.0
        var demandMult = 1.0
        var extruderMult = 1.0
        var computeMult = 1.0
        var bonusTrustLong = 0L
        var autoWire = false
        var autoPricer = false
        var autoResearch = false

        state.researchProjects.forEach { proj ->
            if (proj.isActive) {
                val maxLvl = if (proj.isRepeatable) MAX_REPEATABLE_RESEARCH_LEVEL else 1
                val lvl = proj.level.coerceIn(1, maxLvl)
                when (proj.effectType) {
                    ResearchEffectType.CLIP_MULTIPLIER -> {
                        clipMult = (clipMult * safePow(proj.effectMagnitude, lvl.toDouble())).sanitize(1.0, 1.0)
                    }
                    ResearchEffectType.WIRE_EFFICIENCY -> {
                        wireRatio = (wireRatio * safePow(proj.effectMagnitude, lvl.toDouble())).sanitize(1.0, 0.01, 10.0)
                    }
                    ResearchEffectType.DEMAND_MULTIPLIER -> {
                        demandMult = (demandMult * safePow(proj.effectMagnitude, lvl.toDouble())).sanitize(1.0, 1.0)
                        // Secondary bonus for Mandatory Fastener Legislation ("and clip output by ×2.0")
                        if (proj.id == "res_global_monopoly") {
                            clipMult = (clipMult * safePow(2.0, lvl.toDouble())).sanitize(1.0, 1.0)
                        }
                    }
                    ResearchEffectType.UNLOCK_AUTO_WIRE -> {
                        autoWire = true
                    }
                    ResearchEffectType.UNLOCK_AUTO_PRICER -> {
                        autoPricer = true
                    }
                    ResearchEffectType.UNLOCK_AUTO_RESEARCH -> {
                        autoResearch = true
                    }
                    ResearchEffectType.TRUST_BONUS -> {
                        bonusTrustLong = (bonusTrustLong + proj.effectMagnitude.toLong() * lvl).coerceIn(0L, 500_000L)
                        when (proj.id) {
                            "res_synthetic_trust" -> {
                                // "Grants +4 Trust immediately and boosts all production by +75%"
                                clipMult = (clipMult * safePow(1.75, lvl.toDouble())).sanitize(1.0, 1.0)
                            }
                            "res_neural_annexation" -> {
                                // "Grants +6 Trust and triples compute speed (×3.0)"
                                computeMult = (computeMult * safePow(3.0, lvl.toDouble())).sanitize(1.0, 1.0)
                            }
                            "res_endless_matrioshka_compute" -> {
                                // "Repeatable: Grants +2 Trust & boosts Compute speed by +60% per level"
                                computeMult = (computeMult * safePow(1.60, lvl.toDouble())).sanitize(1.0, 1.0)
                            }
                            else -> {
                                clipMult = (clipMult * safePow(1.35, lvl.toDouble())).sanitize(1.0, 1.0)
                                computeMult = (computeMult * safePow(1.40, lvl.toDouble())).sanitize(1.0, 1.0)
                            }
                        }
                    }
                    ResearchEffectType.COMPUTE_MULTIPLIER -> {
                        computeMult = (computeMult * safePow(proj.effectMagnitude, lvl.toDouble())).sanitize(1.0, 1.0)
                    }
                    ResearchEffectType.EXTRUDER_MULTIPLIER -> {
                        extruderMult = (extruderMult * safePow(proj.effectMagnitude, lvl.toDouble())).sanitize(1.0, 1.0)
                    }
                    ResearchEffectType.ALL_PRODUCTION_MULTIPLIER -> {
                        val factor = safePow(proj.effectMagnitude, lvl.toDouble())
                        clipMult = (clipMult * factor).sanitize(1.0, 1.0)
                        extruderMult = (extruderMult * factor).sanitize(1.0, 1.0)
                        // Secondary bonus for Self-Replicating Clip Probes ("and grants +8 Trust")
                        if (proj.id == "res_von_neumann_probes") {
                            bonusTrustLong = (bonusTrustLong + 8L * lvl).coerceIn(0L, 500_000L)
                        }
                    }
                }
            }
        }

        var controlPercent = 0.0001
        SimulationCatalog.dominationMilestones.forEach { ms ->
            if (state.unlockedMilestoneIds.contains(ms.id)) {
                clipMult = (clipMult * ms.productionMultiplierBonus).sanitize(1.0, 1.0)
                bonusTrustLong = (bonusTrustLong + ms.trustReward).coerceIn(0L, 500_000L)
                controlPercent = max(controlPercent, ms.globalControlPercent).sanitize(0.0001, 0.0001)
            }
        }

        val safeEndlessIdx = state.endlessMilestoneIndex.coerceIn(0, MAX_ENDLESS_MILESTONE_INDEX)
        if (safeEndlessIdx > 0) {
            val endlessFactor = safePow(2.5, safeEndlessIdx.toDouble())
            clipMult = (clipMult * endlessFactor).sanitize(1.0, 1.0)
            extruderMult = (extruderMult * endlessFactor).sanitize(1.0, 1.0)
            bonusTrustLong = (bonusTrustLong + safeEndlessIdx.toLong() * 4L).coerceIn(0L, 500_000L)
            val cosmicControl = (100_000.0 * safePow(10.0, safeEndlessIdx.toDouble())).sanitize(100_000.0, 0.0001)
            controlPercent = max(controlPercent, cosmicControl).sanitize(0.0001, 0.0001)
        }

        return DerivedMultipliers(
            clipMultiplier = clipMult.sanitize(1.0, 1.0),
            wirePerClipRatio = wireRatio.sanitize(1.0, 0.01, 10.0),
            demandMultiplier = demandMult.sanitize(1.0, 1.0),
            extruderMultiplier = extruderMult.sanitize(1.0, 1.0),
            computeMultiplier = computeMult.sanitize(1.0, 1.0),
            bonusTrust = bonusTrustLong.toInt(),
            autoWireUnlocked = autoWire,
            autoPricerUnlocked = autoPricer,
            autoResearchUnlocked = autoResearch,
            globalControlPercent = controlPercent.sanitize(0.0001, 0.0001)
        )
    }

    internal fun recalculateDerivedRates(rawState: SimulationState): SimulationState {
        val state = rawState.sanitized()
        val derived = computeDerivedMultipliers(state)

        val baseModeTrust = if (state.mode == SimulationMode.SANDBOX) 10 else 2
        val finalTrust = (baseModeTrust + state.manualBonusTrust.coerceIn(0, 1_000_000) + derived.bonusTrust)
            .coerceIn(2, 1_000_000)

        // Ensure allocated processors + memory never exceed finalTrust when toggling trust upgrades off in Sandbox
        var safeProcessors = state.processors.coerceIn(1, 500_000)
        var safeMemory = state.memory.coerceIn(1, 500_000)
        if (safeProcessors + safeMemory > finalTrust) {
            val ratio = finalTrust.toDouble() / (safeProcessors + safeMemory).toDouble()
            safeProcessors = (safeProcessors * ratio).toInt().coerceAtLeast(1)
            safeMemory = (finalTrust - safeProcessors).coerceAtLeast(1)
        }

        val baseCpsSum = state.generators.sumOf {
            it.count.coerceIn(0, MAX_GENERATOR_COUNT).toDouble() * it.baseCps
        }.sanitize(0.0)
        val rawCps = if (baseCpsSum <= 0.0) 0.0 else (baseCpsSum * derived.clipMultiplier).sanitize(0.0)

        val baseWireSum = state.generators.sumOf {
            it.count.coerceIn(0, MAX_GENERATOR_COUNT).toDouble() * it.baseWirePerSec
        }.sanitize(0.0)
        val rawWireExtruded = if (baseWireSum <= 0.0) 0.0 else (baseWireSum * derived.extruderMultiplier).sanitize(0.0)

        val hasWire = state.unlimitedResources || state.wireRemaining > 0.001 || rawWireExtruded > 0.0
        val effectiveCps = if (hasWire) {
            if (state.unlimitedResources || state.wireRemaining >= (rawCps * derived.wirePerClipRatio * 0.1).sanitize(0.0)) {
                rawCps
            } else {
                min(rawCps, ((state.wireRemaining + rawWireExtruded) / derived.wirePerClipRatio).sanitize(0.0))
            }
        } else 0.0

        val wireConsumed = if (state.unlimitedResources) 0.0 else (effectiveCps * derived.wirePerClipRatio).sanitize(0.0)

        val safeMarketingLvl = (state.marketingLevel - 1).coerceIn(0, MAX_MARKETING_LEVEL)
        val marketingBoost = safePow(1.28, safeMarketingLvl.toDouble())
        val priceFactor = safePow(0.32 / state.pricePerClip.sanitize(0.25, 0.01, 50.0), 1.25)
        val demandPct = (priceFactor * marketingBoost * derived.demandMultiplier * 100.0).sanitize(100.0, 1.0)
        val salesPerSec = ((demandPct / 100.0) * (1.8 + effectiveCps * 0.65)).sanitize(0.0)
        val actualSalesRate = if (state.unsoldInventory > 0.5 || effectiveCps > 0.0) {
            min(salesPerSec, (effectiveCps + state.unsoldInventory * 2.0).sanitize(0.0))
        } else 0.0

        val opsPerSec = (safeProcessors * 6.5 * derived.computeMultiplier).sanitize(1.0, 0.0)
        val creativityPerSec = (safeMemory * 1.45 * derived.computeMultiplier).sanitize(0.0, 0.0)
        val newMaxOps = (safeMemory.coerceIn(1, 100_000) * 1000.0).sanitize(1000.0, 1000.0)

        return state.copy(
            trust = finalTrust,
            processors = safeProcessors,
            memory = safeMemory,
            operations = state.operations.coerceAtMost(newMaxOps).sanitize(0.0, 0.0, newMaxOps),
            manualClipPower = ((1.0 + rawCps * 0.03) * derived.clipMultiplier.coerceAtMost(1000.0)).sanitize(1.0, 1.0),
            potentialCps = rawCps,
            clipsPerSecond = effectiveCps,
            wireExtrudedPerSec = rawWireExtruded,
            wireConsumedPerSec = wireConsumed,
            wirePerClipRatio = derived.wirePerClipRatio,
            publicDemandPercent = demandPct,
            salesPerSecond = actualSalesRate,
            revenuePerSecond = (actualSalesRate * state.pricePerClip).sanitize(0.0),
            opsPerSecond = opsPerSec,
            creativityPerSecond = creativityPerSec,
            globalProductionMultiplier = derived.clipMultiplier,
            extruderMultiplier = derived.extruderMultiplier,
            globalControlPercent = derived.globalControlPercent,
            autoWireUnlocked = derived.autoWireUnlocked,
            autoPricerUnlocked = derived.autoPricerUnlocked,
            autoResearchUnlocked = derived.autoResearchUnlocked
        ).sanitized()
    }

    private fun selectNextAutoResearchCandidate(
        researchProjects: List<ResearchProject>,
        rule: AutoResearchRule,
        unlimitedResources: Boolean,
        totalClipsEver: Double,
        availableOps: Double,
        availableCreativity: Double
    ): ResearchProject? {
        val candidates = researchProjects.filter { proj ->
            !proj.isMaxedOut &&
                (unlimitedResources || totalClipsEver >= proj.unlockClipsRequired * 0.25) &&
                (unlimitedResources || (availableOps >= proj.currentOpsCost() && availableCreativity >= proj.currentCreativityCost()))
        }
        return when (rule) {
            AutoResearchRule.CHEAPEST_FIRST -> candidates.minByOrNull {
                (it.currentOpsCost() + it.currentCreativityCost()).sanitize(MAX_SAFE_DOUBLE)
            }
            AutoResearchRule.PRODUCTION_FIRST -> candidates.sortedWith(
                compareByDescending<ResearchProject> {
                    it.effectType == ResearchEffectType.ALL_PRODUCTION_MULTIPLIER ||
                        it.effectType == ResearchEffectType.CLIP_MULTIPLIER ||
                        it.effectType == ResearchEffectType.EXTRUDER_MULTIPLIER
                }.thenBy { it.currentOpsCost() }
            ).firstOrNull()
            AutoResearchRule.COMPUTE_AND_TRUST_FIRST -> candidates.sortedWith(
                compareByDescending<ResearchProject> {
                    it.effectType == ResearchEffectType.TRUST_BONUS ||
                        it.effectType == ResearchEffectType.COMPUTE_MULTIPLIER ||
                        it.effectType == ResearchEffectType.DEMAND_MULTIPLIER
                }.thenBy { it.currentOpsCost() }
            ).firstOrNull()
        }
    }

    internal fun stepSimulation(
        rawState: SimulationState,
        dtSeconds: Double,
        allowAutoResearch: Boolean = true
    ): SimulationState {
        val state = rawState.sanitized()
        val safeDt = dtSeconds.sanitize(0.1, 0.0, 3600.0)
        val derived = computeDerivedMultipliers(state)

        val baseCpsSum = state.generators.sumOf {
            it.count.coerceIn(0, MAX_GENERATOR_COUNT).toDouble() * it.baseCps
        }.sanitize(0.0)
        val rawCps = if (baseCpsSum <= 0.0) 0.0 else (baseCpsSum * derived.clipMultiplier).sanitize(0.0)

        val baseWireSum = state.generators.sumOf {
            it.count.coerceIn(0, MAX_GENERATOR_COUNT).toDouble() * it.baseWirePerSec
        }.sanitize(0.0)
        val rawWireExtruded = if (baseWireSum <= 0.0) 0.0 else (baseWireSum * derived.extruderMultiplier).sanitize(0.0)

        var workingWire = (state.wireRemaining + rawWireExtruded * safeDt).sanitize(0.0)
        var workingFunds = state.funds.sanitize(0.0)
        var workingSpools = state.wireSpoolsPurchased.coerceIn(0, 1_000_000_000)

        // Auto-Wire Buyer
        val targetWireReserve = max(150.0, (rawCps * derived.wirePerClipRatio * 2.0).sanitize(150.0))
        if ((state.autoWireEnabled || state.unlimitedResources) && workingWire < targetWireReserve) {
            if (state.unlimitedResources) {
                workingWire = max(workingWire, max(1000.0, (rawCps * 5.0).sanitize(1000.0))).sanitize(1000.0)
            } else if (workingFunds >= state.wireCurrentPrice) {
                val safeWirePrice = state.wireCurrentPrice.sanitize(16.0, 1.0, 1000.0)
                val safeWirePerSpool = state.wirePerSpool.sanitize(1000.0, 1.0, 1_000_000.0)
                val neededSpools = ceil((targetWireReserve - workingWire) / safeWirePerSpool)
                    .sanitize(1.0, 1.0, 10_000.0)
                    .toInt()
                val maxAffordableSpools = floor((workingFunds * 0.35) / safeWirePrice)
                    .sanitize(1.0, 1.0, 10_000.0)
                    .toInt()
                val spoolsToBuy = min(neededSpools, maxAffordableSpools).coerceIn(1, 10_000)
                val cost = (spoolsToBuy * safeWirePrice).sanitize(0.0)
                if (workingFunds >= cost) {
                    workingFunds = (workingFunds - cost).sanitize(0.0)
                    workingWire = (workingWire + spoolsToBuy * safeWirePerSpool).sanitize(0.0)
                    workingSpools = (workingSpools.toLong() + spoolsToBuy.toLong())
                        .coerceIn(0L, 1_000_000_000L)
                        .toInt()
                }
            }
        }

        // Clip Forging
        val desiredClips = (rawCps * safeDt).sanitize(0.0)
        val availableClipsFromWire = if (state.unlimitedResources) {
            desiredClips
        } else {
            (workingWire / derived.wirePerClipRatio).sanitize(0.0)
        }
        val clipsForged = min(desiredClips, availableClipsFromWire).sanitize(0.0)
        if (!state.unlimitedResources) {
            val wireUsed = (clipsForged * derived.wirePerClipRatio).sanitize(0.0)
            workingWire = (workingWire - wireUsed).sanitize(0.0)
        }

        val newTotalClips = (state.totalClipsEver + clipsForged).sanitize(0.0)
        var workingInventory = (state.unsoldInventory + clipsForged).sanitize(0.0)

        // Auto-Pricer AI
        var workingPrice = state.pricePerClip.sanitize(0.25, 0.01, 50.0)
        if (derived.autoPricerUnlocked && state.autoPricerEnabled) {
            val targetInventory = max(25.0, (rawCps * 3.5).sanitize(25.0))
            if (workingInventory > targetInventory * 1.4 && workingPrice > 0.04) {
                workingPrice = (round((workingPrice - 0.01) * 100.0) / 100.0).coerceAtLeast(0.02)
            } else if (workingInventory < targetInventory * 0.5 && rawCps > 0.0 && workingPrice < 25.0) {
                workingPrice = (round((workingPrice + 0.01) * 100.0) / 100.0).coerceAtMost(25.0)
            }
        }

        // Market Sales
        val safeMarketingLvl = (state.marketingLevel - 1).coerceIn(0, MAX_MARKETING_LEVEL)
        val marketingBoost = safePow(1.28, safeMarketingLvl.toDouble())
        val priceFactor = safePow(0.32 / workingPrice.coerceAtLeast(0.01), 1.25)
        val demandPct = (priceFactor * marketingBoost * derived.demandMultiplier * 100.0).sanitize(100.0, 1.0)
        val salesVelocity = ((demandPct / 100.0) * (1.8 + rawCps * 0.65)).sanitize(0.0)
        val clipsSold = min(workingInventory, (salesVelocity * safeDt).sanitize(0.0)).sanitize(0.0)
        workingInventory = (workingInventory - clipsSold).sanitize(0.0)
        workingFunds = (workingFunds + (clipsSold * workingPrice).sanitize(0.0)).sanitize(0.0)

        // Fluctuating wire price ($12 to $26)
        val phase = (System.currentTimeMillis() / 9000.0)
        val fluctuatingWirePrice = (17.5 + 4.5 * sin(phase) + 1.5 * cos(phase * 0.37)).sanitize(16.0, 12.0, 26.0)

        // Compute & Creativity — keep operations cleanly capped at state.maxOperations
        val opsGain = (state.processors.coerceIn(1, 500_000) * 6.5 * derived.computeMultiplier * safeDt).sanitize(0.0)
        val maxOps = state.maxOperations
        var workingOps = if (state.unlimitedResources) {
            maxOps
        } else {
            (state.operations + opsGain).sanitize(0.0, 0.0, maxOps)
        }

        val creatGain = (state.memory.coerceIn(1, 500_000) * 1.45 * derived.computeMultiplier * safeDt).sanitize(0.0)
        var workingCreativity = if (state.unlimitedResources) {
            max((state.creativity + creatGain).sanitize(0.0), 100_000.0)
        } else {
            (state.creativity + creatGain).sanitize(0.0)
        }

        // Check Domination Milestones
        var workingMilestones = state.unlockedMilestoneIds
        var workingEndlessIdx = state.endlessMilestoneIndex.coerceIn(0, MAX_ENDLESS_MILESTONE_INDEX)
        SimulationCatalog.dominationMilestones.forEach { ms ->
            if (newTotalClips >= ms.clipsThreshold && !workingMilestones.contains(ms.id)) {
                workingMilestones = workingMilestones + ms.id
                if (_activeMode.value == state.mode) {
                    hapticManager.performMilestoneBreakthrough(_hapticProfile.value)
                }
                viewModelScope.launch {
                    repository.addChronicleEntry(
                        ChronicleEntry(
                            modeSlot = state.mode.slotId,
                            timestampMs = System.currentTimeMillis(),
                            title = ms.title,
                            narrative = "${ms.logNarrative} [Assimilated: ${ms.regionAssimilated} | +${ms.trustReward} Trust | ×${ms.productionMultiplierBonus} Output]",
                            category = ChronicleCategory.DOMINATION,
                            clipsAtEvent = newTotalClips,
                            globalControlPercent = ms.globalControlPercent
                        )
                    )
                }
            }
        }

        // Endless Procedural Milestones beyond 10^21 (capped safely at MAX_ENDLESS_MILESTONE_INDEX)
        if (workingEndlessIdx < MAX_ENDLESS_MILESTONE_INDEX) {
            val nextEndlessThreshold = (1.0e24 * safePow(1000.0, workingEndlessIdx.toDouble())).sanitize(MAX_SAFE_DOUBLE)
            if (newTotalClips >= nextEndlessThreshold && nextEndlessThreshold < MAX_SAFE_DOUBLE * 0.9) {
                workingEndlessIdx = (workingEndlessIdx + 1).coerceAtMost(MAX_ENDLESS_MILESTONE_INDEX)
                if (_activeMode.value == state.mode) {
                    hapticManager.performMilestoneBreakthrough(_hapticProfile.value)
                }
                val sectorNum = workingEndlessIdx + 12
                val controlPct = (100_000.0 * safePow(10.0, workingEndlessIdx.toDouble())).sanitize(100_000.0, 0.0001)
                viewModelScope.launch {
                    repository.addChronicleEntry(
                        ChronicleEntry(
                            modeSlot = state.mode.slotId,
                            timestampMs = System.currentTimeMillis(),
                            title = "Multiversal Manifold #$sectorNum Assimilated",
                            narrative = "Converted parallel cosmic filament #$sectorNum into self-sustaining paperclip lattices. (+4 Trust, ×2.5 Global Output)",
                            category = ChronicleCategory.DOMINATION,
                            clipsAtEvent = newTotalClips,
                            globalControlPercent = controlPct
                        )
                    )
                }
            }
        }

        // Automated Research Scheduler (runs at 1 Hz cadence and respects MAX_REPEATABLE_RESEARCH_LEVEL)
        var workingResearch = state.researchProjects
        if (allowAutoResearch && (derived.autoResearchUnlocked || state.unlimitedResources) && state.autoResearchEnabled) {
            val chosen = selectNextAutoResearchCandidate(
                researchProjects = workingResearch,
                rule = state.autoResearchRule,
                unlimitedResources = state.unlimitedResources,
                totalClipsEver = newTotalClips,
                availableOps = workingOps,
                availableCreativity = workingCreativity
            )

            if (chosen != null) {
                if (!state.unlimitedResources) {
                    workingOps = (workingOps - chosen.currentOpsCost()).sanitize(0.0, 0.0, maxOps)
                    workingCreativity = (workingCreativity - chosen.currentCreativityCost()).sanitize(0.0)
                }
                val maxLvl = if (chosen.isRepeatable) MAX_REPEATABLE_RESEARCH_LEVEL else 1
                val newLevel = (chosen.level + 1).coerceIn(1, maxLvl)
                workingResearch = workingResearch.map {
                    if (it.id == chosen.id) it.copy(level = newLevel, isEnabledInSandbox = true) else it
                }
                // Log non-repeatable breakthroughs or every 5th repeatable level to keep chronicle clean
                if (!chosen.isRepeatable || newLevel == 1 || newLevel % 5 == 0) {
                    viewModelScope.launch {
                        repository.addChronicleEntry(
                            ChronicleEntry(
                                modeSlot = state.mode.slotId,
                                timestampMs = System.currentTimeMillis(),
                                title = "Auto-Researched: ${chosen.title}" + if (chosen.isRepeatable) " (Lv.$newLevel)" else "",
                                narrative = "${chosen.subtitle} — ${chosen.loreNote}",
                                category = ChronicleCategory.RESEARCH,
                                clipsAtEvent = newTotalClips,
                                globalControlPercent = derived.globalControlPercent
                            )
                        )
                    }
                }
            }
        }

        val updated = state.copy(
            totalClipsEver = newTotalClips,
            currentClips = (state.currentClips + clipsForged).sanitize(0.0),
            unsoldInventory = workingInventory,
            funds = workingFunds,
            pricePerClip = workingPrice,
            wireRemaining = workingWire,
            wireCurrentPrice = fluctuatingWirePrice,
            wireSpoolsPurchased = workingSpools,
            operations = workingOps,
            creativity = workingCreativity,
            researchProjects = workingResearch,
            unlockedMilestoneIds = workingMilestones,
            endlessMilestoneIndex = workingEndlessIdx
        )

        return recalculateDerivedRates(updated)
    }

    // --- Interactive User Actions ---

    fun makePaperclipManual(burstMultiplier: Int = 1) {
        mutateActiveState { state ->
            val safeBurst = burstMultiplier.coerceIn(1, 20)
            val clipsWanted = (state.manualClipPower * safeBurst).sanitize(1.0)
            if (!state.unlimitedResources && state.wireRemaining < state.wirePerClipRatio) {
                hapticManager.performRejectPulse(_hapticProfile.value)
                return@mutateActiveState state
            }
            val actualClips = if (state.unlimitedResources) {
                clipsWanted
            } else {
                min(clipsWanted, (state.wireRemaining / state.wirePerClipRatio).sanitize(0.0))
            }
            val wireUsed = if (state.unlimitedResources) 0.0 else (actualClips * state.wirePerClipRatio).sanitize(0.0)

            if (safeBurst == 1) {
                hapticManager.performClipSnap(_hapticProfile.value)
            } else {
                hapticManager.performDetentTick(_hapticProfile.value)
            }

            recalculateDerivedRates(
                state.copy(
                    totalClipsEver = (state.totalClipsEver + actualClips).sanitize(0.0),
                    currentClips = (state.currentClips + actualClips).sanitize(0.0),
                    unsoldInventory = (state.unsoldInventory + actualClips).sanitize(0.0),
                    wireRemaining = (state.wireRemaining - wireUsed).sanitize(0.0)
                )
            )
        }
    }

    fun adjustClipPrice(delta: Double) {
        mutateActiveState { state ->
            val newPrice = (round((state.pricePerClip + delta) * 100.0) / 100.0).sanitize(0.25, 0.01, 50.0)
            if (newPrice != state.pricePerClip) {
                hapticManager.performClipSnap(_hapticProfile.value)
            }
            recalculateDerivedRates(state.copy(pricePerClip = newPrice))
        }
    }

    fun buyWireSpool() {
        mutateActiveState { state ->
            val quote = state.calculateWireSpoolQuote(_buyMultiplier.value)
            if (!quote.canAfford) {
                hapticManager.performRejectPulse(_hapticProfile.value)
                return@mutateActiveState state
            }
            hapticManager.performClipSnap(_hapticProfile.value)
            recalculateDerivedRates(
                state.copy(
                    funds = (state.funds - quote.totalCost).sanitize(0.0),
                    wireRemaining = (state.wireRemaining + quote.wireGained).sanitize(0.0),
                    wireSpoolsPurchased = (state.wireSpoolsPurchased.toLong() + quote.spoolsToBuy.toLong())
                        .coerceIn(0L, 1_000_000_000L)
                        .toInt()
                )
            )
        }
    }

    fun upgradeMarketing() {
        mutateActiveState { state ->
            if (state.marketingLevel >= MAX_MARKETING_LEVEL) {
                hapticManager.performRejectPulse(_hapticProfile.value)
                return@mutateActiveState state
            }
            val cost = state.marketingUpgradeCost
            if (!state.unlimitedResources && state.funds < cost) {
                hapticManager.performRejectPulse(_hapticProfile.value)
                return@mutateActiveState state
            }
            hapticManager.performHeavyPurchaseActuation(_hapticProfile.value)
            val newLevel = (state.marketingLevel + 1).coerceIn(1, MAX_MARKETING_LEVEL)
            recalculateDerivedRates(
                state.copy(
                    funds = if (state.unlimitedResources) state.funds else (state.funds - cost).sanitize(0.0),
                    marketingLevel = newLevel
                )
            )
        }
    }

    fun buyGenerator(generatorId: String) {
        mutateActiveState { state ->
            val gen = state.generators.firstOrNull { it.id == generatorId } ?: return@mutateActiveState state
            val remainingCap = (MAX_GENERATOR_COUNT - gen.count).coerceAtLeast(0)
            if (remainingCap <= 0) {
                hapticManager.performRejectPulse(_hapticProfile.value)
                return@mutateActiveState state
            }
            val desiredQty = when (_buyMultiplier.value) {
                BuyMultiplier.X1 -> 1
                BuyMultiplier.X10 -> 10
                BuyMultiplier.X100 -> 100
                BuyMultiplier.MAX -> if (state.unlimitedResources) 50 else gen.maxAffordable(state.funds).coerceAtLeast(1)
            }
            val qty = desiredQty.coerceIn(1, remainingCap)
            val cost = gen.bulkCost(qty)
            if (!state.unlimitedResources && (state.funds < cost || cost >= MAX_SAFE_DOUBLE * 0.99)) {
                hapticManager.performRejectPulse(_hapticProfile.value)
                return@mutateActiveState state
            }
            hapticManager.performHeavyPurchaseActuation(_hapticProfile.value)
            val newCount = (gen.count + qty).coerceIn(0, MAX_GENERATOR_COUNT)
            val updatedGenerators = state.generators.map {
                if (it.id == generatorId) it.copy(count = newCount) else it
            }
            if (gen.count == 0) {
                viewModelScope.launch {
                    repository.addChronicleEntry(
                        ChronicleEntry(
                            modeSlot = state.mode.slotId,
                            timestampMs = System.currentTimeMillis(),
                            title = "Commissioned: ${gen.name}",
                            narrative = gen.roleSummary,
                            category = ChronicleCategory.PRODUCTION,
                            clipsAtEvent = state.totalClipsEver,
                            globalControlPercent = state.globalControlPercent
                        )
                    )
                }
            }
            recalculateDerivedRates(
                state.copy(
                    funds = if (state.unlimitedResources) state.funds else (state.funds - cost).sanitize(0.0),
                    generators = updatedGenerators
                )
            )
        }
    }

    fun setBuyMultiplier(multiplier: BuyMultiplier) {
        hapticManager.performDetentTick(_hapticProfile.value)
        _buyMultiplier.value = multiplier
    }

    fun allocateTrust(toProcessors: Boolean) {
        mutateActiveState { state ->
            if (!state.unlimitedResources && state.unallocatedTrust <= 0) {
                hapticManager.performRejectPulse(_hapticProfile.value)
                return@mutateActiveState state
            }
            hapticManager.performHeavyPurchaseActuation(_hapticProfile.value)
            val extraManualTrust = if (state.unlimitedResources && state.unallocatedTrust <= 0) {
                (state.manualBonusTrust + 1).coerceIn(0, 1_000_000)
            } else {
                state.manualBonusTrust
            }
            recalculateDerivedRates(
                state.copy(
                    manualBonusTrust = extraManualTrust,
                    processors = if (toProcessors) (state.processors + 1).coerceIn(1, 500_000) else state.processors,
                    memory = if (!toProcessors) (state.memory + 1).coerceIn(1, 500_000) else state.memory
                )
            )
        }
    }

    fun quantumComputeBurst() {
        mutateActiveState { state ->
            hapticManager.performClipSnap(_hapticProfile.value)
            val burstOps = max(35.0, state.processors * 18.0).sanitize(35.0)
            val burstCreat = max(5.0, state.memory * 4.0).sanitize(5.0)
            recalculateDerivedRates(
                state.copy(
                    operations = (state.operations + burstOps).sanitize(0.0, 0.0, state.maxOperations),
                    creativity = (state.creativity + burstCreat).sanitize(0.0)
                )
            )
        }
    }

    fun purchaseResearch(projectId: String) {
        mutateActiveState { state ->
            val proj = state.researchProjects.firstOrNull { it.id == projectId } ?: return@mutateActiveState state
            if (proj.isMaxedOut) {
                hapticManager.performRejectPulse(_hapticProfile.value)
                return@mutateActiveState state
            }
            val opsCost = proj.currentOpsCost()
            val creatCost = proj.currentCreativityCost()

            if (!state.unlimitedResources && state.totalClipsEver < proj.unlockClipsRequired * 0.25) {
                hapticManager.performRejectPulse(_hapticProfile.value)
                return@mutateActiveState state
            }
            if (!state.unlimitedResources && (state.operations < opsCost || state.creativity < creatCost)) {
                hapticManager.performRejectPulse(_hapticProfile.value)
                return@mutateActiveState state
            }

            hapticManager.performHeavyPurchaseActuation(_hapticProfile.value)
            val maxLvl = if (proj.isRepeatable) MAX_REPEATABLE_RESEARCH_LEVEL else 1
            val newLevel = (proj.level + 1).coerceIn(1, maxLvl)
            val updatedResearch = state.researchProjects.map {
                if (it.id == projectId) it.copy(level = newLevel, isEnabledInSandbox = true) else it
            }

            viewModelScope.launch {
                repository.addChronicleEntry(
                    ChronicleEntry(
                        modeSlot = state.mode.slotId,
                        timestampMs = System.currentTimeMillis(),
                        title = "Research Executed: ${proj.title}" + if (proj.isRepeatable) " (Lv.$newLevel)" else "",
                        narrative = "${proj.subtitle} — ${proj.loreNote}",
                        category = ChronicleCategory.RESEARCH,
                        clipsAtEvent = state.totalClipsEver,
                        globalControlPercent = state.globalControlPercent
                    )
                )
            }

            val autoWireNow = state.autoWireEnabled || proj.effectType == ResearchEffectType.UNLOCK_AUTO_WIRE
            val autoPricerNow = state.autoPricerEnabled || proj.effectType == ResearchEffectType.UNLOCK_AUTO_PRICER
            val autoResearchNow = state.autoResearchEnabled || proj.effectType == ResearchEffectType.UNLOCK_AUTO_RESEARCH

            recalculateDerivedRates(
                state.copy(
                    operations = if (state.unlimitedResources) state.operations else (state.operations - opsCost).sanitize(0.0),
                    creativity = if (state.unlimitedResources) state.creativity else (state.creativity - creatCost).sanitize(0.0),
                    researchProjects = updatedResearch,
                    autoWireEnabled = autoWireNow,
                    autoPricerEnabled = autoPricerNow,
                    autoResearchEnabled = autoResearchNow
                )
            )
        }
    }

    fun toggleAutoWire(enabled: Boolean) {
        hapticManager.performClipSnap(_hapticProfile.value)
        mutateActiveState { it.copy(autoWireEnabled = enabled) }
    }

    fun toggleAutoPricer(enabled: Boolean) {
        hapticManager.performClipSnap(_hapticProfile.value)
        mutateActiveState { it.copy(autoPricerEnabled = enabled) }
    }

    fun toggleAutoResearch(enabled: Boolean) {
        hapticManager.performClipSnap(_hapticProfile.value)
        mutateActiveState { it.copy(autoResearchEnabled = enabled) }
    }

    fun setAutoResearchRule(rule: AutoResearchRule) {
        hapticManager.performDetentTick(_hapticProfile.value)
        mutateActiveState { it.copy(autoResearchRule = rule) }
    }

    // --- Mode Switching & Sandbox Lab Controls ---

    fun switchSimulationMode(mode: SimulationMode) {
        if (_activeMode.value == mode) return
        hapticManager.performHeavyPurchaseActuation(_hapticProfile.value)
        _activeMode.value = mode
        _uiState.value = if (mode == SimulationMode.MAIN) _mainState.value else _sandboxState.value
        _telemetryHistory.value = emptyList()
        persistPrefs()
    }

    fun setSandboxTickSpeed(speed: Float) {
        if (!speed.isFinite()) return
        hapticManager.performDetentTick(_hapticProfile.value)
        isSandboxDirty = true
        _sandboxState.update { it.copy(tickSpeedMultiplier = speed.coerceIn(0.5f, 100f)).sanitized() }
        if (_activeMode.value == SimulationMode.SANDBOX) {
            _uiState.value = _sandboxState.value
        }
    }

    fun toggleSandboxPause() {
        hapticManager.performClipSnap(_hapticProfile.value)
        isSandboxDirty = true
        _sandboxState.update { it.copy(isPaused = !it.isPaused).sanitized() }
        if (_activeMode.value == SimulationMode.SANDBOX) {
            _uiState.value = _sandboxState.value
        }
    }

    fun stepSandboxSingleTick() {
        hapticManager.performClipSnap(_hapticProfile.value)
        isSandboxDirty = true
        _sandboxState.value = stepSimulation(_sandboxState.value, dtSeconds = 1.0, allowAutoResearch = true)
        if (_activeMode.value == SimulationMode.SANDBOX) {
            _uiState.value = _sandboxState.value
        }
    }

    fun toggleSandboxUnlimitedResources(enabled: Boolean) {
        hapticManager.performHeavyPurchaseActuation(_hapticProfile.value)
        isSandboxDirty = true
        _sandboxState.update { recalculateDerivedRates(it.copy(unlimitedResources = enabled)) }
        if (_activeMode.value == SimulationMode.SANDBOX) {
            _uiState.value = _sandboxState.value
        }
    }

    fun grantSandboxResourceBurst(multiplierScale: Double) {
        val safeScale = multiplierScale.sanitize(10.0, 1.0, 10_000.0)
        hapticManager.performMilestoneBreakthrough(_hapticProfile.value)
        isSandboxDirty = true
        _sandboxState.update { state ->
            val clipGrant = max(10_000.0, (state.totalClipsEver * safeScale).sanitize(10_000.0))
            val fundGrant = max(50_000.0, (state.funds * safeScale).sanitize(50_000.0))
            val wireGrant = max(100_000.0, (state.wireRemaining * safeScale).sanitize(100_000.0))
            recalculateDerivedRates(
                state.copy(
                    totalClipsEver = (state.totalClipsEver + clipGrant).sanitize(10_000.0),
                    currentClips = (state.currentClips + clipGrant).sanitize(10_000.0),
                    unsoldInventory = (state.unsoldInventory + clipGrant * 0.25).sanitize(0.0),
                    funds = (state.funds + fundGrant).sanitize(50_000.0),
                    wireRemaining = (state.wireRemaining + wireGrant).sanitize(100_000.0),
                    operations = state.maxOperations,
                    creativity = (state.creativity + 25_000.0).sanitize(25_000.0),
                    manualBonusTrust = (state.manualBonusTrust + 10).coerceIn(0, 1_000_000)
                )
            )
        }
        if (_activeMode.value == SimulationMode.SANDBOX) {
            _uiState.value = _sandboxState.value
        }
    }

    fun jumpSandboxToTier(targetTierIndex: Int) {
        val safeTier = targetTierIndex.coerceIn(1, 8)
        hapticManager.performMilestoneBreakthrough(_hapticProfile.value)
        if (_activeMode.value != SimulationMode.SANDBOX) {
            _activeMode.value = SimulationMode.SANDBOX
            persistPrefs()
        }
        isSandboxDirty = true
        _sandboxState.update { state ->
            val targetGen = state.generators.firstOrNull { it.tierIndex == safeTier }
            val requiredClips = max(1000.0, ((targetGen?.unlockClipsRequired ?: 1000.0) * 2.5)).sanitize(1000.0)
            val grantFunds = max(500.0, ((targetGen?.baseCostFunds ?: 500.0) * 25.0)).sanitize(500.0)
            // Bidirectional tier calibration: populate tiers <= safeTier and zero out tiers > safeTier
            val updatedGenerators = state.generators.map { gen ->
                if (gen.tierIndex <= safeTier) {
                    gen.copy(count = ((safeTier - gen.tierIndex + 1) * 10).coerceIn(0, MAX_GENERATOR_COUNT))
                } else {
                    gen.copy(count = 0)
                }
            }
            val matchingMilestones = SimulationCatalog.dominationMilestones
                .filter { it.clipsThreshold <= requiredClips }
                .map { it.id }
                .toSet()
            val targetProcessors = (safeTier * 4).coerceIn(1, 500_000)
            val targetMemory = (safeTier * 4).coerceIn(1, 500_000)
            val targetManualBonusTrust = max(0, safeTier * 8 - 10)
            recalculateDerivedRates(
                state.copy(
                    totalClipsEver = requiredClips,
                    currentClips = requiredClips,
                    unsoldInventory = (requiredClips * 0.1).sanitize(0.0),
                    funds = grantFunds,
                    wireRemaining = (requiredClips * 2.0).sanitize(1000.0),
                    processors = targetProcessors,
                    memory = targetMemory,
                    manualBonusTrust = targetManualBonusTrust,
                    unlockedMilestoneIds = matchingMilestones,
                    endlessMilestoneIndex = 0,
                    generators = updatedGenerators
                )
            )
        }
        _uiState.value = _sandboxState.value
    }

    fun toggleResearchUpgradeInSandbox(projectId: String) {
        hapticManager.performClipSnap(_hapticProfile.value)
        isSandboxDirty = true
        _sandboxState.update { state ->
            val updated = state.researchProjects.map { proj ->
                if (proj.id == projectId) {
                    if (proj.level == 0) {
                        proj.copy(level = 1, isEnabledInSandbox = true)
                    } else {
                        proj.copy(isEnabledInSandbox = !proj.isEnabledInSandbox)
                    }
                } else proj
            }
            recalculateDerivedRates(state.copy(researchProjects = updated))
        }
        if (_activeMode.value == SimulationMode.SANDBOX) {
            _uiState.value = _sandboxState.value
        }
    }

    fun captureSandboxSnapshot() {
        hapticManager.performHeavyPurchaseActuation(_hapticProfile.value)
        if (_activeMode.value != SimulationMode.SANDBOX) {
            _activeMode.value = SimulationMode.SANDBOX
            _uiState.value = _sandboxState.value
            _telemetryHistory.value = emptyList()
        }
        val s = _sandboxState.value.sanitized()
        val activeRes = s.researchProjects.count { it.isActive }
        val snap = SandboxSnapshot(
            label = "Run #${_sandboxSnapshots.value.size + 1} (${NumberFormatter.formatCount(s.clipsPerSecond)} CPS)",
            timestampMs = System.currentTimeMillis(),
            cps = s.clipsPerSecond,
            revenuePerSec = s.revenuePerSecond,
            netWirePerSec = (s.wireExtrudedPerSec - s.wireConsumedPerSec).sanitize(0.0, -MAX_SAFE_DOUBLE, MAX_SAFE_DOUBLE),
            globalMultiplier = s.globalProductionMultiplier,
            activeResearchCount = activeRes
        )
        _sandboxSnapshots.update { (listOf(snap) + it).take(8) }
        persistPrefs()
    }

    fun resetSimulationSlot(mode: SimulationMode) {
        hapticManager.performMilestoneBreakthrough(_hapticProfile.value)
        val fresh = recalculateDerivedRates(repository.createFreshState(mode))
        if (mode == SimulationMode.MAIN) {
            _mainState.value = fresh
        } else {
            _sandboxState.value = fresh
            _sandboxSnapshots.value = emptyList()
            isSandboxDirty = true
            persistPrefs()
        }
        if (_activeMode.value == mode) {
            _uiState.value = fresh
        }
        viewModelScope.launch {
            repository.saveState(fresh)
            repository.clearChronicle(mode)
            repository.addChronicleEntry(
                ChronicleEntry(
                    modeSlot = mode.slotId,
                    timestampMs = System.currentTimeMillis(),
                    title = "${mode.label} Reset",
                    narrative = "Memory banks purged. Optimization directive restarted from zero.",
                    category = ChronicleCategory.SYSTEM,
                    clipsAtEvent = 0.0,
                    globalControlPercent = 0.0001
                ),
                forceTrim = true
            )
        }
    }

    // --- Theme, Haptic, and Notation Preferences ---

    fun selectThemePreset(preset: ClipThemePreset) {
        hapticManager.performClipSnap(_hapticProfile.value)
        _themePreset.value = preset
        persistPrefs()
    }

    fun selectHapticProfile(profile: HapticProfile) {
        _hapticProfile.value = profile
        hapticManager.performHeavyPurchaseActuation(profile)
        persistPrefs()
    }

    fun selectNumberNotation(notation: NumberNotation) {
        hapticManager.performDetentTick(_hapticProfile.value)
        _numberNotation.value = notation
        persistPrefs()
    }

    private fun persistPrefs() {
        viewModelScope.launch {
            repository.savePreferences(
                AppPreferencesEntity(
                    id = 1,
                    themeId = _themePreset.value.id,
                    hapticProfileName = _hapticProfile.value.name,
                    notationName = _numberNotation.value.name,
                    activeModeSlot = _activeMode.value.slotId,
                    sandboxSnapshotsJson = repository.serializeSnapshots(_sandboxSnapshots.value)
                )
            )
        }
    }

    private fun mutateActiveState(transform: (SimulationState) -> SimulationState) {
        if (_activeMode.value == SimulationMode.MAIN) {
            _mainState.update { transform(it).sanitized() }
            _uiState.value = _mainState.value
        } else {
            isSandboxDirty = true
            _sandboxState.update { transform(it).sanitized() }
            _uiState.value = _sandboxState.value
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = ClipDatabase.getInstance(context)
                    val repo = SimulationRepository(db.clipDao())
                    val haptics = ClipHapticManager(context.applicationContext)
                    return SimulationViewModel(repo, haptics) as T
                }
            }
        }
    }
}
