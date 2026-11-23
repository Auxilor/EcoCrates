package com.willfp.ecocrates.crate

import com.willfp.ecocrates.plugin
import org.bukkit.entity.Player
import wiki.ecohub.game.sound.PlayableSound
import wiki.ecohub.modules.menus.addCustomSlots
import wiki.ecohub.modules.menus.addPage
import wiki.ecohub.modules.menus.addPageChanger
import wiki.ecohub.modules.menus.menu
import wiki.ecohub.modules.menus.menu.Menu
import wiki.ecohub.modules.menus.page.PageChanger
import wiki.ecohub.modules.menus.setMask

object KeyGUI {
    @Volatile
    private lateinit var menu: Menu

    @JvmStatic
    fun update() {
        val keysByPage = Keys.values()
            .filter { it.keyGuiEnabled }
            .groupBy { it.keyGuiPage }

        val maxPage = (keysByPage.keys.maxOrNull() ?: 1).coerceAtLeast(1)

        val pageChangeSound = PlayableSound.create(plugin.configYml.getSubsection("keygui.page-change-sound"))

        menu = menu(plugin.configYml.getInt("keygui.rows")) {
            setTitle(plugin.configYml.getFormattedString("keygui.title"))

            maxPages(maxPage)

            addPageChanger(plugin.configYml, "keygui.forwards-arrow", PageChanger.Direction.FORWARDS, pageChangeSound)
            addPageChanger(plugin.configYml, "keygui.backwards-arrow", PageChanger.Direction.BACKWARDS, pageChangeSound)

            for (page in 1..maxPage) {
                addPage(page) {
                    setMask(plugin.configYml, "keygui")

                    addCustomSlots(plugin.configYml, "keygui")

                    for (key in keysByPage[page].orEmpty()) {
                        key.addToKeyGUI(this)
                    }
                }
            }
        }
    }

    fun open(player: Player) {
        if (!::menu.isInitialized) {
            update()
        }

        menu.open(player)
    }
}
