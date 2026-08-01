package com.willfp.ecocrates.envoy.session

import com.willfp.ecocrates.envoy.EnvoyCategory
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * One running envoy event. Owns every crate it spawned; when it ends, all
 * of them are removed from the world.
 */
class EnvoySession(
    val category: EnvoyCategory,
    var ticksRemaining: Int
) {
    private val spawnList = CopyOnWriteArrayList<SpawnedEnvoy>()

    /** How many crates each player has collected this session. */
    private val collections = ConcurrentHashMap<UUID, Int>()

    val spawns: List<SpawnedEnvoy>
        get() = spawnList.toList()

    /** How many crates this session started with, for the collected count. */
    var totalSpawned: Int = 0
        internal set

    val collectedCount: Int
        get() = collections.values.sum()

    fun recordCollection(player: Player) {
        collections.merge(player.uniqueId, 1, Int::plus)
    }

    fun collectionsFor(player: OfflinePlayer): Int = collections[player.uniqueId] ?: 0

    /** Restores a persisted count, used when reloading mid-session. */
    internal fun restoreCollection(uuid: UUID, amount: Int) {
        collections[uuid] = amount
    }

    internal fun collectionEntries(): Map<UUID, Int> = collections.toMap()

    /**
     * The player who has collected the most this session, or null if nobody
     * has collected anything yet. Ties resolve arbitrarily but stably.
     */
    val topCollector: OfflinePlayer?
        get() = collections.maxByOrNull { it.value }
            ?.let { Bukkit.getOfflinePlayer(it.key) }

    val topCollectorAmount: Int
        get() = collections.values.maxOrNull() ?: 0

    fun addSpawn(spawn: SpawnedEnvoy) {
        spawnList.add(spawn)
    }

    fun removeSpawn(spawn: SpawnedEnvoy) {
        spawnList.remove(spawn)
    }

    /** The still-standing spawn at a block location, if any. */
    fun spawnAt(location: Location): SpawnedEnvoy? =
        spawnList.firstOrNull {
            it.blockLocation.world == location.world
                && it.blockLocation.blockX == location.blockX
                && it.blockLocation.blockY == location.blockY
                && it.blockLocation.blockZ == location.blockZ
        }

    fun despawnAll() {
        for (spawn in spawnList) {
            spawn.despawn()
        }

        spawnList.clear()
    }

    override fun toString() =
        "EnvoySession{category=${category.id},spawns=${spawnList.size},ticksRemaining=$ticksRemaining}"
}
