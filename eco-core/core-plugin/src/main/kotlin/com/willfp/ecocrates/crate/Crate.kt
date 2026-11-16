package com.willfp.ecocrates.crate

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.data.ServerProfile
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.leaderboard.Leaderboard
import com.willfp.eco.core.leaderboard.Leaderboards
import com.willfp.eco.core.leaderboard.registerStandardPlaceholders
import com.willfp.eco.core.particle.Particles
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.core.placeholder.PlayerlessPlaceholder
import com.willfp.eco.core.price.ConfiguredPrice
import com.willfp.eco.core.price.Prices
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecocrates.crate.open.OpenContext
import com.willfp.ecocrates.crate.open.OpenPick
import com.willfp.ecocrates.crate.open.OpenSession
import com.willfp.ecocrates.crate.open.OpenSessions
import com.willfp.ecocrates.crate.placed.HologramFrame
import com.willfp.ecocrates.crate.placed.PlacedCrate
import com.willfp.ecocrates.crate.placed.PlacedCrates
import com.willfp.ecocrates.crate.placed.particle.ParticleAnimations
import com.willfp.ecocrates.crate.placed.particle.ParticleData
import com.willfp.ecocrates.crate.reroll.RerollProfile
import com.willfp.ecocrates.crate.roll.Roll
import com.willfp.ecocrates.crate.roll.RollOptions
import com.willfp.ecocrates.crate.roll.RollPickContext
import com.willfp.ecocrates.crate.roll.Rolls
import com.willfp.ecocrates.event.CrateOpenEvent
import com.willfp.ecocrates.event.CrateRewardEvent
import com.willfp.ecocrates.event.PickInfo
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.BonusChances
import com.willfp.ecocrates.reward.MultiRewardSettings
import com.willfp.ecocrates.reward.PendingRewards
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.reward.Rewards
import com.willfp.ecocrates.reward.SourceTypes
import com.willfp.ecocrates.reward.planPicks
import com.willfp.libreforge.NamedValue
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.conditions.Conditions
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
import java.util.concurrent.ConcurrentHashMap

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
) : KRegistrable, RewardSource {
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
    private val allFinishEffects = Effects.compileChain(
        config.getSubsections("all-finish-effects"),
        NormalExecutorFactory.create(),
        ViolationContext(plugin, "Crate $id All Finish Effects")
    )

    private val openConditions = Conditions.compile(
        config.getSubsections("open-conditions"),
        ViolationContext(plugin, "Crate $id Open Conditions")
    )

    override val name: String = config.getFormattedString("name")

    override val sourceType = SourceTypes.CRATE

    val hologramFrames = config.getSubsections("placed.hologram.frames")
        .map { HologramFrame(it.getInt("tick"), it.getFormattedStrings("lines")) }

    val hologramTicks = positiveTicks("placed.hologram.ticks", hologramFrames.isNotEmpty())

    val hologramHeight = config.getDouble("placed.hologram.height")

    val isShowingRandomReward = config.getBool("placed.random-reward.enabled")

    val randomRewardHeight = config.getDouble("placed.random-reward.height")

    val randomRewardDelay = positiveTicks("placed.random-reward.delay", isShowingRandomReward)

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

    override val rewards: List<Reward> = config.getStrings("rewards").mapNotNull { rewardId ->
        Rewards.getByID(rewardId).also {
            if (it == null) {
                plugin.logger.warning("Crate '$id' references unknown reward '$rewardId'")
            }
        }
    }

    override val rollHeight: Double
        get() = randomRewardHeight

    override val bonusChances = BonusChances(config.getStrings("bonus-chances"))

    override val multiRewardSettings = MultiRewardSettings.fromConfig(config)

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

    private val priceToOpenFormat = config.getStringOrNull("pay-to-open.display") ?: "%value_commas% $currencyType"

    private val opensKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_opens"),
        PersistentDataKeyType.INT,
        0
    )

    private val lastWinnerKey: PersistentDataKey<String> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_last_winner"),
        PersistentDataKeyType.STRING,
        ""
    )

    private val lastRewardKey: PersistentDataKey<String> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_last_reward"),
        PersistentDataKeyType.STRING,
        ""
    )

    /**
     * The leaderboard ranking players by how many times they have opened this crate, or null
     * when leaderboards are disabled or before the first reload has registered it.
     */
    @Volatile
    var leaderboard: Leaderboard? = null
        private set

    /** The last player to win a reward from this crate, or null if nobody has yet. */
    val lastWinner: OfflinePlayer?
        get() = ServerProfile.load().read(lastWinnerKey)
            .takeIf { it.isNotBlank() }
            ?.let { Bukkit.getOfflinePlayer(UUID.fromString(it)) }

    /** The reward the [lastWinner] won, or null if nobody has won yet or it no longer exists. */
    val lastReward: Reward?
        get() = Rewards.getByID(ServerProfile.load().read(lastRewardKey))

    internal val rollFactory = Rolls.get(config.getString("roll"))!!

    // Whether to hide the placed crate's preview hologram/item from the player
    // while this crate's roll animation is playing, configured per roll type.
    private val hidesPlacedCrate = plugin.configYml
        .getBool("rolls.${rollFactory.id}.hide-placed-crate")

    private val previewGUI = PreviewGUI.build(config, this)



    init {
        if (rewards.isEmpty()) {
            plugin.logger.warning("Crate '$id' has no rewards - it can't be opened.")
        }

        bonusChances.warnIfUnsorted("Crate '$id'", multiRewardSettings.bonusChanceMode)

        PlayerPlaceholder(
            plugin,
            "${id}_keys",
        ) { sharedKey.getVirtualKeys(it).toString() }.register()

        PlayerPlaceholder(
            plugin,
            "${id}_opens",
        ) { getOpens(it).toString() }.register()

        PlayerlessPlaceholder(
            plugin,
            "${id}_last_winner",
        ) { lastWinner?.savedDisplayName ?: noLastWinner() }.register()

        PlayerlessPlaceholder(
            plugin,
            "${id}_last_reward",
        ) { lastReward?.name ?: noLastWinner() }.register()
    }

    /**
     * Register (or re-register) this crate's opens leaderboard and its placeholders.
     *
     * Called from the plugin's reload handler, after [Leaderboards.unregisterAll], because config
     * categories are loaded before that handler runs.
     */
    internal fun registerLeaderboard() {
        if (!plugin.configYml.getBool("leaderboard.enabled")) {
            leaderboard = null
            return
        }

        val board = Leaderboards.ofKey(plugin, "${id}_leaderboard", opensKey)

        leaderboard = board

        board.registerStandardPlaceholders(
            plugin = plugin,
            prefix = "${id}_leaderboard",
            emptyText = plugin.langYml.getString("top.empty-position").formatEco()
        ) { it.toInt().toString() }
    }

    private fun noLastWinner() = plugin.langYml.getFormattedString("messages.last-winner-none")

    private fun recordWin(player: Player, reward: Reward) {
        ServerProfile.load().apply {
            write(lastWinnerKey, player.uniqueId.toString())
            write(lastRewardKey, reward.id)
        }
    }

    private fun positiveTicks(path: String, isUsed: Boolean): Int {
        val ticks = config.getInt(path)
        if (ticks <= 0 && isUsed) {
            plugin.logger.warning("Crate '$id' has $path set to $ticks, defaulting to 1.")
        }
        return ticks.coerceAtLeast(1)
    }

    /**
     * Makes the roll that shows [rewards], the first of which is the roll's main reward.
     * [pickContext] says where [rewards] sit in the open.
     */
    internal fun createRoll(
        player: Player,
        context: OpenContext,
        rewards: List<Reward>,
        isReroll: Boolean,
        pickContext: RollPickContext
    ): Roll = rollFactory.create(
        RollOptions(
            rewards.first(),
            this,
            player,
            context.location,
            isReroll,
            context.method,
            context.placedCrate,
            rewards
        ).apply { this.pickContext = pickContext }
    )

    /** Hides the placed crate of [context] from [player] while a roll plays, if this crate's roll asks for it. */
    internal fun hidePlacedCrate(player: Player, context: OpenContext) {
        if (hidesPlacedCrate) {
            context.placedCrate?.hideFrom(player)
        }
    }

    /** Shows the placed crate of [context] to [player] again once a roll ends. */
    internal fun showPlacedCrate(player: Player, context: OpenContext) {
        if (hidesPlacedCrate) {
            context.placedCrate?.showTo(player)
        }
    }

    internal fun hasRanOutOfRewardsAndNotify(player: Player): Boolean {
        val ranOut = hasRanOutOfRewards(player)

        if (ranOut) {
            player.sendMessage(plugin.langYml.getMessage("all-rewards-used"))
        }

        return ranOut
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

    /** Evaluates open-conditions; failing conditions run their own not-met-effects. */
    private fun meetsOpenConditions(player: Player, location: Location?): Boolean =
        openConditions.areMetAndTrigger(
            TriggerData(
                player = player,
                location = location ?: player.location
            ).dispatch(player.toDispatcher())
        )

    /** Opens the placed crate at [location] for [player], pushing them away if they can't pay/afford it. */
    fun openPlaced(player: Player, location: Location, method: OpenMethod) {
        val nicerLocation = location.block.location.add(0.5, 1.5, 0.5)

        if (!canOpenAndNotify(player, method)) {
            pushAwayFromCrate(player, nicerLocation)
            return
        }

        if (player.hasPermission(permission) && !meetsOpenConditions(player, nicerLocation)) {
            pushAwayFromCrate(player, nicerLocation)
            return
        }

        val blockLocation = location.block.location
        val placedCrate = PlacedCrates.getPlacedCrateAt(blockLocation)

        if (needsPaymentConfirm(player, method)) {
            PayToOpenConfirmGUI.open(this, player) {
                if (PlacedCrates.getPlacedCrateAt(blockLocation) === placedCrate) {
                    openWithMethod(player, method, nicerLocation, placedCrate)
                }
            }
        } else {
            openWithMethod(player, method, nicerLocation, placedCrate)
        }
    }

    private fun needsPaymentConfirm(player: Player, method: OpenMethod) = method == OpenMethod.MONEY
        && canPayToOpen
        && plugin.configYml.getBool("pay-to-open-confirm.enabled")
        && player.hasPermission(permission)
        && !player.isOpeningCrate
        && !hasRanOutOfRewards(player)

    /** The display string of the pay-to-open price for [player]. */
    fun getPriceToOpenDisplay(player: Player): String =
        ConfiguredPrice(Prices.create(priceToOpen.toString(), currencyType), priceToOpenFormat).getDisplay(player)

    /** Like [openPlaced], but opens every key the player has (shift-right-click-open-all). */
    fun openPlacedAll(player: Player, location: Location, method: OpenMethod) {
        val nicerLocation = location.block.location.add(0.5, 1.5, 0.5)

        if (!canOpenAndNotify(player, method)) {
            pushAwayFromCrate(player, nicerLocation)
            return
        }

        if (player.hasPermission(permission) && !meetsOpenConditions(player, nicerLocation)) {
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

        if (!meetsOpenConditions(player, location)) {
            return
        }

        if (open(player, method, location = location, placedCrate = placedCrate)) {
            method.useMethod(this, player)
        }
    }

    /**
     * Opens every key [player] can pay for via [method] in one go, running
     * open/finish effects either once for the whole batch or per-key depending
     * on [isOpenAllEffectsPerKey]. Stops once [player] has run out of rewards,
     * leaving the remaining keys unspent.
     */
    fun openAllWithMethod(player: Player, method: OpenMethod, location: Location? = null) {
        if (
            !canOpenAndNotify(player, method)
            || !hasPermissionAndNotify(player)
            || !meetsOpenConditions(player, location)
        ) {
            return
        }

        val loc = location ?: player.eyeLocation
        var remainingOpens = method.getBulkAmount(this, player)
        var lastPicks: List<OpenPick>? = null
        val batchRewards = ArrayList<Reward>()

        while (remainingOpens-- > 0 && !hasRanOutOfRewardsAndNotify(player)) {
            val picks = openOneKeyOfAll(player, method, loc, isFirstKey = lastPicks == null)

            if (picks.isNotEmpty()) {
                lastPicks = picks
                batchRewards += picks.map { it.reward }
            }
        }

        if (!isOpenAllEffectsPerKey) {
            lastPicks?.let { picks ->
                val last = picks.last()
                triggerFinishEffects(
                    player = player,
                    reward = last.reward,
                    pickValues = pickValues(pickNumber = last.number, amount = picks.size),
                    location = loc
                )
                runAllFinishEffects(player, batchRewards, loc)
            }
        }
    }

    private fun openOneKeyOfAll(
        player: Player,
        method: OpenMethod,
        location: Location,
        isFirstKey: Boolean
    ): List<OpenPick> {
        val picks = planPicks(player) { pickNumber, reward, isBonus ->
            fireOpenEvent(player, method, reward, false, PickInfo(pickNumber, isBonus))
        }

        if (picks.isNotEmpty()) {
            method.useMethod(this, player)
            recordOpen(player)

            if (isOpenAllEffectsPerKey || isFirstKey) {
                triggerOpenEffects(player, location)
            }

            for (pick in picks) {
                deliver(
                    player = player,
                    pick = pick,
                    amount = picks.size,
                    location = location,
                    runFinishEffects = isOpenAllEffectsPerKey
                )
            }

            if (isOpenAllEffectsPerKey) {
                runAllFinishEffects(player, picks.map { it.reward }, location)
            }
        }

        return picks
    }

    /**
     * Starts an open for [player] (already assumed to have paid): decides every pick, then plays
     * them through rolls, offering rerolls, and gives each reward as it is accepted.
     *
     * @param rerollNumber How many times this roll has already been rerolled; 0 for a fresh open.
     * @return `false` if the player has run out of rewards, already has a crate open, or the open was
     * cancelled; `true` once rolling starts.
     */
    fun open(
        player: Player,
        method: OpenMethod,
        location: Location? = null,
        rerollNumber: Int = 0,
        placedCrate: PlacedCrate? = null
    ): Boolean {
        // getRandomReward throws if every reward is exhausted; bail before it does.
        if (hasRanOutOfRewardsAndNotify(player) || player.isOpeningCrate || OpenSessions.get(player) != null) {
            return false
        }

        val loc = location ?: player.eyeLocation
        val isReroll = rerollNumber > 0

        val picks = if (isReroll) {
            picksForRerollOutsideSession(player, method, rerollNumber)
        } else {
            planPicks(player) { pickNumber, reward, isBonus ->
                fireOpenEvent(player, method, reward, false, PickInfo(pickNumber, isBonus))
            }
        }

        if (picks.isNotEmpty()) {
            if (!isReroll) {
                triggerOpenEffects(player, loc)
            }

            recordOpen(player)

            OpenSessions.start(OpenSession(this, player, picks, OpenContext(method, loc, placedCrate)))
        }

        return picks.isNotEmpty()
    }

    private fun picksForRerollOutsideSession(player: Player, method: OpenMethod, rerollNumber: Int): List<OpenPick> {
        val reward = fireOpenEvent(player, method, getRandomReward(player), true, PickInfo.FIRST)

        return listOfNotNull(reward?.let { OpenPick(number = 1, reward = it, isBonus = false) })
            .onEach { it.rerolls = rerollNumber }
    }

    private fun canRerollNow(player: Player, rerollNumber: Int) = rerollProfile.enabled
        && rerollNumber < rerollProfile.maxRerolls
        && player.hasPermission(rerollPermission)
        && rerollProfile.priceFor(rerollNumber + 1).canAfford(player)

    /** If a reroll can start for [player] now: rewards are left and no roll is playing. */
    internal fun canStartReroll(player: Player): Boolean =
        !hasRanOutOfRewardsAndNotify(player) && !player.isOpeningCrate

    /** If [player] can be offered a reroll of [pick] now. */
    internal fun canReroll(player: Player, pick: OpenPick): Boolean = canRerollNow(player, pick.rerolls)
        && pick.reward.isRerollable
        && (!pick.isBonus || multiRewardSettings.isBonusRerollable)

    /** The price of the next reroll of [pick]. */
    internal fun rerollPrice(pick: OpenPick): ConfiguredPrice = rerollProfile.priceFor(pick.rerolls + 1)

    /** Counts one open of this crate for [player]. Rerolls count as opens too. */
    internal fun recordOpen(player: Player) {
        player.profile.write(opensKey, getOpens(player) + 1)
    }

    /**
     * Fires the [CrateOpenEvent] for one pick.
     *
     * @return The event's reward, or null if the event was cancelled.
     */
    internal fun fireOpenEvent(
        player: Player,
        method: OpenMethod,
        reward: Reward,
        isReroll: Boolean,
        pick: PickInfo
    ): Reward? {
        val event = CrateOpenEvent(player, this, method, reward, isReroll, pick)
        Bukkit.getPluginManager().callEvent(event)
        return event.takeUnless { it.isCancelled }?.reward
    }

    private fun crateValues(): List<NamedValue> = listOf(
        NamedValue("crate", name),
        NamedValue("crate_id", id)
    )

    private fun pickValues(pickNumber: Int, amount: Int): List<NamedValue> = listOf(
        NamedValue("reward_number", pickNumber),
        NamedValue("reward_amount", amount)
    )

    private fun triggerOpenEffects(player: Player, location: Location) {
        openEffects?.trigger(
            TriggerData(player = player, location = location)
                .dispatch(player.toDispatcher())
                .apply { addPlaceholders(crateValues()) }
        )
    }

    private fun triggerFinishEffects(
        player: Player,
        reward: Reward,
        pickValues: List<NamedValue>,
        location: Location?
    ) {
        finishEffects?.trigger(
            TriggerData(player = player, location = location)
                .dispatch(player.toDispatcher())
                .apply {
                    addPlaceholders(
                        crateValues() + listOf(
                            NamedValue("reward", reward.name),
                            NamedValue("reward_id", reward.id)
                        ) + pickValues
                    )
                }
        )
    }

    /** Runs the all-finish effects once for every reward of an open. */
    internal fun runAllFinishEffects(player: Player, rewards: List<Reward>, location: Location? = null) {
        allFinishEffects?.trigger(
            TriggerData(player = player, location = location)
                .dispatch(player.toDispatcher())
                .apply {
                    addPlaceholders(
                        crateValues() + listOf(
                            NamedValue("rewards", rewards.joinToString(", ") { it.name }),
                            NamedValue("reward_amount", rewards.size)
                        )
                    )
                }
        )
    }

    /** Opens this crate's read-only preview GUI (rewards/odds) for [player]. */
    fun previewForPlayer(player: Player) {
        previewGUI.open(player)
    }

    /** Fires [CrateRewardEvent], runs finish effects, and gives [roll]'s reward to its player. Kept for addons. */
    @Suppress("U002")
    fun handleFinish(roll: Roll) {
        handleFinish(roll.player, roll.reward)
    }

    override fun handleFinish(player: Player, reward: Reward) {
        deliver(player = player, pick = OpenPick(number = 1, reward = reward, isBonus = false), amount = 1)
    }

    /**
     * Gives one pick of an open (or queues it if [player] is offline), firing [CrateRewardEvent]
     * and running finish effects. The pick's reward is replaced by the one event listeners left.
     *
     * @param player The player receiving the reward.
     * @param pick The pick to give.
     * @param amount The number of rewards in the open.
     * @param location Where the finish effects run, or null for the player's location.
     * @param runFinishEffects If the finish effects run for this reward.
     */
    internal fun deliver(
        player: Player,
        pick: OpenPick,
        amount: Int,
        location: Location? = null,
        runFinishEffects: Boolean = true
    ) {
        if (!player.isOnline) {
            PendingRewards.queue(player, this, pick.reward)
            return
        }

        val event = CrateRewardEvent(player, this, pick.reward, PickInfo(pick.number, pick.isBonus))
        Bukkit.getPluginManager().callEvent(event)
        pick.reward = event.reward

        val placeholders = pickValues(pickNumber = pick.number, amount = amount)

        if (runFinishEffects) {
            triggerFinishEffects(player = player, reward = event.reward, pickValues = placeholders, location = location)
        }

        recordWin(player, event.reward)
        event.reward.giveTo(player = player, sourceName = name, sourceId = id, extraPlaceholders = placeholders)
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

private val openingCrates: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

var Player.isOpeningCrate: Boolean
    get() = openingCrates.contains(this.uniqueId)
    set(value) {
        if (value) {
            openingCrates.add(this.uniqueId)
        } else {
            openingCrates.remove(this.uniqueId)
        }
    }
