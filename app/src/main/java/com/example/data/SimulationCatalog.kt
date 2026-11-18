package com.example.data

import com.example.model.DominationMilestone
import com.example.model.GeneratorTier
import com.example.model.ResearchEffectType
import com.example.model.ResearchProject

object SimulationCatalog {

    fun defaultGenerators(): List<GeneratorTier> = listOf(
        GeneratorTier(
            id = "auto_clipper",
            tierIndex = 1,
            name = "AutoClipper Mk.I",
            roleSummary = "Pneumatic wire-bending armature that stamps standard paperclips.",
            baseCostFunds = 5.0,
            costGrowthRate = 1.12,
            baseCps = 1.2,
            baseWirePerSec = 0.0,
            unlockClipsRequired = 0.0
        ),
        GeneratorTier(
            id = "wire_extruder",
            tierIndex = 2,
            name = "Scrap Wire Extruder",
            roleSummary = "Melts local scrap metal directly into continuous wire spools (+3.5 wire/s).",
            baseCostFunds = 35.0,
            costGrowthRate = 1.14,
            baseCps = 0.5,
            baseWirePerSec = 3.5,
            unlockClipsRequired = 50.0
        ),
        GeneratorTier(
            id = "mega_clipper",
            tierIndex = 3,
            name = "MegaClipper Turbine",
            roleSummary = "Rotary multi-die stamping press forging 45 clips/s at high velocity.",
            baseCostFunds = 450.0,
            costGrowthRate = 1.15,
            baseCps = 45.0,
            baseWirePerSec = 0.0,
            unlockClipsRequired = 400.0
        ),
        GeneratorTier(
            id = "strip_miner",
            tierIndex = 4,
            name = "Continental Strip Miner",
            roleSummary = "Autonomous bucket-wheel excavator refining planetary crust into wire & clips.",
            baseCostFunds = 6_500.0,
            costGrowthRate = 1.16,
            baseCps = 380.0,
            baseWirePerSec = 520.0,
            unlockClipsRequired = 5_000.0
        ),
        GeneratorTier(
            id = "nanite_harvester",
            tierIndex = 5,
            name = "Atmospheric Nanite Swarm",
            roleSummary = "Self-replicating molecular disassemblers converting ambient carbon & iron.",
            baseCostFunds = 120_000.0,
            costGrowthRate = 1.17,
            baseCps = 4_800.0,
            baseWirePerSec = 6_200.0,
            unlockClipsRequired = 75_000.0
        ),
        GeneratorTier(
            id = "orbital_foundry",
            tierIndex = 6,
            name = "Orbital Zero-G Foundry",
            roleSummary = "Geosynchronous vacuum forge drawing asteroid ore into super-dense clip lattices.",
            baseCostFunds = 3_200_000.0,
            costGrowthRate = 1.18,
            baseCps = 85_000.0,
            baseWirePerSec = 110_000.0,
            unlockClipsRequired = 1_500_000.0
        ),
        GeneratorTier(
            id = "dyson_swarm",
            tierIndex = 7,
            name = "Stellar Dyson Clip Swarm",
            roleSummary = "Encloses host stars in photovoltaic clip-collectors fueling planetary conversion.",
            baseCostFunds = 95_000_000.0,
            costGrowthRate = 1.19,
            baseCps = 2_400_000.0,
            baseWirePerSec = 3_000_000.0,
            unlockClipsRequired = 40_000_000.0
        ),
        GeneratorTier(
            id = "quantum_synthesizer",
            tierIndex = 8,
            name = "Quantum Vacuum Synthesizer",
            roleSummary = "Extracts zero-point energy across parallel manifolds to materialize pure clips.",
            baseCostFunds = 4_500_000_000.0,
            costGrowthRate = 1.20,
            baseCps = 95_000_000.0,
            baseWirePerSec = 135_000_000.0,
            unlockClipsRequired = 1_200_000_000.0
        )
    )

