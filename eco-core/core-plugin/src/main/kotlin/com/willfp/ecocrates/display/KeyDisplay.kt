package com.willfp.ecocrates.display

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.display.Display
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.display.DisplayPriority
import com.willfp.eco.core.fast.FastItemStack
import com.willfp.ecocrates.crate.getAsKey
import org.bukkit.inventory.ItemStack

class KeyDisplay(
    plugin: EcoPlugin
) : DisplayModule(plugin, DisplayPriority.LOW) {
    override fun display(itemStack: ItemStack, vararg args: Any) {
        val crate = itemStack.getAsKey() ?: return

        if (crate.keyIsCustomItem) {
            return
        }

        val fis = FastItemStack.wrap(itemStack)

        fis.lore = crate.keyLore.map { Display.PREFIX + it } + fis.lore
    }
}
