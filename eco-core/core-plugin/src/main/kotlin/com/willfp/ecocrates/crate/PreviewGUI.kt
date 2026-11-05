package com.willfp.ecocrates.crate

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.gui.GUIComponent
import com.willfp.eco.core.gui.addCustomSlots
import com.willfp.eco.core.gui.addPage
import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.eco.util.StringUtils
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.reward.Rewards
import org.bukkit.entity.Player

/**
 * Builds the paged, read-only reward preview menu from a `preview:` config
 * block. Shared by crates and pouches.
 */
object PreviewGUI {
    fun build(config: Config, source: RewardSource): Menu = menu(config.getInt("preview.rows")) {
        val pages = config.getSubsections("preview.pages")
        val hideUnobtainable = config.getBool("preview.hide-unobtainable")

        title = StringUtils.format(config.getString("preview.title"))

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

                addCustomSlots(config, "preview")
                addCustomSlots(page, "")
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
