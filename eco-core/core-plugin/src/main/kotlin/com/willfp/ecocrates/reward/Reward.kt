package com.willfp.ecocrates.reward

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

class Reward(
    private val plugin: EcoPlugin,
    private val config: Config
) {
    val id = config.getString("id")

    private val commands = config.getStrings("commands")

    private val items = config.getStrings("items").map { Items.lookup(it) }.filterNot { it is EmptyTestableItem }

    private val messages = config.getFormattedStrings("messages")

    private val maxWins = config.getInt("max-wins")

    private val winsKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_wins"),
        PersistentDataKeyType.INT,
        0
    ).apply {
        // Only register if max wins are being limited
        if (maxWins > 0) player()
    }

    val display: ItemStack = ItemStackBuilder(Items.lookup(config.getString("display.item")))
        .addLoreLines(config.getStrings("display.lore"))
        .setDisplayName(config.getString("display.name"))
        .build()

    fun getWeight(player: Player): Double {
        val weight = config.getDoubleFromExpression("weight.actual", player)
        if (maxWins > 0) {
            if (player.profile.read(winsKey) >= maxWins) {
                return 0.0
            }
        }
        return weight
    }

    fun getDisplayWeight(player: Player): Double {
        val weight = config.getDoubleFromExpression("weight.display", player)
        if (maxWins > 0) {
            if (player.profile.read(winsKey) >= maxWins) {
                return 0.0
            }
        }
        return weight
    }

    val displayRow = config.getInt("display.row")

    val displayColumn = config.getInt("display.column")

    val displayName = config.getFormattedString("display.name")

    fun giveTo(player: Player) {
        for (command in commands) {
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                command.replace("%player%", player.name)
            )
        }

        DropQueue(player)
            .addItems(items.map { it.item })
            .forceTelekinesis()
            .push()

        messages.forEach { player.sendMessage(plugin.langYml.prefix + it) }

        if (maxWins > 0) {
            player.profile.write(winsKey, player.profile.read(winsKey) + 1)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Reward) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}
