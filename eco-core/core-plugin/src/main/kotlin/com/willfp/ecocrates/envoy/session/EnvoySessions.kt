package com.willfp.ecocrates.envoy.session

import com.willfp.ecocrates.envoy.EnvoyCategory
import com.willfp.ecocrates.envoy.spawn.EnvoyPoints
import com.willfp.ecocrates.envoy.spawn.SpawnLocationMode
import com.willfp.ecocrates.envoy.spawn.SpawnLocator
import com.willfp.ecocrates.plugin
import com.willfp.eco.util.savedDisplayName
import org.bukkit.Bukkit
import org.bukkit.Location
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

/**
 * The one place that knows whether an envoy is running.
 *
 * Only one session may be active globally. Starting one while another runs
 * fails - an admin or a schedule stepping on a live player-facing event
 * should be visible, not a silent takeover.
 */
object EnvoySessions {
    @Volatile
    var active: EnvoySession? = null
        private set

    // Bookkeeping for the auto-start scheduler, per category.
    private val lastStartTick = ConcurrentHashMap<String, Long>()
    private val lastStartTime = ConcurrentHashMap<String, LocalTime>()

    @Volatile
    private var currentTick = 0L

    fun isActive(categoryId: String? = null): Boolean {
        val session = active ?: return false

        return categoryId == null || session.category.id.equals(categoryId, ignoreCase = true)
    }

    /** Total crates left standing in the active session. */
    fun remaining(): Int = active?.spawns?.size ?: 0

    /**
     * Starts a session. Returns false if one is already active, or if the
     * category could not place a single crate.
     */
    fun start(category: EnvoyCategory): Boolean {
        if (active != null) {
            return false
        }

        val session = EnvoySession(category, category.duration)
        val count = category.rollSpawnCount()

        repeat(count) {
            val rarity = category.randomRarity() ?: return@repeat
            val candidate = rollCandidate(category)

            if (candidate == null) {
                plugin.logger.warning(
                    "Envoy '${category.id}' could not roll a candidate location for " +
                        "rarity '${rarity.id}' - check its location-type configuration."
                )
                return@repeat
            }

            val resolved = SpawnLocator.resolve(candidate)

            if (resolved == null) {
                plugin.logger.warning(
                    "Envoy '${category.id}' rarity '${rarity.id}': no air block found near " +
                        "${candidate.world?.name}@${candidate.blockX},${candidate.blockY}," +
                        "${candidate.blockZ} - skipping this crate."
                )
                return@repeat
            }

            val spawn = SpawnedEnvoy(category, rarity, resolved)
            spawn.place(withFireworks = true)
            session.addSpawn(spawn)
        }

        if (session.spawns.isEmpty()) {
            plugin.logger.warning(
                "Envoy '${category.id}' started but placed no crates - ending it immediately."
            )
            return false
        }

        session.totalSpawned = session.spawns.size

        active = session
        markStarted(category.id)
        EnvoySessionStore.save(session)

        category.triggerStartEffects(session.totalSpawned)

        return true
    }

    fun end() {
        val session = active ?: return

        // Snapshot before despawnAll clears the spawn list.
        val remaining = session.spawns.size
        val collected = session.collectedCount
        val topCollector = session.topCollector?.savedDisplayName
            ?: plugin.langYml.getMessage("envoy-no-top-collector")
        val topAmount = session.topCollectorAmount

        active = null
        session.despawnAll()
        EnvoySessionStore.clear()

        session.category.triggerEndEffects(topCollector, topAmount, collected, remaining)
    }

    /** Called when a player collects a crate. Ends the session if it was the last one. */
    fun collect(spawn: SpawnedEnvoy) {
        val session = active ?: return

        session.removeSpawn(spawn)
        spawn.despawn()

        if (session.spawns.isEmpty()) {
            end()
            return
        }

        EnvoySessionStore.save(session)
    }

    fun tick(tick: Int) {
        currentTick++

        val session = active ?: return

        for (spawn in session.spawns) {
            if (!spawn.blockLocation.isChunkLoaded) {
                continue
            }

            spawn.tick(tick)
        }

        session.ticksRemaining--

        if (session.ticksRemaining <= 0) {
            end()
        }
    }

    /** Rebuilds an interrupted session from disk, if configured to. */
    fun restore() {
        if (!plugin.configYml.getBool("envoy.restore-session-on-restart")) {
            EnvoySessionStore.clear()
            return
        }

        val session = EnvoySessionStore.load() ?: return

        if (session.spawns.isEmpty() || session.ticksRemaining <= 0) {
            session.despawnAll()
            EnvoySessionStore.clear()
            return
        }

        for (spawn in session.spawns) {
            spawn.place(withFireworks = false)
        }

        active = session
        markStarted(session.category.id)
        EnvoySessionStore.save(session)
    }

    /**
     * Tears down visuals and world blocks on disable, but leaves the file
     * intact so [restore] can bring the session back.
     */
    fun shutdown() {
        val session = active ?: return

        EnvoySessionStore.save(session)
        session.despawnAll()
        active = null
    }

    fun markStarted(categoryId: String) {
        lastStartTick[categoryId] = currentTick
        lastStartTime[categoryId] = LocalTime.now()
    }

    fun lastStartTicks(categoryId: String): Long =
        currentTick - (lastStartTick[categoryId] ?: 0L)

    fun lastStartTime(categoryId: String): LocalTime? = lastStartTime[categoryId]

    private fun rollCandidate(category: EnvoyCategory): Location? =
        when (category.locationMode) {
            SpawnLocationMode.POINTS -> EnvoyPoints.randomFor(category.id)

            SpawnLocationMode.RADIUS -> {
                val box = category.radiusBox ?: return null
                val world = Bukkit.getWorlds()
                    .firstOrNull { it.name.equals(box.worldName, true) } ?: return null
                val pos = box.roll()

                Location(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
            }
        }
}
