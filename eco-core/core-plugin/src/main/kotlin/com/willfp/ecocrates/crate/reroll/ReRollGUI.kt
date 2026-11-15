package com.willfp.ecocrates.crate.reroll

import com.willfp.eco.core.gui.getSlotLocations
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.price.ConfiguredPrice
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.open.OpenPick
import com.willfp.ecocrates.crate.open.OpenSession
import com.willfp.ecocrates.crate.open.OpenSessions
import com.willfp.ecocrates.crate.roll.Roll
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

    /**
     * Offers the reroll menu for [roll], kept so addons built against the old menu still link.
     *
     * The open session now prices and counts rerolls per pick, so [rerollNumber] and [profile] are not
     * used. If [roll]'s player has an open of [crate] in progress with a pick of [roll]'s reward not yet
     * given, the menu opens for that pick. Otherwise the reward is given straight away.
     *
     * @param crate The crate the roll is for.
     * @param roll The finished roll.
     * @param rerollNumber How many times the roll has been rerolled.
     * @param profile The crate's reroll settings.
     */
    @Deprecated("Rerolls are offered per pick by the open session")
    @Suppress("UnusedParameter")
    fun open(crate: Crate, roll: Roll, rerollNumber: Int, profile: RerollProfile) {
        val session = OpenSessions.get(roll.player)?.takeIf { it.source === crate }
        val pick = session?.run { picks.firstOrNull { !it.isGiven && it.reward == roll.reward } }

        if (session != null && pick != null) {
            open(session, pick)
        } else {
            crate.handleFinish(roll)
        }
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
