package com.willfp.ecocrates.crate.reroll

import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.roll.Roll

object ReRollGUI {
    private const val metaKey = "crates-accept"

    fun open(roll: Roll) {
        val plugin = roll.plugin
        val player = roll.player

        val menu = menu(plugin.configYml.getInt("reroll.rows")) {
            setMask(
                FillerMask(
                    MaskItems.fromItemNames(plugin.configYml.getStrings("reroll.mask.items")),
                    *plugin.configYml.getStrings("reroll.mask.pattern").toTypedArray()
                )
            )

            setTitle(plugin.configYml.getFormattedString("reroll.title"))

            for (crate in Crates.values()) {
                crate.addToKeyGUI(this)
            }

            setSlot(
                plugin.configYml.getInt("reroll.accept.row"),
                plugin.configYml.getInt("reroll.accept.column"),
                slot(roll.reward.getDisplay(player, roll.crate)) {
                    onLeftClick { _, _, _ ->
                        player.setMetadata(metaKey, plugin.metadataValueFactory.create(true))
                        player.closeInventory()
                        roll.crate.handleFinish(roll)
                    }
                }
            )

            setSlot(
                plugin.configYml.getInt("reroll.reroll.row"),
                plugin.configYml.getInt("reroll.reroll.column"),
                slot(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("reroll.reroll.item")))
                        .addLoreLines(plugin.configYml.getStrings("reroll.reroll.lore"))
                        .setDisplayName(plugin.configYml.getString("reroll.reroll.name"))
                        .build()
                ) {
                    onLeftClick { _, _, _ ->
                        player.setMetadata(metaKey, plugin.metadataValueFactory.create(true))
                        roll.crate.open(player, roll.location, isReroll = true)
                    }
                }
            )

            onClose { _, _ ->
                if (player.hasMetadata(metaKey)) {
                    player.removeMetadata(metaKey, plugin)
                } else {
                    roll.crate.handleFinish(roll)
                }
            }
        }

        menu.open(player)
    }
}
