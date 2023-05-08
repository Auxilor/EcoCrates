package com.willfp.ecocrates.crate

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.gui.addPage
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.MenuBuilder
import com.willfp.eco.core.gui.menu.MenuLayer
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.TestableItem
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.particle.Particles
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecocrates.crate.placed.HologramFrame
import com.willfp.ecocrates.crate.placed.particle.ParticleAnimations
import com.willfp.ecocrates.crate.placed.particle.ParticleData
import com.willfp.ecocrates.crate.reroll.ReRollGUI
import com.willfp.ecocrates.crate.roll.Roll
import com.willfp.ecocrates.crate.roll.RollOptions
import com.willfp.ecocrates.crate.roll.Rolls
import com.willfp.ecocrates.event.CrateOpenEvent
import com.willfp.ecocrates.event.CrateRewardEvent
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.Rewards
import com.willfp.ecocrates.util.ConfiguredFirework
import com.willfp.ecocrates.util.ConfiguredSound
import com.willfp.ecocrates.util.PlayableSound
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.util.Vector
import java.util.Objects
import java.util.UUID

class Crate(
    val id: String,
    private val config: Config,
    private val plugin: EcoPlugin
) {
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
            ParticleAnimations.getByID(it.getString("animation")) ?: ParticleAnimations.SPIRAL
        )
    }

    val keyIsCustomItem = config.getBool("key.use-custom-item")

    val key: TestableItem = if (keyIsCustomItem) {
        Items.lookup(config.getString("key.item"))
    } else {
        CustomItem(
            plugin.namespacedKeyFactory.create("${id}_key"),
            { it.getAsKey() == this },
            Items.lookup(config.getString("key.item")).item
                .clone().apply { setAsKeyFor(this@Crate) }
        ).apply { register() }
    }

    val keyLore = config.getFormattedStrings("key.lore")

    val rewards = config.getStrings("rewards").mapNotNull { Rewards.getByID(it) }

    val permission: Permission =
        Bukkit.getPluginManager().getPermission("ecocrates.open.$id") ?: Permission(
            "ecocrates.open.$id",
            "Allows opening the $id crate",
            PermissionDefault.TRUE
        ).apply {
            addParent(Bukkit.getPluginManager().getPermission("ecocrates.open.*")!!, true)
            Bukkit.getPluginManager().addPermission(this)
        }

    val canReroll = config.getBool("can-reroll")

    val rerollPermission: Permission =
        Bukkit.getPluginManager().getPermission("ecocrates.reroll.$id") ?: Permission(
            "ecocrates.reroll.$id",
            "Allows rerolling the $id crate",
            PermissionDefault.TRUE
        ).apply {
            addParent(Bukkit.getPluginManager().getPermission("ecocrates.reroll.*")!!, true)
            Bukkit.getPluginManager().addPermission(this)
        }

    val canPayToOpen = config.getBool("pay-to-open.enabled")

    val priceToOpen = config.getDouble("pay-to-open.price")

    private val keysKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_keys"),
        PersistentDataKeyType.INT,
        0
    )

    private val opensKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_opens"),
        PersistentDataKeyType.INT,
        0
    )

    private val toGetKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_to_get"),
        PersistentDataKeyType.INT,
        0
    )

    private val rollFactory = Rolls.getByID(config.getString("roll"))!!

    private val previewGUI = menu(config.getInt("preview.rows")) {
        title = config.getFormattedString("preview.title")

        if (config.has("preview.pages")) {
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
                        val reward = Rewards.getByID(previewReward.getString("id")) ?: continue
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
                }
            }
        } else {
            setMask(
                FillerMask(
                    MaskItems.fromItemNames(config.getStrings("preview.mask.items")),
                    *config.getStrings("preview.mask.pattern").toTypedArray()
                )
            )

            /*
            Legacy reward config.
             */
            for (reward in rewards) {
                if (reward.displayRow == null || reward.displayColumn == null) {
                    continue
                }

                setSlot(
                    reward.displayRow,
                    reward.displayColumn,
                    slot(reward.getDisplay()) {
                        setUpdater { player, _, _ -> reward.getDisplay(player, this@Crate) }
                    }
                )
            }

            /*
            Modern reward config.
             */
            for (previewReward in config.getSubsections("preview.rewards")) {
                val reward = Rewards.getByID(previewReward.getString("id")) ?: continue
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
        }
    }

    private val openSound = PlayableSound(
        config.getSubsections("open.sounds")
            .map { ConfiguredSound.fromConfig(it) }
    )

    private val openMessages = config.getStrings("open.messages")

    private val openBroadcasts = config.getStrings("open.broadcasts")

    private val openCommands = config.getStrings("open.commands")

    private val finishSound = PlayableSound(
        config.getSubsections("finish.sounds")
            .map { ConfiguredSound.fromConfig(it) }
    )

    private val finishFireworks = config.getSubsections("finish.fireworks")
        .map { ConfiguredFirework.fromConfig(it) }

    private val finishMessages = config.getStrings("finish.messages")

    private val finishBroadcasts = config.getStrings("finish.broadcasts")

    private val finishCommands = config.getStrings("finish.commands")

    init {
        PlayerPlaceholder(
            plugin,
            "${id}_keys",
        ) { getVirtualKeys(it).toString() }.register()

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
        for (i in 0..(35 + 3)) {
            display.add(getRandomReward(player, displayWeight = true)) // Fill roll with display weight items
        }

        return rollFactory.create(
            RollOptions(
                reward,
                this,
                this.plugin,
                player,
                location,
                isReroll,
                method
            )
        )
    }

    private fun hasRanOutOfRewardsAndNotify(player: Player): Boolean {
        val ranOut = rewards.all { it.getWeight(player) <= 0 || it.getDisplayWeight(player) <= 0 }

        if (ranOut) {
            player.sendMessage(plugin.langYml.getMessage("all-rewards-used"))
        }

        return ranOut
    }

    private fun getRandomReward(player: Player, displayWeight: Boolean = false): Reward {
        val selection = rewards.toList().shuffled()

        // Limit to 1024 in case RNG breaks.
        for (i in 0..1024) {
            val reward = selection[i % rewards.size]
            if (NumberUtils.randFloat(0.0, 100.0) < reward.getPercentageChance(player, selection, displayWeight)) {
                return reward
            }
        }

        return selection.first()
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

    internal fun addToKeyGUI(builder: MenuBuilder) {
        if (!config.getBool("keygui.enabled")) {
            return
        }

        builder.setSlot(
            config.getInt("keygui.row"),
            config.getInt("keygui.column"),
            slot(
                ItemStackBuilder(Items.lookup(config.getString("keygui.item"))).build()
            ) {
                onLeftClick { event, _, _ ->
                    if (config.getBool("keygui.left-click-opens")) {
                        val player = event.whoClicked as Player
                        player.closeInventory()
                        openWithMethod(player, OpenMethod.VIRTUAL_KEY)
                    }
                }

                onRightClick { event, _, _ ->
                    if (config.getBool("keygui.right-click-previews")) {
                        val player = event.whoClicked as Player
                        player.closeInventory()
                        previewForPlayer(player)
                    }
                }

                onShiftLeftClick { event, _, _ ->
                    event.whoClicked.closeInventory()
                    config.getFormattedStrings("keygui.shift-left-click-message")
                        .forEach { event.whoClicked.sendMessage(it) }
                }

                setUpdater { player, _, previous ->
                    previous.apply {
                        itemMeta = itemMeta?.apply {
                            lore = config.getStrings("keygui.lore")
                                .map { it.replace("%keys%", getVirtualKeys(player).toString()) }
                                .map { it.formatEco(player) }
                        }
                    }
                    previous
                }
            }
        )
    }

    fun getRandomRewards(player: Player, amount: Int, displayWeight: Boolean = false): List<Reward> {
        return (0..amount).map { getRandomReward(player, displayWeight) }
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
            openSound.play(loc)

            openCommands.map { it.replace("%player%", player.name) }
                .forEach { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it) }

            openMessages.map { it.replace("%reward%", event.reward.displayName) }
                .map { plugin.langYml.prefix + StringUtils.format(it, player) }
                .forEach { player.sendMessage(it) }

            openBroadcasts.map { it.replace("%reward%", event.reward.displayName) }
                .map { it.replace("%player%", player.savedDisplayName) }
                .map { plugin.langYml.prefix + StringUtils.format(it, player) }
                .forEach { Bukkit.broadcastMessage(it) }
        }

        val roll = makeRoll(player, loc, event.reward, method, isReroll = isReroll)
        var tick = 0

        plugin.runnableFactory.create {
            roll.tick(tick)

            tick++
            if (!roll.shouldContinueTicking(tick) || !player.isOpeningCrate) {
                it.cancel()
                roll.onFinish()
                player.isOpeningCrate = false
                if (!canReroll(player) || roll.isReroll) handleFinish(roll) else ReRollGUI.open(roll)
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
        val location = roll.location

        val event = CrateRewardEvent(player, this, roll.reward)
        Bukkit.getPluginManager().callEvent(event)

        event.reward.giveTo(player)
        finishSound.play(location)
        finishFireworks.forEach { it.launch(location) }

        finishCommands.map { it.replace("%player%", player.name) }
            .forEach { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it) }

        finishMessages.map { it.replace("%reward%", event.reward.displayName) }
            .map { plugin.langYml.prefix + StringUtils.format(it, player) }
            .forEach { player.sendMessage(it) }

        finishBroadcasts.map { it.replace("%reward%", event.reward.displayName) }
            .map { it.replace("%player%", player.savedDisplayName) }
            .map { plugin.langYml.prefix + StringUtils.format(it, player) }
            .forEach { Bukkit.broadcastMessage(it) }
    }

    fun canReroll(player: Player): Boolean {
        if (!canReroll) {
            return false
        }

        return player.hasPermission(rerollPermission)
    }

    fun adjustVirtualKeys(player: OfflinePlayer, amount: Int) {
        player.profile.write(keysKey, player.profile.read(keysKey) + amount)
    }

    fun getVirtualKeys(player: OfflinePlayer): Int {
        return player.profile.read(keysKey)
    }

    fun hasPhysicalKey(player: Player): Boolean {
        return key.matches(player.inventory.itemInMainHand)
    }

    fun getKeysToGet(player: OfflinePlayer): Int {
        return player.profile.read(this.toGetKey)
    }

    fun setKeysToGet(player: OfflinePlayer, amount: Int) {
        player.profile.write(this.toGetKey, amount)
    }

    fun adjustKeysToGet(player: OfflinePlayer, amount: Int) {
        this.setKeysToGet(player, this.getKeysToGet(player) + amount)
    }

    fun hasVirtualKey(player: Player): Boolean {
        return getVirtualKeys(player) > 0
    }

    fun getOpens(player: OfflinePlayer): Int {
        return player.profile.read(opensKey)
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
