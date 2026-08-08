package com.willfp.ecocrates.envoy

import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.drops.DropQueue
import com.willfp.ecocrates.plugin
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * The kinds of envoy item the give/take commands can hand out. The token is
 * what an admin types in the command's type slot, where crates take
 * "physical" or "virtual".
 */
enum class EnvoyItemType(val token: String) {
    FLARE("flare"),
    COMPASS("compass");

    companion object {
        fun fromToken(raw: String?): EnvoyItemType? =
            raw?.let { token -> entries.firstOrNull { it.token.equals(token, ignoreCase = true) } }

        val tokens = entries.map { it.token }
    }
}

/**
 * Shared plumbing for handing out and taking back envoy items, so all four
 * commands behave identically.
 */
object EnvoyItems {
    fun itemFor(category: EnvoyCategory, type: EnvoyItemType): ItemStack? =
        when (type) {
            EnvoyItemType.FLARE -> category.flare?.item
            EnvoyItemType.COMPASS -> category.compass?.item
        }

    private fun matches(category: EnvoyCategory, type: EnvoyItemType, item: ItemStack): Boolean =
        when (type) {
            EnvoyItemType.FLARE -> category.flare?.matches(item) == true
            EnvoyItemType.COMPASS -> category.compass?.matches(item) == true
        }

    private fun toGetKey(category: EnvoyCategory, type: EnvoyItemType): PersistentDataKey<Int>? =
        when (type) {
            EnvoyItemType.FLARE -> category.flare?.toGetKey
            EnvoyItemType.COMPASS -> category.compass?.toGetKey
        }

    /** Returns false if the category has that item disabled. */
    fun give(player: Player, category: EnvoyCategory, type: EnvoyItemType, amount: Int): Boolean {
        val item = itemFor(category, type) ?: return false

        DropQueue(player)
            .addItems(List(amount.coerceAtLeast(1)) { item.clone() })
            .forceTelekinesis()
            .push()

        return true
    }

    /** Returns false if the player didn't have enough to take. */
    fun take(player: Player, category: EnvoyCategory, type: EnvoyItemType, amount: Int): Boolean {
        val toTake = amount.coerceAtLeast(1)

        val held = player.inventory.contents
            .filterNotNull()
            .filter { matches(category, type, it) }
            .sumOf { it.amount }

        if (held < toTake) {
            return false
        }

        var remaining = toTake

        for (item in player.inventory.contents) {
            if (remaining <= 0) {
                break
            }

            if (item == null || !matches(category, type, item)) {
                continue
            }

            val removed = minOf(remaining, item.amount)
            item.amount -= removed

            if (item.amount == 0) {
                item.type = Material.AIR
            }

            remaining -= removed
        }

        return true
    }

    /** Queues items for an offline player, cashed in by [grantPending] on join. */
    fun adjustToGet(
        player: OfflinePlayer,
        category: EnvoyCategory,
        type: EnvoyItemType,
        amount: Int
    ) {
        val key = toGetKey(category, type) ?: return
        player.profile.write(key, player.profile.read(key) + amount)
    }

    /** Hands over everything queued for a player, and clears the queue. */
    fun grantPending(player: Player) {
        for (category in Envoys.values()) {
            for (type in EnvoyItemType.entries) {
                val key = toGetKey(category, type) ?: continue
                val pending = player.profile.read(key)

                if (pending <= 0) {
                    continue
                }

                player.profile.write(key, 0)
                give(player, category, type, pending)

                player.sendMessage(
                    plugin.langYml.getMessage("offline-envoy-items-received")
                        .replace("%amount%", pending.toString())
                        .replace("%item%", itemFor(category, type)?.itemMeta?.displayName ?: type.token)
                        .withEnvoyPlaceholders(category)
                )
            }
        }
    }
}
