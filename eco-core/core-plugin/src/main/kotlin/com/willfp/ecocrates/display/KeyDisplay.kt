package com.willfp.ecocrates.display

import com.willfp.ecocrates.crate.key
import com.willfp.ecocrates.plugin
import wiki.ecohub.ui.display.DisplayContext
import wiki.ecohub.ui.display.DisplayModule
import wiki.ecohub.ui.display.DisplayPriority
import wiki.ecohub.ui.text.StringUtils

object KeyDisplay : DisplayModule(plugin, DisplayPriority.LOW) {
    override fun display(context: DisplayContext) {
        val sharedKey = context.getItemStack().key ?: return

        if (sharedKey.isCustomItem) {
            return
        }

        context.getLore().prepend(
            sharedKey.lore.map { StringUtils.formatToRichComponent(it, context.getPlaceholderContext()) }
        )
    }
}
