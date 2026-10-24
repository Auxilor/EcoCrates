package com.willfp.ecocrates.crate

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.gui.addCustomSlots
import com.willfp.eco.core.gui.addPage
import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.eco.util.StringUtils
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.reward.Rewards

/**
 * Builds the paged, read-only reward preview menu from a `preview:` config
 * block. Shared by crates and pouches.
 */
object PreviewGUI {
    fun build(config: Config, source: RewardSource): Menu = menu(config.getInt("preview.rows")) {
        val pages = config.getSubsections("preview.pages")

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

                    setSlot(
                        row,
                        column,
                        slot(reward.getDisplay()) {
                            setUpdater { player, _, _ -> reward.getDisplay(player, source) }
                        }
                    )
                }

                addCustomSlots(config, "preview")
                addCustomSlots(page, "")
            }
        }
    }
}
