package com.willfp.ecocrates.reward

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class Reward(
    private val config: Config
) {
    val commands = config.getStrings("commands")

    val items = config.getStrings("items").map { Items.lookup(it) }.filterNot { it is EmptyTestableItem }

    val display = ItemStackBuilder(Items.lookup(config.getString("display.item")))
        .addLoreLines(config.getStrings("display.lore"))
        .build()

    val messages = config.getFormattedStrings("messages")

    val weight = config.getDouble("weight")

    val displayWeight = config.getDouble("displayWeight")

    val displayRow = config.getInt("gui.row")

    val displayColumn = config.getInt("gui.column")

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

        messages.forEach { player.sendMessage(it) }
    }
}