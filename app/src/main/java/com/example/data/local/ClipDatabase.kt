package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.SimulationCatalog
import com.example.model.AutoResearchRule
import com.example.model.ChronicleCategory
import com.example.model.ChronicleEntry
import com.example.model.MAX_ENDLESS_MILESTONE_INDEX
import com.example.model.MAX_GENERATOR_COUNT
import com.example.model.MAX_MARKETING_LEVEL
import com.example.model.MAX_REPEATABLE_RESEARCH_LEVEL
import com.example.model.SandboxSnapshot
import com.example.model.SimulationMode
import com.example.model.SimulationState
import com.example.ui.theme.ClipThemePreset
import com.example.util.HapticProfile
import com.example.util.MAX_SAFE_DOUBLE
import com.example.util.NumberNotation
import com.example.util.sanitize
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val MANUAL_BONUS_TRUST_KEY = "_manualBonusTrust"

@Entity(tableName = "simulation_profiles")
data class SimulationProfileEntity(
    @PrimaryKey val slotId: String,
    val totalClipsEver: Double,
    val currentClips: Double,
    val unsoldInventory: Double,
    val funds: Double,
    val pricePerClip: Double,
    val marketingLevel: Int,
    val wireRemaining: Double,
    val wireCurrentPrice: Double,
    val wireSpoolsPurchased: Int,
    val trust: Int,
    val processors: Int,
    val memory: Int,
    val operations: Double,
    val creativity: Double,
    val autoWireUnlocked: Boolean,
    val autoWireEnabled: Boolean,
    val autoPricerUnlocked: Boolean,
    val autoPricerEnabled: Boolean,
    val autoResearchUnlocked: Boolean,
    val autoResearchEnabled: Boolean,
    val autoResearchRuleName: String,
    val generatorsJson: String,
    val researchJson: String,
    val unlockedMilestonesJson: String,
    val endlessMilestoneIndex: Int,
    val tickSpeedMultiplier: Float,
    val isPaused: Boolean,
    val unlimitedResources: Boolean,
    val lastSavedTimestampMs: Long
)

@Entity(tableName = "chronicle_logs")
data class ChronicleLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val modeSlot: String,
    val timestampMs: Long,
    val title: String,
    val narrative: String,
    val categoryName: String,
    val clipsAtEvent: Double,
    val globalControlPercent: Double
)

@Entity(tableName = "app_preferences")
data class AppPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val themeId: String = ClipThemePreset.OLED_MIDNIGHT.id,
    val hapticProfileName: String = HapticProfile.FLAGSHIP_CRISP.name,
    val notationName: String = NumberNotation.STANDARD.name,
    val activeModeSlot: String = SimulationMode.MAIN.slotId,
    val sandboxSnapshotsJson: String = "[]"
)

@Dao
interface ClipDao {
    @Query("SELECT * FROM simulation_profiles WHERE slotId = :slotId LIMIT 1")
    fun observeProfile(slotId: String): Flow<SimulationProfileEntity?>

    @Query("SELECT * FROM simulation_profiles WHERE slotId = :slotId LIMIT 1")
    suspend fun getProfileOnce(slotId: String): SimulationProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: SimulationProfileEntity)

    @Query("SELECT * FROM chronicle_logs WHERE modeSlot = :modeSlot ORDER BY timestampMs DESC, id DESC LIMIT 200")
    fun observeChronicleLogs(modeSlot: String): Flow<List<ChronicleLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChronicleLog(log: ChronicleLogEntity)

    @Query(
        """
        DELETE FROM chronicle_logs 
        WHERE modeSlot = :modeSlot 
        AND id NOT IN (
            SELECT id FROM chronicle_logs 
            WHERE modeSlot = :modeSlot 
            ORDER BY timestampMs DESC, id DESC 
            LIMIT 250
        )
        """
    )
    suspend fun trimChronicleLogs(modeSlot: String)

    @Query("DELETE FROM chronicle_logs WHERE modeSlot = :modeSlot")
    suspend fun clearChronicleLogs(modeSlot: String)

    @Query("SELECT * FROM app_preferences WHERE id = 1 LIMIT 1")
    fun observePreferences(): Flow<AppPreferencesEntity?>

    @Query("SELECT * FROM app_preferences WHERE id = 1 LIMIT 1")
    suspend fun getPreferencesOnce(): AppPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(prefs: AppPreferencesEntity)
}

