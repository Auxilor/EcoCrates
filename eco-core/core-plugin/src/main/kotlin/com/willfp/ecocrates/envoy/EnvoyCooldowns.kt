package com.willfp.ecocrates.envoy

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-player envoy cooldowns, held in memory.
 *
 * These are deliberately not persisted: collection cooldowns are per-rarity
 * and usually shorter than a session, and persisting them would mean
 * registering an unbounded number of PersistentDataKeys at config-load time.
 */
object EnvoyCooldowns {
    // key -> expiry timestamp in millis
    private val expiries = ConcurrentHashMap<UUID, MutableMap<String, Long>>()

    private fun remaining(player: Player, key: String): Int {
        val expiry = expiries[player.uniqueId]?.get(key) ?: return 0
        val millisLeft = expiry - System.currentTimeMillis()

        if (millisLeft <= 0) {
            return 0
        }

        return Math.ceil(millisLeft / 50.0).toInt()
    }

    private fun apply(player: Player, key: String, ticks: Int) {
        if (ticks <= 0) {
            return
        }

        expiries.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }[key] =
            System.currentTimeMillis() + (ticks * 50L)
    }

    fun remainingCollection(player: Player, rarity: EnvoyRarity): Int =
        remaining(player, "collect:${rarity.categoryId}:${rarity.id}")

    fun applyCollection(player: Player, rarity: EnvoyRarity) =
        apply(player, "collect:${rarity.categoryId}:${rarity.id}", rarity.collectionCooldown)

    fun remainingFlare(player: Player, categoryId: String): Int =
        remaining(player, "flare:$categoryId")

    fun applyFlare(player: Player, categoryId: String, cooldownTicks: Int) =
        apply(player, "flare:$categoryId", cooldownTicks)
}
