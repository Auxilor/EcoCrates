package com.willfp.ecocrates.crate.reroll

import com.willfp.eco.core.gui.getSlotLocations
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.price.ConfiguredPrice
import com.willfp.ecocrates.crate.open.OpenPick
import com.willfp.ecocrates.crate.open.OpenSession
import com.willfp.ecocrates.plugin
import org.bukkit.inventory.ItemStack

/**
 * The menu offered after a roll to accept the reward of one pick or pay to roll that pick again.
 *
 * On a reroll, affordability is re-checked because the balance may have changed since the menu opened.
 * The flag is set before closing because onClose runs inside closeInventory, and the menu closes so the
 * roll animation plays without it.
 */
object ReRollGUI {
    private const val metaKey = "ecocrates-reroll-fix"

    internal fun open(session: OpenSession, pick: OpenPick) {
        val player = session.player

        val price = session.sessionSource.rerollPrice(pick)
        val priceDisplay = price.getDisplay(player)

        val menu = menu(plugin.configYml.getInt("reroll.rows")) {
            setMask(plugin.configYml, "reroll")

            title = plugin.configYml.getFormattedString("reroll.title")

            for (location in plugin.configYml.getSlotLocations("reroll.accept")) {
                setSlot(
                    location.row,
                    location.column,
                    slot(pick.reward.getDisplay(player, session.source)) {
                        onLeftClick { _, _, _ ->
                            player.setMetadata(metaKey, plugin.metadataValueFactory.create(true))
                            player.closeInventory()
                            session.accept(pick)
                        }
                    }
                )
            }

            for (location in plugin.configYml.getSlotLocations("reroll.reroll")) {
                setSlot(
                    location.row,
                    location.column,
                    slot(rerollItem(priceDisplay)) {
                        onLeftClick { _, _, _ ->
                            reroll(session, pick, price)
                        }
                    }
                )
            }

            onClose { _, _ ->
                if (player.hasMetadata(metaKey)) {
                    player.removeMetadata(metaKey, plugin)
                } else {
                    plugin.scheduler.on(player)
                        .onRetired { session.accept(pick) }
                        .runLater(1) { session.accept(pick) }
                }
            }
        }

        menu.open(player)
    }

    private fun rerollItem(priceDisplay: String): ItemStack =
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

    private fun reroll(session: OpenSession, pick: OpenPick, price: ConfiguredPrice) {
        val player = session.player
        val canAfford = price.canAfford(player)

        player.setMetadata(metaKey, plugin.metadataValueFactory.create(true))
        player.closeInventory()

        val started = canAfford && session.reroll(pick)

        if (started) {
            price.pay(player)
        } else {
            session.accept(pick)
        }
    }
}