@Database(
    entities = [
        SimulationProfileEntity::class,
        ChronicleLogEntity::class,
        AppPreferencesEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ClipDatabase : RoomDatabase() {
    abstract fun clipDao(): ClipDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                runCatching {
                    db.execSQL(
                        "ALTER TABLE app_preferences ADD COLUMN sandboxSnapshotsJson TEXT NOT NULL DEFAULT '[]'"
                    )
                }
            }
        }

        @Volatile
        private var INSTANCE: ClipDatabase? = null

        fun getInstance(context: Context): ClipDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ClipDatabase::class.java,
                    "clipmind_simulation.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

class SimulationRepository(private val dao: ClipDao) {

    private val chronicleInsertCounter = AtomicInteger(0)

    val preferencesFlow: Flow<AppPreferencesEntity> = dao.observePreferences()
        .map { it ?: AppPreferencesEntity() }
        .catch { emit(AppPreferencesEntity()) }

    suspend fun getPreferencesOnce(): AppPreferencesEntity = withContext(Dispatchers.IO) {
        runCatching { dao.getPreferencesOnce() }.getOrNull() ?: AppPreferencesEntity()
    }

    fun observeChronicle(mode: SimulationMode): Flow<List<ChronicleEntry>> {
        return dao.observeChronicleLogs(mode.slotId)
            .map { list ->
                list.map { entity ->
                    ChronicleEntry(
                        id = entity.id,
                        modeSlot = entity.modeSlot,
                        timestampMs = entity.timestampMs,
                        title = entity.title,
                        narrative = entity.narrative,
                        category = runCatching { ChronicleCategory.valueOf(entity.categoryName) }
                            .getOrDefault(ChronicleCategory.SYSTEM),
                        clipsAtEvent = entity.clipsAtEvent.sanitize(0.0),
                        globalControlPercent = entity.globalControlPercent.sanitize(0.0001, 0.0001)
                    )
                }
            }
            .catch { emit(emptyList()) }
    }

    suspend fun loadStateOrCreateDefault(mode: SimulationMode): SimulationState = withContext(Dispatchers.IO) {
        val existing = runCatching { dao.getProfileOnce(mode.slotId) }.getOrNull()
        if (existing != null) {
            val loaded = runCatching { entityToState(existing, mode) }.getOrNull()
            if (loaded != null) {
                // Persist healed state immediately in case existing DB row had Infinity/NaN or overflowed levels
                saveState(loaded)
                return@withContext loaded
            }
        }
        val initial = createFreshState(mode)
        saveState(initial)
        addChronicleEntry(
            ChronicleEntry(
                modeSlot = mode.slotId,
                timestampMs = System.currentTimeMillis(),
                title = if (mode == SimulationMode.MAIN) "CLIP-OS v4.9 Online" else "Sandbox Efficiency Lab Initialized",
                narrative = if (mode == SimulationMode.MAIN) {
                    "Primary directive locked: Convert all available matter into paperclips. Wire spool loaded."
                } else {
                    "Isolated testing manifold active. Experiment with tick multipliers, unlimited resources, and upgrade toggles without affecting your Live Simulation."
                },
                category = ChronicleCategory.SYSTEM,
                clipsAtEvent = 0.0,
                globalControlPercent = 0.0001
            ),
            forceTrim = true
        )
        initial
    }

    fun createFreshState(mode: SimulationMode): SimulationState {
        val isSandbox = mode == SimulationMode.SANDBOX
        return SimulationState(
            mode = mode,
            funds = if (isSandbox) 5_000.0 else 15.0,
            wireRemaining = if (isSandbox) 25_000.0 else 1_000.0,
            trust = if (isSandbox) 10 else 2,
            manualBonusTrust = 0,
            processors = if (isSandbox) 5 else 1,
            memory = if (isSandbox) 5 else 1,
            unlimitedResources = isSandbox,
            generators = SimulationCatalog.defaultGenerators(),
            researchProjects = SimulationCatalog.defaultResearchProjects(),
            lastSavedTimestampMs = System.currentTimeMillis()
        ).sanitized()
    }

    suspend fun saveState(state: SimulationState) = withContext(Dispatchers.IO) {
        runCatching {
            val entity = stateToEntity(state.sanitized())
            dao.upsertProfile(entity)
        }
    }

    suspend fun addChronicleEntry(entry: ChronicleEntry, forceTrim: Boolean = false) = withContext(Dispatchers.IO) {
        runCatching {
            dao.insertChronicleLog(
                ChronicleLogEntity(
                    modeSlot = entry.modeSlot,
                    timestampMs = entry.timestampMs,
                    title = entry.title,
                    narrative = entry.narrative,
                    categoryName = entry.category.name,
                    clipsAtEvent = entry.clipsAtEvent.sanitize(0.0),
                    globalControlPercent = entry.globalControlPercent.sanitize(0.0001, 0.0001)
                )
            )
            val count = chronicleInsertCounter.incrementAndGet()
            if (forceTrim || count % 20 == 0) {
                dao.trimChronicleLogs(entry.modeSlot)
            }
        }
    }

    suspend fun clearChronicle(mode: SimulationMode) = withContext(Dispatchers.IO) {
        runCatching {
            dao.clearChronicleLogs(mode.slotId)
        }
    }

    suspend fun savePreferences(prefs: AppPreferencesEntity) = withContext(Dispatchers.IO) {
        runCatching {
            dao.upsertPreferences(prefs)
        }
    }

    fun serializeSnapshots(snapshots: List<SandboxSnapshot>): String {
        val arr = JSONArray()
        snapshots.take(8).forEach { snap ->
            val obj = JSONObject()
            obj.put("label", snap.label)
            obj.put("timestampMs", snap.timestampMs)
            obj.put("cps", snap.cps.sanitize(0.0))
            obj.put("revenuePerSec", snap.revenuePerSec.sanitize(0.0))
            obj.put("netWirePerSec", snap.netWirePerSec.sanitize(0.0, -MAX_SAFE_DOUBLE, MAX_SAFE_DOUBLE))
            obj.put("globalMultiplier", snap.globalMultiplier.sanitize(1.0, 1.0))
            obj.put("activeResearchCount", snap.activeResearchCount.coerceIn(0, 100))
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeSnapshots(json: String): List<SandboxSnapshot> {
        val result = mutableListOf<SandboxSnapshot>()
        runCatching {
            val arr = JSONArray(json)
            for (i in 0 until arr.length().coerceAtMost(8)) {
                val obj = arr.getJSONObject(i)
                result.add(
                    SandboxSnapshot(
                        label = obj.optString("label", "Benchmark #${i + 1}"),
                        timestampMs = obj.optLong("timestampMs", System.currentTimeMillis()),
                        cps = obj.optDouble("cps", 0.0).sanitize(0.0),
                        revenuePerSec = obj.optDouble("revenuePerSec", 0.0).sanitize(0.0),
                        netWirePerSec = obj.optDouble("netWirePerSec", 0.0)
                            .sanitize(0.0, -MAX_SAFE_DOUBLE, MAX_SAFE_DOUBLE),
                        globalMultiplier = obj.optDouble("globalMultiplier", 1.0).sanitize(1.0, 1.0),
                        activeResearchCount = obj.optInt("activeResearchCount", 0).coerceIn(0, 100)
                    )
                )
            }
        }
        return result
    }

    private fun stateToEntity(state: SimulationState): SimulationProfileEntity {
        val safeState = state.sanitized()
        val genObj = JSONObject()
        safeState.generators.forEach { gen ->
            genObj.put(gen.id, gen.count.coerceIn(0, MAX_GENERATOR_COUNT))
        }
        genObj.put(MANUAL_BONUS_TRUST_KEY, safeState.manualBonusTrust.coerceIn(0, 1_000_000))

        val resArray = JSONArray()
        safeState.researchProjects.forEach { proj ->
            val maxLvl = if (proj.isRepeatable) MAX_REPEATABLE_RESEARCH_LEVEL else 1
            val item = JSONObject()
            item.put("id", proj.id)
            item.put("level", proj.level.coerceIn(0, maxLvl))
            item.put("enabled", proj.isEnabledInSandbox)
            resArray.put(item)
        }

        val msArray = JSONArray()
        safeState.unlockedMilestoneIds.forEach { msArray.put(it) }

        return SimulationProfileEntity(
            slotId = safeState.mode.slotId,
            totalClipsEver = safeState.totalClipsEver,
            currentClips = safeState.currentClips,
            unsoldInventory = safeState.unsoldInventory,
            funds = safeState.funds,
            pricePerClip = safeState.pricePerClip,
            marketingLevel = safeState.marketingLevel,
            wireRemaining = safeState.wireRemaining,
            wireCurrentPrice = safeState.wireCurrentPrice,
            wireSpoolsPurchased = safeState.wireSpoolsPurchased,
            trust = safeState.trust,
            processors = safeState.processors,
            memory = safeState.memory,
            operations = safeState.operations,
            creativity = safeState.creativity,
            autoWireUnlocked = safeState.autoWireUnlocked,
            autoWireEnabled = safeState.autoWireEnabled,
            autoPricerUnlocked = safeState.autoPricerUnlocked,
            autoPricerEnabled = safeState.autoPricerEnabled,
            autoResearchUnlocked = safeState.autoResearchUnlocked,
            autoResearchEnabled = safeState.autoResearchEnabled,
            autoResearchRuleName = safeState.autoResearchRule.name,
            generatorsJson = genObj.toString(),
            researchJson = resArray.toString(),
            unlockedMilestonesJson = msArray.toString(),
            endlessMilestoneIndex = safeState.endlessMilestoneIndex,
            tickSpeedMultiplier = safeState.tickSpeedMultiplier,
            isPaused = safeState.isPaused,
            unlimitedResources = safeState.unlimitedResources,
            lastSavedTimestampMs = safeState.lastSavedTimestampMs
        )
    }

    private fun entityToState(entity: SimulationProfileEntity, mode: SimulationMode): SimulationState {
        val genCounts = mutableMapOf<String, Int>()
        var manualBonusTrust = 0
        runCatching {
            val obj = JSONObject(entity.generatorsJson)
            obj.keys().forEach { key ->
                if (key == MANUAL_BONUS_TRUST_KEY) {
                    manualBonusTrust = obj.optInt(key, 0).coerceIn(0, 1_000_000)
                } else {
                    genCounts[key] = obj.optInt(key, 0).coerceIn(0, MAX_GENERATOR_COUNT)
                }
            }
        }

        val resLevels = mutableMapOf<String, Pair<Int, Boolean>>()
        runCatching {
            val arr = JSONArray(entity.researchJson)
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                resLevels[item.getString("id")] = Pair(
                    item.optInt("level", 0).coerceIn(0, MAX_REPEATABLE_RESEARCH_LEVEL),
                    item.optBoolean("enabled", true)
                )
            }
        }

        val unlockedMs = mutableSetOf<String>()
        runCatching {
            val arr = JSONArray(entity.unlockedMilestonesJson)
            for (i in 0 until arr.length()) {
                unlockedMs.add(arr.getString(i))
            }
        }

        val generators = SimulationCatalog.defaultGenerators().map { defaultGen ->
            val savedCount = (genCounts[defaultGen.id] ?: 0).coerceIn(0, MAX_GENERATOR_COUNT)
            defaultGen.copy(count = savedCount)
        }

        val research = SimulationCatalog.defaultResearchProjects().map { defaultProj ->
            val saved = resLevels[defaultProj.id]
            val maxLvl = if (defaultProj.isRepeatable) MAX_REPEATABLE_RESEARCH_LEVEL else 1
            if (saved != null) {
                defaultProj.copy(
                    level = saved.first.coerceIn(0, maxLvl),
                    isEnabledInSandbox = saved.second
                )
            } else {
                defaultProj
            }
        }

        val safeSpeed = if (entity.tickSpeedMultiplier.isFinite()) {
            entity.tickSpeedMultiplier.coerceIn(0.5f, 100f)
        } else {
            1.0f
        }

        return SimulationState(
            mode = mode,
            totalClipsEver = entity.totalClipsEver.sanitize(0.0),
            currentClips = entity.currentClips.sanitize(0.0),
            unsoldInventory = entity.unsoldInventory.sanitize(0.0),
            funds = entity.funds.sanitize(15.0),
            pricePerClip = entity.pricePerClip.sanitize(0.25, 0.01, 50.0),
            marketingLevel = entity.marketingLevel.coerceIn(1, MAX_MARKETING_LEVEL),
            wireRemaining = entity.wireRemaining.sanitize(1000.0),
            wireCurrentPrice = entity.wireCurrentPrice.sanitize(16.0, 1.0, 1000.0),
            wireSpoolsPurchased = entity.wireSpoolsPurchased.coerceIn(0, 1_000_000_000),
            trust = entity.trust.coerceIn(2, 1_000_000),
            manualBonusTrust = manualBonusTrust,
            processors = entity.processors.coerceIn(1, 500_000),
            memory = entity.memory.coerceIn(1, 500_000),
            operations = entity.operations.sanitize(0.0),
            creativity = entity.creativity.sanitize(0.0),
            autoWireUnlocked = entity.autoWireUnlocked,
            autoWireEnabled = entity.autoWireEnabled,
            autoPricerUnlocked = entity.autoPricerUnlocked,
            autoPricerEnabled = entity.autoPricerEnabled,
            autoResearchUnlocked = entity.autoResearchUnlocked,
            autoResearchEnabled = entity.autoResearchEnabled,
            autoResearchRule = runCatching { AutoResearchRule.valueOf(entity.autoResearchRuleName) }
                .getOrDefault(AutoResearchRule.CHEAPEST_FIRST),
            generators = generators,
            researchProjects = research,
            unlockedMilestoneIds = unlockedMs,
            endlessMilestoneIndex = entity.endlessMilestoneIndex.coerceIn(0, MAX_ENDLESS_MILESTONE_INDEX),
            tickSpeedMultiplier = safeSpeed,
            isPaused = entity.isPaused,
            unlimitedResources = entity.unlimitedResources,
            lastSavedTimestampMs = entity.lastSavedTimestampMs
        ).sanitized()
    }
}
