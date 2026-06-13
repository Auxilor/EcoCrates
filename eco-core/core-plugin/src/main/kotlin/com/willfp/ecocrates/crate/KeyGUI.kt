package com.willfp.ecocrates.crate

import com.willfp.eco.core.gui.addPage
import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.ecocrates.plugin
import org.bukkit.entity.Player

object KeyGUI {
    private lateinit var menu: Menu

    @JvmStatic
    fun update() {
        val maskItems = MaskItems.fromItemNames(plugin.configYml.getStrings("keygui.mask.items"))
        val maskPattern = plugin.configYml.getStrings("keygui.mask.pattern").toTypedArray()

        val customSlots = plugin.configYml.getSubsections("keygui.custom-slots")

        val keysByPage = Keys.values()
            .filter { it.keyGuiEnabled }
            .groupBy { it.keyGuiPage }

        val maxPage = (keysByPage.keys.maxOrNull() ?: 1).coerceAtLeast(1)

        val pageChangeSound = PlayableSound.create(plugin.configYml.getSubsection("keygui.page-change-sound"))

        menu = menu(plugin.configYml.getInt("keygui.rows")) {
            title = plugin.configYml.getFormattedString("keygui.title")

            maxPages(maxPage)

            addPageChanger(plugin.configYml, "keygui.forwards-arrow", PageChanger.Direction.FORWARDS, pageChangeSound)
            addPageChanger(plugin.configYml, "keygui.backwards-arrow", PageChanger.Direction.BACKWARDS, pageChangeSound)

            for (page in 1..maxPage) {
                addPage(page) {
                    setMask(FillerMask(maskItems, *maskPattern))

                    for (config in customSlots) {
                        setSlot(
                            config.getInt("row"),
                            config.getInt("column"),
                            ConfigSlot(config)
                        )
                    }

                    for (key in keysByPage[page].orEmpty()) {
                        key.addToKeyGUI(this)
                    }
                }
            }
        }
    }

    fun open(player: Player) {
        menu.open(player)
    }
}
