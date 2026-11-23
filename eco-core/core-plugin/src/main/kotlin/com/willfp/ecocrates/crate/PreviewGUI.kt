package com.willfp.ecocrates.crate

import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.BonusPlaceholders
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.reward.Rewards
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import wiki.ecohub.config.Config
import wiki.ecohub.game.item.FastItemStack
import wiki.ecohub.game.sound.PlayableSound
import wiki.ecohub.modules.menus.addCustomSlots
import wiki.ecohub.modules.menus.addPage
import wiki.ecohub.modules.menus.addPageChanger
import wiki.ecohub.modules.menus.component.GUIComponent
import wiki.ecohub.modules.menus.getSlotLocations
import wiki.ecohub.modules.menus.menu
import wiki.ecohub.modules.menus.menu.Menu
import wiki.ecohub.modules.menus.page.PageBuilder
import wiki.ecohub.modules.menus.page.PageChanger
import wiki.ecohub.modules.menus.setMask
import wiki.ecohub.modules.menus.slot
import wiki.ecohub.modules.menus.slot.ConfigSlot
import wiki.ecohub.modules.menus.slot.Slot
import wiki.ecohub.ui.text.StringUtils

/**
 * Builds the paged, read-only reward preview menu from a `preview:` config
 * block. Shared by crates and pouches.
 */
object PreviewGUI {
    fun build(config: Config, source: RewardSource): Menu = menu(config.getInt("preview.rows")) {
        val pages = config.getSubsections("preview.pages")
        val hideUnobtainable = config.getBool("preview.hide-unobtainable")

        setTitle(BonusPlaceholders.replaceMaxRewards(StringUtils.format(config.getString("preview.title")), source))

        maxPages(pages.size)

        val pageChangeSound = PlayableSound.create(config.getSubsection("preview.page-change-sound"))

        addPageChanger(config, "preview.forwards-arrow", PageChanger.Direction.FORWARDS, pageChangeSound)
        addPageChanger(config, "preview.backwards-arrow", PageChanger.Direction.BACKWARDS, pageChangeSound)

        for (page in pages) {
            addPage(page.getInt("page")) {
                setMask(page)

                for (previewReward in page.getSubsections("rewards")) {
                    val rewardId = previewReward.getString("id")
                    val reward = Rewards[rewardId]

                    if (reward == null) {
                        plugin.logger.warning("${source.sourceType.replaceFirstChar { it.uppercase() }} '${source.id}' preview references unknown reward '$rewardId'")
                        continue
                    }

                    val row = previewReward.getInt("row")
                    val column = previewReward.getInt("column")

                    val rewardSlot = slot(reward.getDisplay()) {
                        setUpdater { player, _, _ -> reward.getDisplay(player, source) }
                    }

                    addComponent(
                        row,
                        column,
                        if (hideUnobtainable) ObtainableRewardSlot(reward, source, rewardSlot) else rewardSlot
                    )
                }

                addBonusCustomSlots(config, "preview", source)
                addBonusCustomSlots(page, "", source)
            }
        }
    }
}

/**
 * A reward slot that gives way to the layers below it, such as the mask,
 * for a player with no chance of winning the reward.
 */
private class ObtainableRewardSlot(
    private val reward: Reward,
    private val source: RewardSource,
    private val rewardSlot: Slot
) : GUIComponent {
    override fun getRows() = 1

    override fun getColumns() = 1

    override fun getSlotAt(row: Int, column: Int): Slot = rewardSlot

    override fun getSlotAt(row: Int, column: Int, player: Player, menu: Menu): Slot? =
        rewardSlot.takeIf { reward.isObtainable(player, source) }
}

private fun PageBuilder.addBonusCustomSlots(config: Config, path: String, source: RewardSource) {
    val prefix = if (path.isEmpty()) "" else "$path."

    for (slotConfig in config.getSubsections("${prefix}custom-slots")) {
        val slot = BonusPlaceholderSlot(ConfigSlot(slotConfig), source)

        for ((row, column) in slotConfig.getSlotLocations()) {
            addComponent(row, column, slot)
        }
    }
}

private class BonusPlaceholderSlot(private val inner: Slot, private val source: RewardSource) : Slot {
    override fun getItemStack(player: Player): ItemStack {
        val item = inner.getItemStack(player)
        val meta = item.itemMeta ?: return item
        val fast = FastItemStack.wrap(item.clone())
        val hasName = meta.hasDisplayName() && BonusPlaceholders.hasPlaceholder(fast.getDisplayName())
        val hasLore = fast.getLore().any { BonusPlaceholders.hasPlaceholder(it) }

        if (hasName) {
            fast.setDisplayName(BonusPlaceholders.replace(fast.getDisplayName(), source, player))
        }

        if (hasLore) {
            fast.setLore(fast.getLore().map { BonusPlaceholders.replace(it, source, player) })
        }

        return if (hasName || hasLore) fast.unwrap() else item
    }

    override fun getActionableSlot(player: Player, menu: Menu): Slot = inner.getActionableSlot(player, menu)
}
