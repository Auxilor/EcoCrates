package com.willfp.ecocrates.crate

import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.ecocrates.plugin
import org.bukkit.entity.Player

object KeyGUI {
    private lateinit var menu: Menu

    @JvmStatic
    fun update() {
        menu = menu(plugin.configYml.getInt("keygui.rows")) {
            setMask(
                FillerMask(
                    MaskItems.fromItemNames(plugin.configYml.getStrings("keygui.mask.items")),
                    *plugin.configYml.getStrings("keygui.mask.pattern").toTypedArray()
                )
            )

            title = plugin.configYml.getFormattedString("keygui.title")

            for (crate in Crates.values()) {
                crate.addToKeyGUI(this)
            }
        }
    }

    fun open(player: Player) {
        menu.open(player)
    }
}
