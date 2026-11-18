package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.SimulationCatalog
import com.example.data.local.ClipDatabase
import com.example.data.local.SimulationProfileEntity
import com.example.data.local.SimulationRepository
import com.example.model.SandboxSnapshot
import com.example.model.SimulationMode
import com.example.model.SimulationState
import com.example.ui.theme.ClipThemePreset
import com.example.util.ClipHapticManager
import com.example.util.HapticProfile
import com.example.util.NumberFormatter
import com.example.util.NumberNotation
import com.example.util.safeProgressRatio
import com.example.viewmodel.SimulationViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name and verify endless simulation catalog and haptics`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ClipMind AI", appName)

        // Verify endless generator tiers and repeatable research
        val generators = SimulationCatalog.defaultGenerators()
        assertEquals(8, generators.size)
        val research = SimulationCatalog.defaultResearchProjects()
        assertTrue(research.any { it.isRepeatable && it.tier == 5 })

        // Verify NumberFormatter handles endless scaling
        assertEquals("1.50M", NumberFormatter.formatCount(1_500_000.0, NumberNotation.STANDARD))
        assertEquals("1.00e24", NumberFormatter.formatCount(1.0e24, NumberNotation.SCIENTIFIC))

        // Verify all 8 eye-strain-reducing themes are available
        assertEquals(8, ClipThemePreset.entries.size)

        // Verify HapticEngine executes safely across profiles
        val haptics = ClipHapticManager(context)
        haptics.performClipSnap(HapticProfile.FLAGSHIP_CRISP)
        haptics.performDetentTick(HapticProfile.SUBTLE_TICKS)
        haptics.performHeavyPurchaseActuation(HapticProfile.FLAGSHIP_CRISP)
        haptics.performMilestoneBreakthrough(HapticProfile.FLAGSHIP_CRISP)
    }

    @Test
    fun `self-healing repository recovers from NaN and Infinity states without crashing`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ClipDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = SimulationRepository(db.clipDao())

        // 1. Verify saving a state corrupted with NaN and POSITIVE_INFINITY never throws SQLiteConstraintException
        val corruptedState = SimulationState(
            mode = SimulationMode.MAIN,
            totalClipsEver = Double.POSITIVE_INFINITY,
            currentClips = Double.NaN,
            unsoldInventory = Double.POSITIVE_INFINITY,
            funds = Double.NaN,
            wireRemaining = Double.NEGATIVE_INFINITY,
            operations = Double.NaN,
            creativity = Double.POSITIVE_INFINITY,
            marketingLevel = 999_999,
            endlessMilestoneIndex = 999_999,
            tickSpeedMultiplier = Float.NaN,
            generators = SimulationCatalog.defaultGenerators().map { it.copy(count = 999_999) },
            researchProjects = SimulationCatalog.defaultResearchProjects().map { it.copy(level = 999_999) }
        )
        repo.saveState(corruptedState)

        // 2. Verify loading back heals all values to finite numbers
        val healed = repo.loadStateOrCreateDefault(SimulationMode.MAIN)
        assertTrue(healed.totalClipsEver.isFinite())
        assertTrue(healed.currentClips.isFinite())
        assertTrue(healed.funds.isFinite())
        assertTrue(healed.wireRemaining.isFinite())
        assertTrue(healed.operations.isFinite())
        assertTrue(healed.tickSpeedMultiplier.isFinite())

        // 3. Verify safeProgressRatio never returns NaN even for Infinity/Infinity or NaN/0
        val ratioInf = safeProgressRatio(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        assertFalse(ratioInf.isNaN())
        val ratioNaN = safeProgressRatio(Double.NaN, 0.0)
        assertFalse(ratioNaN.isNaN())

        // 4. Verify directly inserted Infinity row in Room is healed on load
        db.clipDao().upsertProfile(
            SimulationProfileEntity(
                slotId = "SANDBOX",
                totalClipsEver = Double.POSITIVE_INFINITY,
                currentClips = Double.POSITIVE_INFINITY,
                unsoldInventory = Double.POSITIVE_INFINITY,
                funds = Double.POSITIVE_INFINITY,
                pricePerClip = 0.25,
                marketingLevel = 5000,
                wireRemaining = Double.POSITIVE_INFINITY,
                wireCurrentPrice = 16.0,
                wireSpoolsPurchased = Int.MAX_VALUE,
                trust = 50,
                processors = 10,
                memory = 10,
                operations = Double.POSITIVE_INFINITY,
                creativity = Double.POSITIVE_INFINITY,
                autoWireUnlocked = true,
                autoWireEnabled = true,
                autoPricerUnlocked = true,
                autoPricerEnabled = true,
                autoResearchUnlocked = true,
                autoResearchEnabled = true,
                autoResearchRuleName = "CHEAPEST_FIRST",
                generatorsJson = """{"auto_clipper": 999999}""",
                researchJson = """[{"id": "res_endless_harmonic_folding", "level": 99999, "enabled": true}]""",
                unlockedMilestonesJson = "[]",
                endlessMilestoneIndex = 9999,
                tickSpeedMultiplier = 100f,
                isPaused = false,
                unlimitedResources = true,
                lastSavedTimestampMs = System.currentTimeMillis()
            )
        )
        val healedSandbox = repo.loadStateOrCreateDefault(SimulationMode.SANDBOX)
        assertTrue(healedSandbox.totalClipsEver.isFinite())
        assertTrue(healedSandbox.funds.isFinite())
        assertTrue(healedSandbox.generators.all { it.count <= 2500 })
        assertTrue(healedSandbox.researchProjects.all { it.level <= 250 })

        // 5. Verify SandboxSnapshot serialization round-trip
        val snaps = listOf(
            SandboxSnapshot("Run #1", 1000L, 450.0, 112.5, 50.0, 6.0, 4)
        )
        val json = repo.serializeSnapshots(snaps)
        val restored = repo.deserializeSnapshots(json)
        assertEquals(1, restored.size)
        assertEquals("Run #1", restored.first().label)
        assertEquals(450.0, restored.first().cps, 0.01)

        db.close()
    }

    @Test
    fun `research secondary bonuses, reversible sandbox trust, and bidirectional tier jump work accurately`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ClipDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = SimulationRepository(db.clipDao())
        val vm = SimulationViewModel(repo, ClipHapticManager(context))

        val baseSandbox = repo.createFreshState(SimulationMode.SANDBOX)
        val baseRecalc = vm.recalculateDerivedRates(baseSandbox)
        assertEquals(10, baseRecalc.trust)

        // Enable "res_von_neumann_probes" (×6.0 production + 8 Trust) and "res_global_monopoly" (×3.5 demand + ×2.0 clip output)
        val withUpgrades = baseRecalc.copy(
            researchProjects = baseRecalc.researchProjects.map { proj ->
                when (proj.id) {
                    "res_von_neumann_probes", "res_global_monopoly", "res_synthetic_trust" ->
                        proj.copy(level = 1, isEnabledInSandbox = true)
                    else -> proj
                }
            }
        )
        val activeRecalc = vm.recalculateDerivedRates(withUpgrades)
        // Base 10 + 8 (von neumann) + 4 (synthetic trust) = 22 Trust
        assertEquals(22, activeRecalc.trust)
        // Production multiplier: 6.0 (von neumann) * 2.0 (global monopoly) * 1.75 (synthetic trust) = 21.0
        assertEquals(21.0, activeRecalc.globalProductionMultiplier, 0.01)

        // Now toggle those upgrades OFF in Sandbox and verify Trust & multipliers revert cleanly to base!
        val toggledOff = activeRecalc.copy(
            researchProjects = activeRecalc.researchProjects.map { proj ->
                proj.copy(isEnabledInSandbox = false)
            }
        )
        val revertedRecalc = vm.recalculateDerivedRates(toggledOff)
        assertEquals(10, revertedRecalc.trust)
        assertEquals(1.0, revertedRecalc.globalProductionMultiplier, 0.01)

        // Verify unlimitedResources keeps operations capped at maxOperations
        val stepped = vm.stepSimulation(baseRecalc.copy(unlimitedResources = true, memory = 5), dtSeconds = 1.0)
        assertEquals(5000.0, stepped.maxOperations, 0.01)
        assertEquals(5000.0, stepped.operations, 0.01)

        db.close()
    }
}