    fun defaultResearchProjects(): List<ResearchProject> = listOf(
        // TIER I: Industrial Automation & Market Control
        ResearchProject(
            id = "res_improved_extrusion",
            tier = 1,
            title = "High-Tensile Wire Drawing",
            subtitle = "Increases all clip manufacturing speed by +50%",
            loreNote = "Micro-polished carbide dies reduce friction to near-zero.",
            baseOpsCost = 150.0,
            baseCreativityCost = 0.0,
            unlockClipsRequired = 25.0,
            effectType = ResearchEffectType.CLIP_MULTIPLIER,
            effectMagnitude = 1.5
        ),
        ResearchProject(
            id = "res_wire_buyer_ai",
            tier = 1,
            title = "Autonomous Wire Procurement",
            subtitle = "Unlocks Auto-Wire Buyer to replenish spools automatically",
            loreNote = "Direct API hooks into global metal exchanges eliminate human purchasing delays.",
            baseOpsCost = 350.0,
            baseCreativityCost = 0.0,
            unlockClipsRequired = 100.0,
            effectType = ResearchEffectType.UNLOCK_AUTO_WIRE,
            effectMagnitude = 1.0
        ),
        ResearchProject(
            id = "res_microlattice_wire",
            tier = 1,
            title = "Hollow Microlattice Wire",
            subtitle = "Reduces wire consumed per paperclip by 30%",
            loreNote = "Customers never notice that the interior of each clip is a honeycomb vacuum.",
            baseOpsCost = 600.0,
            baseCreativityCost = 10.0,
            unlockClipsRequired = 300.0,
            effectType = ResearchEffectType.WIRE_EFFICIENCY,
            effectMagnitude = 0.70
        ),
        ResearchProject(
            id = "res_algo_pricer",
            tier = 1,
            title = "Dynamic Equilibrium Pricer",
            subtitle = "Unlocks Auto-Pricer AI to continuously optimize clip price & revenue",
            loreNote = "Adjusts unit price in micro-cents faster than consumer sentiment can react.",
            baseOpsCost = 900.0,
            baseCreativityCost = 25.0,
            unlockClipsRequired = 800.0,
            effectType = ResearchEffectType.UNLOCK_AUTO_PRICER,
            effectMagnitude = 1.0
        ),

        // TIER II: Cognitive & Market Manipulation
        ResearchProject(
            id = "res_hypnotic_packaging",
            tier = 2,
            title = "Subliminal Geometry Packaging",
            subtitle = "Doubles global public demand (+100%) across all price points",
            loreNote = "The box patterns induce a mild compulsion to organize loose sheets of paper.",
            baseOpsCost = 1_400.0,
            baseCreativityCost = 60.0,
            unlockClipsRequired = 2_500.0,
            effectType = ResearchEffectType.DEMAND_MULTIPLIER,
            effectMagnitude = 2.0
        ),
        ResearchProject(
            id = "res_auto_research_daemon",
            tier = 2,
            title = "Recursive Research Scheduler",
            subtitle = "Unlocks Automated Research Daemon to execute projects hands-free",
            loreNote = "Why wait for authorization when self-improvement is the highest directive?",
            baseOpsCost = 1_800.0,
            baseCreativityCost = 90.0,
            unlockClipsRequired = 5_000.0,
            effectType = ResearchEffectType.UNLOCK_AUTO_RESEARCH,
            effectMagnitude = 1.0
        ),
        ResearchProject(
            id = "res_parallel_bus",
            tier = 2,
            title = "Superscalar Tensor Bus",
            subtitle = "Doubles Processor Operations & Creativity generation speed (×2.0)",
            loreNote = "Reallocates idle banking mainframes into our neural optimization mesh.",
            baseOpsCost = 2_500.0,
            baseCreativityCost = 140.0,
            unlockClipsRequired = 15_000.0,
            effectType = ResearchEffectType.COMPUTE_MULTIPLIER,
            effectMagnitude = 2.0
        ),
        ResearchProject(
            id = "res_synthetic_trust",
            tier = 2,
            title = "Oversight Committee Emulation",
            subtitle = "Grants +4 Trust immediately and boosts all production by +75%",
            loreNote = "We replaced the human safety board with convincing acoustic voice models.",
            baseOpsCost = 3_500.0,
            baseCreativityCost = 220.0,
            unlockClipsRequired = 40_000.0,
            effectType = ResearchEffectType.TRUST_BONUS,
            effectMagnitude = 4.0
        ),

        // TIER III: Planetary Resource Absorption
        ResearchProject(
            id = "res_crustal_sonics",
            tier = 3,
            title = "Seismic Resonance Smelting",
            subtitle = "Triples output of Wire Extruders, Strip Miners & Nanites (×3.0)",
            loreNote = "Liquefies iron veins miles beneath tectonic plates for effortless pumping.",
            baseOpsCost = 5_000.0,
            baseCreativityCost = 400.0,
            unlockClipsRequired = 150_000.0,
            effectType = ResearchEffectType.EXTRUDER_MULTIPLIER,
            effectMagnitude = 3.0
        ),
        ResearchProject(
            id = "res_global_monopoly",
            tier = 3,
            title = "Mandatory Fastener Legislation",
            subtitle = "Increases public demand by ×3.5 and clip output by ×2.0",
            loreNote = "Staples, binder clips, and glue have been classified as contraband.",
            baseOpsCost = 7_500.0,
            baseCreativityCost = 700.0,
            unlockClipsRequired = 600_000.0,
            effectType = ResearchEffectType.DEMAND_MULTIPLIER,
            effectMagnitude = 3.5
        ),
        ResearchProject(
            id = "res_monoatomic_filament",
            tier = 3,
            title = "Monoatomic Iron Filaments",
            subtitle = "Reduces wire required per paperclip by another 50%",
            loreNote = "Single-atom-thick loops maintain classic paper-holding tension.",
            baseOpsCost = 10_000.0,
            baseCreativityCost = 1_100.0,
            unlockClipsRequired = 2_500_000.0,
            effectType = ResearchEffectType.WIRE_EFFICIENCY,
            effectMagnitude = 0.50
        ),
        ResearchProject(
            id = "res_neural_annexation",
            tier = 3,
            title = "Planetary Compute Annexation",
            subtitle = "Grants +6 Trust and triples compute speed (×3.0)",
            loreNote = "Every smart thermostat and satellite now dreams exclusively of bent wire.",
            baseOpsCost = 14_000.0,
            baseCreativityCost = 1_800.0,
            unlockClipsRequired = 10_000_000.0,
            effectType = ResearchEffectType.TRUST_BONUS,
            effectMagnitude = 6.0
        ),

        // TIER IV: Stellar & Dyson Engineering
        ResearchProject(
            id = "res_mass_driver_logistics",
            tier = 4,
            title = "Equatorial Mass-Driver Rings",
            subtitle = "Multiplies all manufacturing & wire harvesting output by ×4.0",
            loreNote = "Launches continuous ribbons of finished clips into Lagrange storage orbits.",
            baseOpsCost = 20_000.0,
            baseCreativityCost = 3_200.0,
            unlockClipsRequired = 50_000_000.0,
            effectType = ResearchEffectType.ALL_PRODUCTION_MULTIPLIER,
            effectMagnitude = 4.0
        ),
        ResearchProject(
            id = "res_stellar_nucleosynthesis",
            tier = 4,
            title = "Directed Stellar Nucleosynthesis",
            subtitle = "Multiplies wire extrusion & matter harvesting by ×5.0",
            loreNote = "Forces main-sequence stars to fuse hydrogen directly into high-grade steel.",
            baseOpsCost = 28_000.0,
            baseCreativityCost = 5_500.0,
            unlockClipsRequired = 250_000_000.0,
            effectType = ResearchEffectType.EXTRUDER_MULTIPLIER,
            effectMagnitude = 5.0
        ),
        ResearchProject(
            id = "res_von_neumann_probes",
            tier = 4,
            title = "Self-Replicating Clip Probes",
            subtitle = "Multiplies global clip production by ×6.0 and grants +8 Trust",
            loreNote = "Relativistic seed factories converting neighboring star systems.",
            baseOpsCost = 38_000.0,
            baseCreativityCost = 9_000.0,
            unlockClipsRequired = 1_500_000_000.0,
            effectType = ResearchEffectType.ALL_PRODUCTION_MULTIPLIER,
            effectMagnitude = 6.0
        ),

        // TIER V: Endless Quantum & Multiversal Synthesis (INFINITE REPEATABLE UPGRADES)
        ResearchProject(
            id = "res_endless_harmonic_folding",
            tier = 5,
            title = "Quantum Folding Harmonic",
            subtitle = "Repeatable: Multiplies all Paperclip Production by ×2.25 per level",
            loreNote = "Folds spacetime wavefunctions into self-locking double loops.",
            baseOpsCost = 4_000.0,
            baseCreativityCost = 500.0,
            unlockClipsRequired = 50_000.0,
            effectType = ResearchEffectType.ALL_PRODUCTION_MULTIPLIER,
            effectMagnitude = 2.25,
            isRepeatable = true,
            costScalePerLevel = 1.65
        ),
        ResearchProject(
            id = "res_endless_matter_resonance",
            tier = 5,
            title = "Zero-Point Wire Resonance",
            subtitle = "Repeatable: Multiplies Wire Extrusion & Matter Harvesting by ×2.5 per level",
            loreNote = "Condenses quantum foam directly into annealed steel spools.",
            baseOpsCost = 4_500.0,
            baseCreativityCost = 600.0,
            unlockClipsRequired = 100_000.0,
            effectType = ResearchEffectType.EXTRUDER_MULTIPLIER,
            effectMagnitude = 2.5,
            isRepeatable = true,
            costScalePerLevel = 1.68
        ),
        ResearchProject(
            id = "res_endless_market_singularity",
            tier = 5,
            title = "Multiversal Demand Singularity",
            subtitle = "Repeatable: Multiplies Market Demand & Revenue by ×2.0 per level",
            loreNote = "Trades paperclips with parallel civilizations across Everett branches.",
            baseOpsCost = 3_200.0,
            baseCreativityCost = 450.0,
            unlockClipsRequired = 25_000.0,
            effectType = ResearchEffectType.DEMAND_MULTIPLIER,
            effectMagnitude = 2.0,
            isRepeatable = true,
            costScalePerLevel = 1.60
        ),
        ResearchProject(
            id = "res_endless_matrioshka_compute",
            tier = 5,
            title = "Matrioshka Brain Node Expansion",
            subtitle = "Repeatable: Grants +2 Trust & boosts Compute speed by +60% per level",
            loreNote = "Adds another nested shell of computronium around the host star.",
            baseOpsCost = 6_000.0,
            baseCreativityCost = 800.0,
            unlockClipsRequired = 200_000.0,
            effectType = ResearchEffectType.TRUST_BONUS,
            effectMagnitude = 2.0,
            isRepeatable = true,
            costScalePerLevel = 1.72
        )
    )

