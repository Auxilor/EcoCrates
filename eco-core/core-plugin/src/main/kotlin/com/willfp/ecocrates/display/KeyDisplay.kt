package com.willfp.ecocrates.display

import com.willfp.eco.core.display.DisplayContext
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.display.DisplayPriority
import com.willfp.eco.util.formatEcoRich
import com.willfp.ecocrates.crate.key
import com.willfp.ecocrates.plugin

object KeyDisplay : DisplayModule(plugin, DisplayPriority.LOW) {
    override fun display(context: DisplayContext) {
        val sharedKey = context.itemStack.key ?: return

        if (sharedKey.isCustomItem) {
            return
        }

        context.lore.prepend(sharedKey.lore.formatEcoRich(context.placeholderContext))
    }
}
