package com.willfp.ecocrates.crate

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.MenuBuilder
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.util.formatEco
import com.willfp.ecocrates.crate.placed.particle.ParticleAnimations
import com.willfp.ecocrates.crate.placed.particle.ParticleData
import com.willfp.ecocrates.crate.roll.Roll
import com.willfp.ecocrates.crate.roll.RollOptions
import com.willfp.ecocrates.crate.roll.Rolls
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.util.ConfiguredSound
import com.willfp.ecocrates.util.PlayableSound
import org.bukkit.OfflinePlayer
import org.bukkit.Particle
import org.bukkit.entity.Player

class Crate(
    private val config: Config,
    private val plugin: EcoPlugin
) {
    val id = config.getString("id")

    val name = config.getFormattedString("name")

    val hologramLines = config.getFormattedStrings("placed.hologram.lines")

    val hologramHeight = config.getDouble("placed.hologram.height")

    val particles = config.getSubsections("placed.particles").map {
        ParticleData(
            Particle.valueOf(it.getString("particle").uppercase()),
            ParticleAnimations.getByID(it.getString("animation")) ?: ParticleAnimations.SPIRAL
        )
    }

    private val keysKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_keys"),
        PersistentDataKeyType.INT,
        0
    ).player()

    private val rollFactory = Rolls.getByID(config.getString("roll"))!!

    private val rewards = config.getSubsections("rewards").map { Reward(it) }

    private val previewGUI = menu(config.getInt("preview.rows")) {
        setMask(
            FillerMask(
                MaskItems.fromItemNames(config.getStrings("preview.mask.items")),
                *config.getStrings("preview.mask.pattern").toTypedArray()
            )
        )

        setTitle(config.getFormattedString("preview.title"))

        for (reward in rewards) {
            setSlot(
                reward.displayRow,
                reward.displayColumn,
                slot(reward.display) {

                }
            )
        }
    }

    private val onFinishSound = PlayableSound(
        config.getSubsections("onFinish.sounds")
            .map { ConfiguredSound.fromConfig(it) }
    )

    init {
        PlayerPlaceholder(
            plugin,
            "${id}_keys",
        ) { getKeys(it).toString() }.register()
    }

    private fun makeRoll(player: Player): Roll {
        val display = mutableListOf<Reward>()

        // Add three to the scroll times so that it lines up
        for (i in 0..(35 + 3)) {
            display.add(getRandomReward(displayWeight = true)) // Fill roll with display weight items
        }

        return rollFactory.create(
            RollOptions(
                getRandomReward(),
                this,
                this.plugin,
                player
            )
        )
    }

    private fun getRandomReward(displayWeight: Boolean = false): Reward {
        var weight = 100.0
        val selection = rewards.toList().shuffled()

        lateinit var current: Reward
        for (i in 0..Int.MAX_VALUE) {
            current = selection[i % selection.size]
            weight -= if (displayWeight) current.displayWeight else current.weight
            if (weight <= 0) {
                break
            }
        }

        return current
    }

    internal fun addToKeyGUI(builder: MenuBuilder) {
        builder.setSlot(
            config.getInt("keygui.row"),
            config.getInt("keygui.column"),
            slot(
                ItemStackBuilder(Items.lookup(config.getString("keygui.item"))).build()
            ) {
                onLeftClick { event, _, _ ->
                    val player = event.whoClicked as Player
                    openWithKey(player)
                }

                onRightClick { event, _, _ ->
                    event.whoClicked.closeInventory()
                    config.getFormattedStrings("keygui.rightClickMessage")
                        .forEach { event.whoClicked.sendMessage(it) }
                }

                setUpdater { player, _, previous ->
                    previous.apply {
                        itemMeta = itemMeta?.apply {
                            lore = config.getStrings("keygui.lore")
                                .map { it.replace("%keys%", getKeys(player).toString()) }
                                .map { it.formatEco(player) }
                        }
                    }
                    previous
                }
            }
        )
    }

    fun getRandomRewards(amount: Int, displayWeight: Boolean = false): List<Reward> {
        return (0..amount).map { getRandomReward(displayWeight) }
    }

    fun openWithKey(player: Player) {
        val keys = player.profile.read(keysKey)

        if (keys == 0) {
            player.sendMessage(plugin.langYml.getMessage("not-enough-keys").replace("%crate%", this.name))
            return
        }

        player.profile.write(keysKey, keys - 1)
        open(player)
    }

    fun open(player: Player) {
        makeRoll(player).roll()
    }

    fun previewForPlayer(player: Player) {
        previewGUI.open(player)
    }

    fun handleFinish(player: Player, roll: Roll) {
        roll.reward.giveTo(player)
        onFinishSound.play(player.location)
    }

    fun giveKeys(player: OfflinePlayer, amount: Int) {
        player.profile.write(keysKey, player.profile.read(keysKey) + amount)
    }

    fun getKeys(player: OfflinePlayer): Int {
        return player.profile.read(keysKey)
    }
}
