package com.willfp.ecocrates.util

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** All-or-nothing removal of matching items from a player's inventory. */
object InventoryTake {
    /**
     * Removes [amount] items matching [matches], or nothing at all if the
     * player holds fewer than that.
     *
     * @return Whether the items were taken.
     */
    fun takeAllOrNothing(player: Player, amount: Int, matches: (ItemStack) -> Boolean): Boolean {
        val toTake = amount.coerceAtLeast(1)

        val held = player.inventory.contents
            .filterNotNull()
            .filter(matches)
            .sumOf { it.amount }

        if (held < toTake) {
            return false
        }

        var remaining = toTake

        for (item in player.inventory.contents) {
            if (remaining <= 0) {
                break
            }

            if (item == null || !matches(item)) {
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
}
