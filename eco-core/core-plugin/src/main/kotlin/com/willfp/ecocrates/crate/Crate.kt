package com.willfp.ecocrates.crate

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.gui.addPage
import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.particle.Particles
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.eco.util.StringUtils
import com.willfp.ecocrates.crate.placed.HologramFrame
import com.willfp.ecocrates.crate.placed.PlacedCrate
import com.willfp.ecocrates.crate.placed.PlacedCrates
import com.willfp.ecocrates.crate.placed.particle.ParticleAnimations
import com.willfp.ecocrates.crate.placed.particle.ParticleData
import com.willfp.ecocrates.crate.reroll.ReRollGUI
import com.willfp.ecocrates.crate.reroll.RerollProfile
import com.willfp.ecocrates.crate.roll.Roll
import com.willfp.ecocrates.crate.roll.RollOptions
import com.willfp.ecocrates.crate.roll.Rolls
import com.willfp.ecocrates.event.CrateOpenEvent
import com.willfp.ecocrates.event.CrateRewardEvent
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.Rewards
import com.willfp.ecocrates.util.weightedRandom
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

/**
 * A configured crate type: its rewards, roll animation, preview GUI, reroll
 * settings, placed-crate display (hologram/particles/item), and the
 * libreforge effects run on open/finish.
 *
 * @param id The crate's config-file ID.
 * @param config The crate's parsed config section.
 */
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

    val isShiftRightClickOpenAllEnabled = config.getBool("placed.shift-right-click-open-all")

    val isOpenAllEffectsPerKey = config.getBool("placed.open-all-effects-per-key")

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

    private val rerollProfile = RerollProfile.fromCrateConfig(config) {
        plugin.logger.warning(
            "Crate '$id' uses the deprecated 'can-reroll' option. " +
                "Migrate to the 'rerolls:' block (see the _example.yml crate). " +
                "The old option still works for now but will be removed in a future release."
        )
    }

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

    // Whether to hide the placed crate's preview hologram/item from the player
    // while this crate's roll animation is playing, configured per roll type.
    private val hidesPlacedCrate = plugin.configYml
        .getBool("rolls.${rollFactory.id}.hide-placed-crate")

    private val previewGUI = menu(config.getInt("preview.rows")) {
        val sharedCustomSlots = config.getSubsections("preview.custom-slots")
        val pages = config.getSubsections("preview.pages")

        title = StringUtils.format(config.getString("preview.title"))

        maxPages(pages.size)

        val pageChangeSound = PlayableSound.create(config.getSubsection("preview.page-change-sound"))

        addPageChanger(config, "preview.forwards-arrow", PageChanger.Direction.FORWARDS, pageChangeSound)
        addPageChanger(config, "preview.backwards-arrow", PageChanger.Direction.BACKWARDS, pageChangeSound)

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
        isReroll: Boolean = false,
        placedCrate: PlacedCrate? = null
    ): Roll {
        val display = mutableListOf<Reward>()

        // Pad the scroll so it lines up
        repeat(35 + 4) {
            display.add(getRandomReward(player))
        }

        return rollFactory.create(
            RollOptions(
                reward,
                this,
                player,
                location,
                isReroll,
                method,
                placedCrate
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

    private fun getRandomReward(player: Player): Reward =
        rewards.weightedRandom { it.getEffectiveWeight(player) }
            ?: throw IllegalStateException("Crate '$id' has no rewards")

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


    /**
     * Rolls [amount] independent random rewards for [player] (each drawn using
     * that player's effective weights), without opening the crate.
     *
     * @return The rolled rewards, in no particular order.
     */
    fun getRandomRewards(player: Player, amount: Int): List<Reward> {
        return List(amount.coerceAtLeast(0)) { getRandomReward(player) }
    }

    /** Opens the placed crate at [location] for [player], pushing them away if they can't pay/afford it. */
    fun openPlaced(player: Player, location: Location, method: OpenMethod) {
        val nicerLocation = location.block.location.add(0.5, 1.5, 0.5)

        if (!canOpenAndNotify(player, method)) {
            pushAwayFromCrate(player, nicerLocation)
            return
        }

        openWithMethod(player, method, nicerLocation, PlacedCrates.getPlacedCrateAt(location.block.location))
    }

    /** Like [openPlaced], but opens every key the player has (shift-right-click-open-all). */
    fun openPlacedAll(player: Player, location: Location, method: OpenMethod) {
        val nicerLocation = location.block.location.add(0.5, 1.5, 0.5)

        if (!canOpenAndNotify(player, method)) {
            pushAwayFromCrate(player, nicerLocation)
            return
        }

        openAllWithMethod(player, method, nicerLocation)
    }

    private fun pushAwayFromCrate(player: Player, nicerLocation: Location) {
        val vector = player.location.clone().subtract(nicerLocation.toVector())
            .toVector()
            .normalize()
            .add(Vector(0.0, 1.0, 0.0))
            .multiply(plugin.configYml.getDouble("no-key-velocity"))

        player.velocity = vector
    }

    /**
     * Opens this crate for [player] via [method] (money/virtual/physical key),
     * checking affordability and the `ecocrates.open.<id>` permission first,
     * then consuming the payment/key on success.
     */
    fun openWithMethod(
        player: Player,
        method: OpenMethod,
        location: Location? = null,
        placedCrate: PlacedCrate? = null
    ) {
        if (!canOpenAndNotify(player, method)) {
            return
        }

        // Goes here rather than open() to keep force opening working
        if (!hasPermissionAndNotify(player)) {
            return
        }

        if (open(player, method, location = location, placedCrate = placedCrate)) {
            method.useMethod(this, player)
        }
    }

    /**
     * Opens every key [player] can pay for via [method] in one go, running
     * open/finish effects either once for the whole batch or per-key depending
     * on [isOpenAllEffectsPerKey].
     */
    fun openAllWithMethod(player: Player, method: OpenMethod, location: Location? = null) {
        if (!canOpenAndNotify(player, method)) {
            return
        }

        if (!hasPermissionAndNotify(player)) {
            return
        }

        if (hasRanOutOfRewardsAndNotify(player)) {
            return
        }

        val loc = location ?: player.eyeLocation
        val amount = method.getBulkAmount(this, player)

        fun triggerOpenEffects() {
            openEffects?.trigger(
                TriggerData(player = player, location = loc)
                    .dispatch(player.toDispatcher())
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

        fun triggerFinishEffects(rewardEvent: CrateRewardEvent) {
            finishEffects?.trigger(
                TriggerData(player = player, location = loc)
                    .dispatch(player.toDispatcher())
                    .apply {
                        addPlaceholders(
                            listOf(
                                NamedValue("crate", name),
                                NamedValue("crate_id", id),
                                NamedValue("reward", rewardEvent.reward.name),
                                NamedValue("reward_id", rewardEvent.reward.id)
                            )
                        )
                    }
            )
        }

        if (!isOpenAllEffectsPerKey) {
            triggerOpenEffects()
        }

        var lastRewardEvent: CrateRewardEvent? = null

        repeat(amount) {
            val openEvent = CrateOpenEvent(player, this, method, getRandomReward(player), false)
            Bukkit.getPluginManager().callEvent(openEvent)

            method.useMethod(this, player)
            player.profile.write(opensKey, getOpens(player) + 1)

            if (isOpenAllEffectsPerKey) {
                triggerOpenEffects()
            }

            val rewardEvent = CrateRewardEvent(player, this, openEvent.reward)
            Bukkit.getPluginManager().callEvent(rewardEvent)

            if (isOpenAllEffectsPerKey) {
                triggerFinishEffects(rewardEvent)
            } else {
                lastRewardEvent = rewardEvent
            }

            rewardEvent.reward.giveTo(player, this)
        }

        if (!isOpenAllEffectsPerKey) {
            lastRewardEvent?.let { triggerFinishEffects(it) }
        }
    }

    /**
     * Starts the roll animation for [player] (already assumed to have paid),
     * ticking it once per tick until it finishes, then offers a reroll or
     * finalizes via [handleFinish].
     *
     * @param rerollNumber How many times this roll has already been rerolled; 0 for a fresh open.
     * @return `false` if the player has run out of rewards or already has a crate open; `true` once rolling starts.
     */
    fun open(
        player: Player,
        method: OpenMethod,
        location: Location? = null,
        rerollNumber: Int = 0,
        placedCrate: PlacedCrate? = null
    ): Boolean {
        // getRandomReward throws if every reward is exhausted; bail before it does.
        if (hasRanOutOfRewardsAndNotify(player)) {
            return false
        }

        if (player.isOpeningCrate) {
            return false
        }

        val loc = location ?: player.eyeLocation
        val isReroll = rerollNumber > 0

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

        val roll = makeRoll(player, loc, event.reward, method, isReroll = isReroll, placedCrate = placedCrate)
        var tick = 0
        var hasFinalized = false

        fun finalizeRoll(forceFinish: Boolean) {
            if (hasFinalized) {
                return
            }

            hasFinalized = true

            roll.onFinish()
            player.isOpeningCrate = false

            if (hidesPlacedCrate) {
                placedCrate?.showTo(player)
            }

            val canRerollNow = rerollProfile.enabled
                && rerollNumber < rerollProfile.maxRerolls
                && player.hasPermission(rerollPermission)
                && rerollProfile.priceFor(rerollNumber + 1).canAfford(player)

            if (forceFinish || !canRerollNow) {
                handleFinish(roll)
            } else {
                ReRollGUI.open(roll, rerollNumber, rerollProfile)
            }
        }

        plugin.scheduler.on(player).runTimer({ task ->
            try {
                roll.tick(tick)
            } catch (e: Exception) {
                /*
                Bukkit doesn't cancel repeating tasks that throw, so without this the
                tick counter would never advance and the roll would repeat the same
                tick (and its effects) forever.
                 */
                plugin.logger.warning("Error while ticking roll for ${player.name}, cancelling")
                e.printStackTrace()

                task.cancel()
                finalizeRoll(true)
                return@runTimer
            }

            tick++

            if (!roll.shouldContinueTicking(tick) || !player.isOpeningCrate) {
                task.cancel()
                finalizeRoll(false)
            }
        }, 1, 1)

        player.isOpeningCrate = true
        player.profile.write(opensKey, getOpens(player) + 1)

        if (hidesPlacedCrate) {
            placedCrate?.hideFrom(player)
        }

        roll.roll()

        return true
    }

    /** Opens this crate's read-only preview GUI (rewards/odds) for [player]. */
    fun previewForPlayer(player: Player) {
        previewGUI.open(player)
    }

    /** Fires [CrateRewardEvent], runs finish effects, and gives [roll]'s reward to its player. */
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
