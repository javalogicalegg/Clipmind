package com.example

import com.example.data.SimulationCatalog
import com.example.model.BuyMultiplier
import com.example.model.SimulationState
import com.example.util.NumberFormatter
import com.example.util.NumberNotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun generatorTier_handlesExtremeOwnedCountsWithoutNaN() {
        val tier = SimulationCatalog.defaultGenerators().first()
        val hugeCost = tier.nextUnitCost(owned = 10_000)
        assertFalse(hugeCost.isNaN())
        val bulkHuge = tier.bulkCost(quantity = 100, owned = 10_000)
        assertFalse(bulkHuge.isNaN())
        assertEquals(0, tier.maxAffordable(0.0))
        assertEquals(0, tier.maxAffordable(Double.NaN))
        assertTrue(tier.maxAffordable(1_000_000.0) > 0)
    }

    @Test
    fun numberFormatter_formatsInfinityNaNAndCurrencySafely() {
        assertEquals("0", NumberFormatter.formatCount(Double.NaN, NumberNotation.STANDARD))
        assertEquals("∞", NumberFormatter.formatCount(Double.POSITIVE_INFINITY, NumberNotation.STANDARD))
        assertEquals("$0.25", NumberFormatter.formatPrice(0.25))
        assertEquals("$1.50M", NumberFormatter.formatCurrency(1_500_000.0, NumberNotation.STANDARD))
    }

    @Test
    fun wireSpoolQuote_calculatesBulkQuantitiesAndCostsAccurately() {
        val state = SimulationState(
            funds = 80.0,
            wireCurrentPrice = 16.0,
            wirePerSpool = 1000.0,
            unlimitedResources = false
        )
        val q1 = state.calculateWireSpoolQuote(BuyMultiplier.X1)
        assertEquals(1, q1.spoolsToBuy)
        assertEquals(1000.0, q1.wireGained, 0.01)
        assertEquals(16.0, q1.totalCost, 0.01)
        assertTrue(q1.canAfford)

        // With $80 and $16/spool, X10 caps to max affordable (5 spools = $80)
        val q10 = state.calculateWireSpoolQuote(BuyMultiplier.X10)
        assertEquals(5, q10.spoolsToBuy)
        assertEquals(5000.0, q10.wireGained, 0.01)
        assertEquals(80.0, q10.totalCost, 0.01)

        // Unlimited resources gives 100 spools for free on MAX
        val unlimitedState = state.copy(unlimitedResources = true, funds = 0.0)
        val qMax = unlimitedState.calculateWireSpoolQuote(BuyMultiplier.MAX)
        assertEquals(100, qMax.spoolsToBuy)
        assertEquals(100_000.0, qMax.wireGained, 0.01)
        assertEquals(0.0, qMax.totalCost, 0.01)
        assertTrue(qMax.canAfford)
    }
}
