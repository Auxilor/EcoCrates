package com.willfp.ecocrates.reward

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class Reward(
    private val plugin: EcoPlugin,
    private val config: Config
) {
    private val commands = config.getStrings("commands")

    private val items = config.getStrings("items").map { Items.lookup(it) }.filterNot { it is EmptyTestableItem }

    private val messages = config.getFormattedStrings("messages")

    val display: ItemStack = ItemStackBuilder(Items.lookup(config.getString("display.item")))
        .addLoreLines(config.getStrings("display.lore"))
        .setDisplayName(config.getString("display.name"))
        .build()

    fun getWeight(player: Player) = config.getDoubleFromExpression("weight.actual", player)

    fun getDisplayWeight(player: Player) = config.getDoubleFromExpression("weight.display", player)

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
    }
}