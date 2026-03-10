package com.willfp.ecocrates.display

import com.willfp.eco.core.display.Display
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.display.DisplayPriority
import com.willfp.eco.core.fast.FastItemStack
import com.willfp.eco.core.placeholder.context.placeholderContext
import com.willfp.eco.util.formatEco
import com.willfp.ecocrates.crate.crate
import com.willfp.ecocrates.plugin
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object KeyDisplay : DisplayModule(plugin, DisplayPriority.LOW) {
    override fun display(
        itemStack: ItemStack,
        player: Player?,
        vararg args: Any
    ) {
        val crate = itemStack.crate ?: return

        if (crate.keyIsCustomItem) {
            return
        }

        val fis = FastItemStack.wrap(itemStack)
        val context = placeholderContext(player = player, item = fis.unwrap())

        fis.lore = crate.keyLore.formatEco(context)
            .map { Display.PREFIX + it } + fis.lore
    }
}
