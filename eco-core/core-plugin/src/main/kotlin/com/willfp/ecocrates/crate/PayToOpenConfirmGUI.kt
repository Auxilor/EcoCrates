package com.willfp.ecocrates.crate

import com.willfp.ecocrates.plugin
import org.bukkit.entity.Player
import wiki.ecohub.game.item.Items
import wiki.ecohub.game.item.builder.ItemStackBuilder
import wiki.ecohub.modules.menus.addCustomSlots
import wiki.ecohub.modules.menus.getSlotLocations
import wiki.ecohub.modules.menus.menu
import wiki.ecohub.modules.menus.menu.MenuBuilder
import wiki.ecohub.modules.menus.setMask
import wiki.ecohub.modules.menus.slot

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

            setTitle(plugin.configYml.getFormattedString("$path.title").withPlaceholders())

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
                row = location.row,
                column = location.column,
                slot = slot(item) {
                    onLeftClick { _, _, _ -> onClick() }
                }
            )
        }
    }
}
