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
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.util.formatEco
import com.willfp.ecocrates.crate.placed.particle.ParticleAnimations
import com.willfp.ecocrates.crate.placed.particle.ParticleData
import com.willfp.ecocrates.crate.roll.Roll
import com.willfp.ecocrates.crate.roll.RollOptions
import com.willfp.ecocrates.crate.roll.Rolls
import com.willfp.ecocrates.event.CrateOpenEvent
import com.willfp.ecocrates.event.CrateRewardEvent
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.util.ConfiguredFirework
import com.willfp.ecocrates.util.ConfiguredSound
import com.willfp.ecocrates.util.PlayableSound
import org.bukkit.*
import org.bukkit.entity.Player
import java.util.*

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

    val key = CustomItem(
        plugin.namespacedKeyFactory.create(id),
        { it.getAsKey() == this },
        Items.lookup(config.getString("key.item")).item
            .clone().apply { setAsKeyFor(this@Crate) }
    ).apply { register() }

    val keyLore = config.getFormattedStrings("key.lore")

    private val keysKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_keys"),
        PersistentDataKeyType.INT,
        0
    ).player()

    private val rollFactory = Rolls.getByID(config.getString("roll"))!!

    private val rewards = config.getSubsections("rewards").map { Reward(plugin, it) }

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

    private val finishSound = PlayableSound(
        config.getSubsections("finish.sounds")
            .map { ConfiguredSound.fromConfig(it) }
    )

    private val finishFireworks = config.getSubsections("finish.fireworks")
        .map { ConfiguredFirework.fromConfig(it) }

    init {
        PlayerPlaceholder(
            plugin,
            "${id}_keys",
        ) { getKeys(it).toString() }.register()
    }

    private fun makeRoll(player: Player, location: Location, reward: Reward): Roll {
        val display = mutableListOf<Reward>()

        // Add three to the scroll times so that it lines up
        for (i in 0..(35 + 3)) {
            display.add(getRandomReward(displayWeight = true)) // Fill roll with display weight items
        }

        return rollFactory.create(
            RollOptions(
                reward,
                this,
                this.plugin,
                player,
                location
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

    private fun hasKeysAndNotify(player: Player, physicalKey: Boolean = false): Boolean {
        if (getKeys(player) == 0) {
            return if (!physicalKey) {
                player.sendMessage(plugin.langYml.getMessage("not-enough-keys").replace("%crate%", this.name))
                false
            } else {
                val physical = hasPhysicalKey(player)
                if (!physical) {
                    player.sendMessage(plugin.langYml.getMessage("not-enough-keys").replace("%crate%", this.name))
                }

                physical
            }
        }

        return true
    }

    internal fun addToKeyGUI(builder: MenuBuilder) {
        builder.setSlot(
            config.getInt("keygui.row"),
            config.getInt("keygui.column"),
            slot(
                ItemStackBuilder(Items.lookup(config.getString("keygui.item"))).build()
            ) {
                onLeftClick { event, _, _ ->
                    if (config.getBool("keygui.left-click-opens")) {
                        val player = event.whoClicked as Player
                        openWithKey(player)
                    }
                }

                onRightClick { event, _, _ ->
                    event.whoClicked.closeInventory()
                    config.getFormattedStrings("keygui.right-click-message")
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

    fun openPhysical(player: Player, location: Location, physicalKey: Boolean) {
        val nicerLocation = location.clone().add(0.5, 1.5, 0.5)

        if (!hasKeysAndNotify(player, physicalKey = physicalKey)) {
            val vector = player.location.clone().subtract(nicerLocation.toVector())
                .add(0.0, 1.5, 0.0)
                .toVector()
                .normalize()
                .multiply(plugin.configYml.getDouble("no-key-velocity"))

            player.velocity = vector

            return
        }

        openWithKey(player, nicerLocation, physicalKey)
    }

    fun openWithKey(player: Player, location: Location? = null, physicalKey: Boolean = false) {
        if (!hasKeysAndNotify(player, physicalKey = true)) {
            return
        }

        if (physicalKey) {
            usePhysicalKey(player)
        } else {
            adjustKeys(player, -1)
        }

        open(player, location, physicalKey)
    }

    fun open(player: Player, location: Location? = null, physicalKey: Boolean = false) {
        val event = CrateOpenEvent(player, this, physicalKey, getRandomReward())
        Bukkit.getPluginManager().callEvent(event)

        val roll = makeRoll(player, location ?: player.location, event.reward)
        roll.roll()
    }

    fun previewForPlayer(player: Player) {
        previewGUI.open(player)
    }

    fun handleFinish(player: Player, roll: Roll, location: Location) {
        val event = CrateRewardEvent(player, this, roll.reward)
        Bukkit.getPluginManager().callEvent(event)

        event.reward.giveTo(player)
        finishSound.play(location)
        finishFireworks.forEach { it.launch(location) }
    }

    fun adjustKeys(player: OfflinePlayer, amount: Int) {
        player.profile.write(keysKey, player.profile.read(keysKey) + amount)
    }

    fun getKeys(player: OfflinePlayer): Int {
        return player.profile.read(keysKey)
    }

    fun usePhysicalKey(player: Player) {
        val itemStack = player.inventory.itemInMainHand
        if (key.matches(itemStack)) {
            itemStack.amount -= 1
            if (itemStack.amount == 0) {
                itemStack.type = Material.AIR
            }
        }
    }

    fun hasPhysicalKey(player: Player): Boolean {
        return key.matches(player.inventory.itemInMainHand)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Crate) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }
}
