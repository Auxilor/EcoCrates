package com.willfp.ecocrates.crate

import com.willfp.eco.core.gui.addCustomSlots
import com.willfp.eco.core.gui.getSlotLocations
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.MenuBuilder
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.ecocrates.plugin
import org.bukkit.entity.Player

/**
 * The menu shown before a player pays to open a crate. Nothing is charged by this menu:
 * [onConfirm] runs the normal open flow, which checks and charges the price itself.
 */
object PayToOpenConfirmGUI {
    private const val path = "pay-to-open-confirm"

    fun open(crate: Crate, player: Player, onConfirm: () -> Unit) {
        val priceDisplay = crate.getPriceToOpenDisplay(player)

        fun String.withPlaceholders() = this
            .replace(oldValue = "%crate%", newValue = crate.name)
            .replace(oldValue = "%price%", newValue = priceDisplay)

        val menu = menu(plugin.configYml.getInt("$path.rows")) {
            setMask(plugin.configYml, path)

            title = plugin.configYml.getFormattedString("$path.title").withPlaceholders()

            addCustomSlots(plugin.configYml, path)

            addButton("$path.confirm", String::withPlaceholders) {
                player.closeInventory()
                onConfirm()
            }

            addButton("$path.cancel", String::withPlaceholders) {
                player.closeInventory()
            }
        }

        menu.open(player)
    }

    private fun MenuBuilder.addButton(
        buttonPath: String,
        withPlaceholders: String.() -> String,
        onClick: () -> Unit
    ) {
        val item = ItemStackBuilder(Items.lookup(plugin.configYml.getString("$buttonPath.item")))
            .addLoreLines(plugin.configYml.getStrings("$buttonPath.lore").map { it.withPlaceholders() })
            .setDisplayName(plugin.configYml.getString("$buttonPath.name").withPlaceholders())
            .build()

        for (location in plugin.configYml.getSlotLocations(buttonPath)) {
            setSlot(
                location.row,
                location.column,
                slot(item) {
                    onLeftClick { _, _, _ -> onClick() }
                }
            )
        }
    }
}
