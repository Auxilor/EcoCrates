package com.willfp.ecocrates.crate.reroll

import com.willfp.eco.core.gui.SlotLocation
import com.willfp.eco.core.gui.getSlotLocations
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.MenuBuilder
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.price.ConfiguredPrice
import com.willfp.ecocrates.crate.open.OpenPick
import com.willfp.ecocrates.crate.open.OpenSession
import com.willfp.ecocrates.plugin
import org.bukkit.inventory.ItemStack
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The menu offered after a roll that showed several rewards: reroll any one rerollable reward of
 * the batch, or accept them all.
 *
 * Each open of the menu acts once: the first of accept, reroll or close wins and the others do nothing.
 * The flag is set before closing because onClose runs inside closeInventory, and the menu closes so the
 * reroll animation plays without it. After a reroll the session opens this menu again for the same batch.
 */
internal object MultiReRollGUI {
    private const val metaKey = "ecocrates-multi-reroll-fix"

    /** Opens the menu for [picks], or gives them all if none can be rerolled. */
    fun open(session: OpenSession, picks: List<OpenPick>) {
        if (picks.none { session.sessionSource.canReroll(session.player, it) }) {
            session.acceptAll(picks)
        } else {
            buildMenu(session, picks).open(session.player)
        }
    }

    private fun buildMenu(session: OpenSession, picks: List<OpenPick>) =
        menu(plugin.configYml.getInt("reroll.multi.rows")) {
            val player = session.player
            val isHandled = AtomicBoolean(false)

            setMask(plugin.configYml, "reroll.multi")

            title = plugin.configYml.getFormattedString("reroll.multi.title")

            placeRewards(session, picks, isHandled)

            for (location in plugin.configYml.getSlotLocations("reroll.multi.accept")) {
                setSlot(
                    location.row,
                    location.column,
                    slot(Items.lookup(plugin.configYml.getString("reroll.multi.accept.item")).item) {
                        onLeftClick { _, _, _ ->
                            if (isHandled.compareAndSet(false, true)) {
                                player.setMetadata(metaKey, plugin.metadataValueFactory.create(true))
                                player.closeInventory()
                                session.acceptAll(picks)
                            }
                        }
                    }
                )
            }

            onClose { _, _ ->
                if (player.hasMetadata(metaKey)) {
                    player.removeMetadata(metaKey, plugin)
                } else if (isHandled.compareAndSet(false, true)) {
                    plugin.scheduler.on(player)
                        .onRetired { session.acceptAll(picks) }
                        .runLater(1) { session.acceptAll(picks) }
                }
            }
        }

    private fun MenuBuilder.placeRewards(session: OpenSession, picks: List<OpenPick>, isHandled: AtomicBoolean) {
        val locations = plugin.configYml.getSubsections("reroll.multi.rewards")
            .map { SlotLocation(row = it.getInt("row"), column = it.getInt("column")) }

        if (picks.size > locations.size) {
            plugin.logger.warning(
                "Crate '${session.source.id}' shows ${picks.size} rewards but reroll.multi.rewards " +
                    "has only ${locations.size} slots; the rest are still given on accept."
            )
        }

        for ((location, pick) in locations.zip(picks)) {
            val price = session.sessionSource.rerollPrice(pick)
                .takeIf { session.sessionSource.canReroll(session.player, pick) }

            setSlot(
                location.row,
                location.column,
                slot(rewardItem(session, pick, price)) {
                    onLeftClick { _, _, _ ->
                        if (price != null && isHandled.compareAndSet(false, true)) {
                            reroll(session, picks, pick, price)
                        }
                    }
                }
            )
        }
    }

    private fun rewardItem(session: OpenSession, pick: OpenPick, price: ConfiguredPrice?): ItemStack {
        val item = pick.reward.getDisplay(session.player, session.source)

        return if (price == null) {
            item
        } else {
            val priceDisplay = price.getDisplay(session.player)

            ItemStackBuilder(item)
                .addLoreLines(
                    plugin.configYml.getStrings("reroll.multi.reroll-lore")
                        .map { it.replace(oldValue = "%price%", newValue = priceDisplay) }
                )
                .build()
        }
    }

    private fun reroll(session: OpenSession, picks: List<OpenPick>, pick: OpenPick, price: ConfiguredPrice) {
        val player = session.player
        val canAfford = price.canAfford(player)

        player.setMetadata(metaKey, plugin.metadataValueFactory.create(true))
        player.closeInventory()

        val started = canAfford && session.reroll(pick, picks)

        if (started) {
            price.pay(player)
        } else {
            session.acceptAll(picks)
        }
    }
}
