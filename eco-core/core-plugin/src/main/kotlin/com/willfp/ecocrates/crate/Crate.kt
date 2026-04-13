package com.willfp.ecocrates.crate

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.gui.addPage
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.MenuLayer
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.particle.Particles
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.eco.util.NumberUtils
import com.willfp.ecocrates.crate.placed.HologramFrame
import com.willfp.ecocrates.crate.placed.particle.ParticleAnimations
import com.willfp.ecocrates.crate.placed.particle.ParticleData
import com.willfp.ecocrates.crate.reroll.ReRollGUI
import com.willfp.ecocrates.crate.roll.Roll
import com.willfp.ecocrates.crate.roll.RollOptions
import com.willfp.ecocrates.crate.roll.Rolls
import com.willfp.ecocrates.event.CrateOpenEvent
import com.willfp.ecocrates.event.CrateRewardEvent
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.Rewards
import com.willfp.libreforge.NamedValue
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.effects.executors.impl.NormalExecutorFactory
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.TriggerData
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.util.Vector
import java.util.Objects
import java.util.UUID

class Crate(
    override val id: String,
    private val config: Config
) : KRegistrable {
    private val openEffects = Effects.compileChain(
        config.getSubsections("open-effects"),
        NormalExecutorFactory.create(),
        ViolationContext(plugin, "Crate $id Opening Effects")
    )
    private val finishEffects = Effects.compileChain(
        config.getSubsections("finish-effects"),
        NormalExecutorFactory.create(),
        ViolationContext(plugin, "Crate $id Finish Effects")
    )

    val name = config.getFormattedString("name")

    val hologramFrames = config.getSubsections("placed.hologram.frames")
        .map { HologramFrame(it.getInt("tick"), it.getFormattedStrings("lines")) }

    val hologramTicks = config.getInt("placed.hologram.ticks")

    val hologramHeight = config.getDouble("placed.hologram.height")

    val isShowingRandomReward = config.getBool("placed.random-reward.enabled")

    val randomRewardHeight = config.getDouble("placed.random-reward.height")

    val randomRewardDelay = config.getInt("placed.random-reward.delay")

    val randomRewardName = config.getFormattedString("placed.random-reward.name")

    val particles = config.getSubsections("placed.particles").map {
        ParticleData(
            Particles.lookup(it.getString("particle")),
            ParticleAnimations.get(it.getString("animation")) ?: ParticleAnimations.SPIRAL
        )
    }

    // The ID of the shared key this crate uses, defined in keys/ folder
    val sharedKey: SharedKey = Keys[config.getString("key")]
        ?: throw IllegalStateException("Crate '$id' references unknown key '${config.getString("key")}' - make sure a matching file exists in the keys/ folder")

    val rewards = config.getStrings("rewards").mapNotNull { Rewards.getByID(it) }

    val permission: Permission =
        Bukkit.getPluginManager().getPermission("ecocrates.open.$id") ?: Permission(
            "ecocrates.open.$id",
            "Allows opening the $id crate",
            PermissionDefault.TRUE
        ).apply {
            Bukkit.getPluginManager().getPermission("ecocrates.open.*")?.let { addParent(it, true) }
            Bukkit.getPluginManager().addPermission(this)
        }

    val canReroll = config.getBool("can-reroll")

    val rerollPermission: Permission =
        Bukkit.getPluginManager().getPermission("ecocrates.reroll.$id") ?: Permission(
            "ecocrates.reroll.$id",
            "Allows rerolling the $id crate",
            PermissionDefault.TRUE
        ).apply {
            Bukkit.getPluginManager().getPermission("ecocrates.reroll.*")?.let { addParent(it, true) }
            Bukkit.getPluginManager().addPermission(this)
        }

    val canPayToOpen = config.getBool("pay-to-open.enabled")

    val priceToOpen = config.getDouble("pay-to-open.price")

    val currencyType = config.getString("pay-to-open.type")

    private val opensKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_opens"),
        PersistentDataKeyType.INT,
        0
    )

    private val rollFactory = Rolls.get(config.getString("roll"))!!

    private val previewGUI = menu(config.getInt("preview.rows")) {
        title = config.getFormattedString("preview.title")

        val sharedCustomSlots = config.getSubsections("preview.custom-slots")
        val pages = config.getSubsections("preview.pages")

        maxPages(pages.size)

        val forwardsArrow = PageChanger(
            Items.lookup(config.getString("preview.forwards-arrow.item")).item,
            PageChanger.Direction.FORWARDS
        )

        val backwardsArrow = PageChanger(
            Items.lookup(config.getString("preview.backwards-arrow.item")).item,
            PageChanger.Direction.BACKWARDS
        )

        addComponent(
            MenuLayer.TOP,
            config.getInt("preview.forwards-arrow.row"),
            config.getInt("preview.forwards-arrow.column"),
            forwardsArrow
        )

        addComponent(
            MenuLayer.TOP,
            config.getInt("preview.backwards-arrow.row"),
            config.getInt("preview.backwards-arrow.column"),
            backwardsArrow
        )

        for (page in pages) {
            addPage(page.getInt("page")) {
                setMask(
                    FillerMask(
                        MaskItems.fromItemNames(page.getStrings("mask.items")),
                        *page.getStrings("mask.pattern").toTypedArray()
                    )
                )

                for (previewReward in page.getSubsections("rewards")) {
                    val reward = Rewards[previewReward.getString("id")] ?: continue
                    val row = previewReward.getInt("row")
                    val column = previewReward.getInt("column")

                    setSlot(
                        row,
                        column,
                        slot(reward.getDisplay()) {
                            setUpdater { player, _, _ -> reward.getDisplay(player, this@Crate) }
                        }
                    )
                }

                for (config in sharedCustomSlots) {
                    setSlot(
                        config.getInt("row"),
                        config.getInt("column"),
                        ConfigSlot(config)
                    )
                }

                for (config in page.getSubsections("custom-slots")) {
                    setSlot(
                        config.getInt("row"),
                        config.getInt("column"),
                        ConfigSlot(config)
                    )
                }
            }
        }
    }



    init {
        PlayerPlaceholder(
            plugin,
            "${id}_keys",
        ) { sharedKey.getVirtualKeys(it).toString() }.register()

        PlayerPlaceholder(
            plugin,
            "${id}_opens",
        ) { getOpens(it).toString() }.register()
    }

    private fun makeRoll(
        player: Player,
        location: Location,
        reward: Reward,
        method: OpenMethod,
        isReroll: Boolean = false
    ): Roll {
        val display = mutableListOf<Reward>()

        // Add three to the scroll times so that it lines up
        repeat(35 + 4) {
            display.add(getRandomReward(player)) // Fill roll with display weight items
        }

        return rollFactory.create(
            RollOptions(
                reward,
                this,
                player,
                location,
                isReroll,
                method
            )
        )
    }

    private fun hasRanOutOfRewardsAndNotify(player: Player): Boolean {
        val ranOut = rewards.all { it.getWeight(player) <= 0 }

        if (ranOut) {
            player.sendMessage(plugin.langYml.getMessage("all-rewards-used"))
        }

        return ranOut
    }

    private fun getRandomReward(player: Player): Reward {
        val weighted = rewards.map { it to it.getEffectiveWeight(player) }
        val totalWeight = weighted.sumOf { it.second }

        if (totalWeight <= 0.0) {
            return rewards.random()
        }
        val roll = NumberUtils.randFloat(0.0, totalWeight)
        var cum = 0.0

        for ((reward, weight) in weighted) {
            cum += weight

            if (roll < cum) {
                return reward
            }
        }
        return weighted.last().first
    }

    private fun canOpenAndNotify(player: Player, method: OpenMethod): Boolean {
        if (!canPayToOpen && method == OpenMethod.MONEY) {
            return canOpenAndNotify(player, OpenMethod.VIRTUAL_KEY)
        }

        return method.canUseAndNotify(this, player)
    }

    private fun hasPermissionAndNotify(player: Player): Boolean {
        val hasPermission = player.hasPermission(permission)

        if (!hasPermission) {
            player.sendMessage(plugin.langYml.getMessage("no-crate-permission").replace("%crate%", this.name))
        }

        return hasPermission
    }


    fun getRandomRewards(player: Player, amount: Int): List<Reward> {
        return List(amount.coerceAtLeast(0)) { getRandomReward(player) }
    }

    fun openPlaced(player: Player, location: Location, method: OpenMethod) {
        val nicerLocation = location.clone().add(0.5, 1.5, 0.5)

        if (!canOpenAndNotify(player, method)) {
            val vector = player.location.clone().subtract(nicerLocation.toVector())
                .toVector()
                .normalize()
                .add(Vector(0.0, 1.0, 0.0))
                .multiply(plugin.configYml.getDouble("no-key-velocity"))

            player.velocity = vector

            return
        }

        openWithMethod(player, method, nicerLocation)
    }

    fun openWithMethod(player: Player, method: OpenMethod, location: Location? = null) {
        if (!canOpenAndNotify(player, method)) {
            return
        }

        // Goes here rather than open() to keep force opening working
        if (!hasPermissionAndNotify(player)) {
            return
        }

        if (open(player, method, location = location)) {
            method.useMethod(this, player)
        }
    }

    fun open(
        player: Player,
        method: OpenMethod,
        location: Location? = null,
        isReroll: Boolean = false
    ): Boolean {
        /* Prevent server crashes */
        if (hasRanOutOfRewardsAndNotify(player)) {
            return false
        }

        if (player.isOpeningCrate) {
            return false
        }

        val loc = location ?: player.eyeLocation

        val event = CrateOpenEvent(player, this, method, getRandomReward(player), isReroll)
        Bukkit.getPluginManager().callEvent(event)

        if (!isReroll) {
            openEffects?.trigger(
                TriggerData(
                    player = player,
                    location = loc
                ).dispatch(player.toDispatcher())
                    .apply {
                        addPlaceholders(
                            listOf(
                                NamedValue("crate", name),
                                NamedValue("crate_id", id)
                            )
                        )
                    }
            )
        }

        val roll = makeRoll(player, loc, event.reward, method, isReroll = isReroll)
        var tick = 0
        var hasFinalized = false

        fun finalizeRoll(forceFinish: Boolean) {
            if (hasFinalized) {
                return
            }

            hasFinalized = true

            roll.onFinish()
            player.isOpeningCrate = false

            if (forceFinish || !canReroll(player) || roll.isReroll) {
                handleFinish(roll)
            } else {
                ReRollGUI.open(roll)
            }
        }

        val task = plugin.runnableFactory.create {
            roll.tick(tick)

            tick++

            if (!roll.shouldContinueTicking(tick) || !player.isOpeningCrate) {
                it.cancel()
                finalizeRoll(false)
            }
        }.runTaskTimer(1, 1)

        player.isOpeningCrate = true
        player.profile.write(opensKey, getOpens(player) + 1)
        roll.roll()

        return true
    }

    fun previewForPlayer(player: Player) {
        previewGUI.open(player)
    }

    fun handleFinish(roll: Roll) {
        val player = roll.player

        val event = CrateRewardEvent(player, this, roll.reward)
        Bukkit.getPluginManager().callEvent(event)

        finishEffects?.trigger(
            TriggerData(player = player)
                .dispatch(player.toDispatcher())
                .apply {
                    addPlaceholders(
                        listOf(
                            NamedValue("crate", name),
                            NamedValue("crate_id", id),
                            NamedValue("reward", roll.reward.name),
                            NamedValue("reward_id", roll.reward.id)
                        )
                    )
                }
        )

        event.reward.giveTo(player, this)
    }

    fun canReroll(player: Player): Boolean {
        if (!canReroll) {
            return false
        }

        return player.hasPermission(rerollPermission)
    }

    fun adjustVirtualKeys(player: OfflinePlayer, amount: Int) {
        sharedKey.adjustVirtualKeys(player, amount)
    }

    fun getVirtualKeys(player: OfflinePlayer): Int {
        return sharedKey.getVirtualKeys(player)
    }

    fun hasPhysicalKey(player: Player): Boolean {
        return sharedKey.matches(player.inventory.itemInMainHand)
    }

    fun getKeysToGet(player: OfflinePlayer): Int {
        return sharedKey.getKeysToGet(player)
    }

    fun setKeysToGet(player: OfflinePlayer, amount: Int) {
        sharedKey.setKeysToGet(player, amount)
    }

    fun adjustKeysToGet(player: OfflinePlayer, amount: Int) {
        this.setKeysToGet(player, this.getKeysToGet(player) + amount)
    }

    fun hasVirtualKey(player: Player): Boolean {
        return sharedKey.getVirtualKeys(player) > 0
    }

    fun getOpens(player: OfflinePlayer): Int {
        return player.profile.read(opensKey)
    }

    fun usePhysicalKey(player: Player) {
        val itemStack = player.inventory.itemInMainHand
        if (sharedKey.matches(itemStack)) {
            itemStack.amount -= 1
            if (itemStack.amount == 0) {
                itemStack.type = Material.AIR
            }
        }
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

    override fun toString(): String {
        return "Crate{id=$id}"
    }

    override fun getID(): String {
        return id
    }
}

private val openingCrates = mutableSetOf<UUID>()

var Player.isOpeningCrate: Boolean
    get() = openingCrates.contains(this.uniqueId)
    set(value) {
        if (value) {
            openingCrates.add(this.uniqueId)
        } else {
            openingCrates.remove(this.uniqueId)
        }
    }
