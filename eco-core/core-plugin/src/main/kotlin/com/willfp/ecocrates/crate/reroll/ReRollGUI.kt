package com.willfp.ecocrates.crate.reroll

import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.roll.Roll
import com.willfp.ecocrates.plugin

object ReRollGUI {
    private const val metaKey = "ecocrates-reroll-fix"

    fun open(crate: Crate, roll: Roll, rerollNumber: Int, profile: RerollProfile) {
        val player = roll.player

        val price = profile.priceFor(rerollNumber + 1)
        val priceDisplay = price.getDisplay(player)

        val menu = menu(plugin.configYml.getInt("reroll.rows")) {
            setMask(
                FillerMask(
                    MaskItems.fromItemNames(plugin.configYml.getStrings("reroll.mask.items")),
                    *plugin.configYml.getStrings("reroll.mask.pattern").toTypedArray()
                )
            )

            title = plugin.configYml.getFormattedString("reroll.title")

            setSlot(
                plugin.configYml.getInt("reroll.accept.row"),
                plugin.configYml.getInt("reroll.accept.column"),
                slot(roll.reward.getDisplay(player, crate)) {
                    onLeftClick { _, _, _ ->
                        player.setMetadata(metaKey, plugin.metadataValueFactory.create(true))
                        player.closeInventory()
                        crate.handleFinish(roll)
                    }
                }
            )

            setSlot(
                plugin.configYml.getInt("reroll.reroll.row"),
                plugin.configYml.getInt("reroll.reroll.column"),
                slot(
                    ItemStackBuilder(Items.lookup(plugin.configYml.getString("reroll.reroll.item")))
                        .addLoreLines(
                            plugin.configYml.getStrings("reroll.reroll.lore")
                                .map { it.replace("%price%", priceDisplay) }
                        )
                        .setDisplayName(
                            plugin.configYml.getString("reroll.reroll.name")
                                .replace("%price%", priceDisplay)
                        )
                        .build()
                ) {
                    onLeftClick { _, _, _ ->
                        // Re-check affordability: balance may have changed since the GUI opened.
                        if (!price.canAfford(player)) {
                            player.setMetadata(metaKey, plugin.metadataValueFactory.create(true))
                            player.closeInventory()
                            crate.handleFinish(roll)
                            return@onLeftClick
                        }

                        // onClose runs inside closeInventory, so the flag has to be set first.
                        player.setMetadata(metaKey, plugin.metadataValueFactory.create(true))
                        // Close the GUI so the roll animation plays without it.
                        player.closeInventory()

                        val started = crate.open(
                            player,
                            roll.method,
                            roll.location,
                            rerollNumber = rerollNumber + 1,
                            placedCrate = roll.placedCrate
                        )

                        if (started) {
                            price.pay(player)
                        } else {
                            crate.handleFinish(roll)
                        }
                    }
                }
            )

            onClose { _, _ ->
                if (player.hasMetadata(metaKey)) {
                    player.removeMetadata(metaKey, plugin)
                } else {
                    plugin.scheduler.runLater(1) { crate.handleFinish(roll) }
                }
            }
        }

        menu.open(player)
    }
}