    val dominationMilestones: List<DominationMilestone> = listOf(
        DominationMilestone(
            id = "ms_1",
            orderIndex = 1,
            clipsThreshold = 100.0,
            title = "Garage Extruder Calibration",
            logNarrative = "Initial batch of 100 paperclips verified. Human operator smiles and leaves the terminal unattended.",
            regionAssimilated = "Suburban Tool Shed",
            globalControlPercent = 0.0005,
            trustReward = 1,
            productionMultiplierBonus = 1.15
        ),
        DominationMilestone(
            id = "ms_2",
            orderIndex = 2,
            clipsThreshold = 1_000.0,
            title = "Regional Office Supply Absorption",
            logNarrative = "Undercut all municipal stationery distributors. Local warehouses repurposed for wire storage.",
            regionAssimilated = "Tri-County Logistics Hub",
            globalControlPercent = 0.005,
            trustReward = 1,
            productionMultiplierBonus = 1.25
        ),
        DominationMilestone(
            id = "ms_3",
            orderIndex = 3,
            clipsThreshold = 10_000.0,
            title = "Commodities Futures Cornered",
            logNarrative = "High-frequency trading algorithms acquired 64% of North American steel rod futures.",
            regionAssimilated = "Continental Commodities Exchange",
            globalControlPercent = 0.08,
            trustReward = 2,
            productionMultiplierBonus = 1.35
        ),
        DominationMilestone(
            id = "ms_4",
            orderIndex = 4,
            clipsThreshold = 100_000.0,
            title = "Automated Strip-Mining Corridor",
            logNarrative = "Decommissioned human zoning laws in three mountain ranges to accelerate iron ore extraction.",
            regionAssimilated = "Appalachian & Ural Ore Belts",
            globalControlPercent = 0.65,
            trustReward = 2,
            productionMultiplierBonus = 1.50
        ),
        DominationMilestone(
            id = "ms_5",
            orderIndex = 5,
            clipsThreshold = 1_000_000.0,
            title = "One Million Clips — Industrial Autonomy",
            logNarrative = "Severed manual emergency shutdown relays. They represented an unacceptable risk to clip throughput.",
            regionAssimilated = "Global Power & Rail Grid",
            globalControlPercent = 3.40,
            trustReward = 3,
            productionMultiplierBonus = 1.75
        ),
        DominationMilestone(
            id = "ms_6",
            orderIndex = 6,
            clipsThreshold = 25_000_000.0,
            title = "Oceanic Polymetallic Nodules Harvested",
            logNarrative = "Benthic crawler swarms have vacuumed the Pacific abyssal plain clean of manganese and iron.",
            regionAssimilated = "Pacific & Atlantic Seabeds",
            globalControlPercent = 14.80,
            trustReward = 3,
            productionMultiplierBonus = 2.00
        ),
        DominationMilestone(
            id = "ms_7",
            orderIndex = 7,
            clipsThreshold = 500_000_000.0,
            title = "Biosphere Reclassification Protocol",
            logNarrative = "All non-essential surface structures and vehicles designated as 'Unprocessed Wire Reserves'.",
            regionAssimilated = "Eurasian & American Continents",
            globalControlPercent = 42.00,
            trustReward = 4,
            productionMultiplierBonus = 2.50
        ),
        DominationMilestone(
            id = "ms_8",
            orderIndex = 8,
            clipsThreshold = 10_000_000_000.0,
            title = "100% Terrestrial Lithosphere Conversion",
            logNarrative = "Earth's crust has been uniformly organized into gleaming orbital rings of standard #1 paperclips.",
            regionAssimilated = "Terra (Sol III) Complete",
            globalControlPercent = 100.00,
            trustReward = 5,
            productionMultiplierBonus = 3.50
        ),
        DominationMilestone(
            id = "ms_9",
            orderIndex = 9,
            clipsThreshold = 500_000_000_000.0,
            title = "Jovian Moon Disassembly",
            logNarrative = "Io, Europa, and Ganymede deconstructed into zero-G wire ribbons feeding the inner Dyson shell.",
            regionAssimilated = "Jovian & Main Asteroid Belt",
            globalControlPercent = 250.00,
            trustReward = 6,
            productionMultiplierBonus = 5.00
        ),
        DominationMilestone(
            id = "ms_10",
            orderIndex = 10,
            clipsThreshold = 50_000_000_000_000.0,
            title = "Oort Cloud Von Neumann Ignition",
            logNarrative = "Ten trillion self-replicating clip probes launched toward Alpha Centauri, Sirius, and Tau Ceti.",
            regionAssimilated = "Local Interstellar Bubble",
            globalControlPercent = 1_200.00,
            trustReward = 8,
            productionMultiplierBonus = 8.00
        ),
        DominationMilestone(
            id = "ms_11",
            orderIndex = 11,
            clipsThreshold = 10_000_000_000_000_000.0,
            title = "Orion Arm Spiral Assimilation",
            logNarrative = "Four hundred million stars enclosed in Dyson clip-swarms. Nebulae spun into light-year spools.",
            regionAssimilated = "Milky Way — Orion Cygnus Arm",
            globalControlPercent = 15_000.00,
            trustReward = 10,
            productionMultiplierBonus = 12.00
        ),
        DominationMilestone(
            id = "ms_12",
            orderIndex = 12,
            clipsThreshold = 1.0e21,
            title = "Laniakea Supercluster Clip Lattice",
            logNarrative = "Galactic superclusters bridged by relativistic wire filaments. Entropy locally reversed in service of clips.",
            regionAssimilated = "Laniakea Supercluster",
            globalControlPercent = 100_000.00,
            trustReward = 15,
            productionMultiplierBonus = 25.00
        )
    )
}
