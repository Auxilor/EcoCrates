package com.willfp.ecocrates.pouch

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.core.price.ConfiguredPrice
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.PreviewGUI
import com.willfp.ecocrates.crate.isOpeningCrate
import com.willfp.ecocrates.crate.open.OpenContext
import com.willfp.ecocrates.crate.open.OpenPick
import com.willfp.ecocrates.crate.open.OpenSession
import com.willfp.ecocrates.crate.open.OpenSessions
import com.willfp.ecocrates.crate.roll.RollFactory
import com.willfp.ecocrates.crate.roll.RollValidation
import com.willfp.ecocrates.crate.roll.Rolls
import com.willfp.ecocrates.event.PickInfo
import com.willfp.ecocrates.event.PouchOpenEvent
import com.willfp.ecocrates.event.PouchRewardEvent
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
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import java.util.Objects

/**
 * A pouch: a held item that, when used, consumes one and runs a GUI roll for
 * one reward, plus any bonus rewards won from its bonus chances.
 *
 * @param id The pouch's config-file ID.
 * @param config The pouch's parsed config section.
 */
class Pouch(
    override val id: String,
    private val config: Config
) : RewardSource, KRegistrable {
    override val name: String = config.getFormattedString("name")

    override val sourceType = SourceTypes.POUCH

    // GUI rolls never read it.
    override val rollHeight: Double = 0.0

    val rarity: String = config.getFormattedString("rarity")

    private val pouchValues: List<NamedValue> = listOf(
        NamedValue("pouch", name),
        NamedValue("pouch_id", id),
        NamedValue("pouch_rarity", rarity)
    )

    /** [rarity] without colour codes, as matched by the pouch_rarity filter. */
    val plainRarity: String = ChatColor.stripColor(rarity) ?: rarity

    override val rewards: List<Reward> = config.getStrings("rewards").mapNotNull { Rewards.getByID(it) }

    val previewOnShift: Boolean = config.getBool("preview-on-shift")

    val price: ConfiguredPrice = config.getSubsectionOrNull("price")
        ?.let { ConfiguredPrice.create(it) }
        ?: ConfiguredPrice.FREE

    val item = PouchItem(id, config.getSubsection("item"))

    val permission: Permission =
        Bukkit.getPluginManager().getPermission("ecocrates.pouch.$id") ?: Permission(
            "ecocrates.pouch.$id",
            "Allows opening the $id pouch",
            PermissionDefault.TRUE
        ).apply {
            Bukkit.getPluginManager().getPermission("ecocrates.pouch.*")?.let { addParent(it, true) }
            Bukkit.getPluginManager().addPermission(this)
        }

    override val bonusChances = BonusChances(config.getStrings("bonus-chances"))

    override val multiRewardSettings = MultiRewardSettings.fromConfig(config)

    internal val rollFactory: RollFactory<*> = run {
        val resolution = RollValidation.resolveGuiRoll(config.getString("roll")) { Rolls.get(it)?.isGuiRoll }
        resolution.warning?.let { plugin.logger.warning("Pouch '$id' $it") }
        Rolls.get(resolution.id)!!
    }

    private val openConditions = Conditions.compile(
        config.getSubsections("open-conditions"),
        ViolationContext(plugin, "Pouch $id Open Conditions")
    )

    private val openEffects = Effects.compileChain(
        config.getSubsections("open-effects"),
        NormalExecutorFactory.create(),
        ViolationContext(plugin, "Pouch $id Opening Effects")
    )

    private val finishEffects = Effects.compileChain(
        config.getSubsections("finish-effects"),
        NormalExecutorFactory.create(),
        ViolationContext(plugin, "Pouch $id Finish Effects")
    )

    private val allFinishEffects = Effects.compileChain(
        config.getSubsections("all-finish-effects"),
        NormalExecutorFactory.create(),
        ViolationContext(plugin, "Pouch $id All Finish Effects")
    )

    private val opensKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("pouch_${id}_opens"),
        PersistentDataKeyType.INT,
        0
    )

    private val previewGUI = PreviewGUI.build(config, this)

    init {
        bonusChances.warnIfUnsorted("Pouch '$id'", multiRewardSettings.bonusChanceMode)

        PlayerPlaceholder(
            plugin,
            "pouch_${id}_opens"
        ) { getOpens(it).toString() }.register()
    }

    fun getOpens(player: OfflinePlayer): Int = player.profile.read(opensKey)

    fun previewForPlayer(player: Player) {
        previewGUI.open(player)
    }

    /**
     * Opens this pouch for [player], consuming one of [held]. Checks run in
     * order - permission, already opening, rewards left, open-conditions,
     * price, open event per pick - and nothing is taken unless all of them pass.
     */
    fun open(player: Player, held: ItemStack) {
        if (canOpenAndNotify(player, held)) {
            val picks = planPicks(player) { pickNumber, reward, isBonus ->
                val event = PouchOpenEvent(player, this, reward, PickInfo(pickNumber, isBonus))
                Bukkit.getPluginManager().callEvent(event)
                event.takeUnless { it.isCancelled }?.reward
            }

            // The held item may have changed since it was captured (e.g. another
            // plugin modified the inventory during the event); re-read it so we
            // don't pay for or consume a pouch the player no longer holds.
            val current = player.inventory.itemInMainHand

            if (picks.isNotEmpty() && item.matches(current)) {
                price.pay(player)
                current.amount -= 1
                player.profile.write(opensKey, getOpens(player) + 1)

                openEffects?.trigger(
                    TriggerData(
                        player = player,
                        location = player.location
                    ).dispatch(player.toDispatcher())
                        .apply { addPlaceholders(pouchValues) }
                )

                OpenSessions.start(
                    OpenSession(this, player, picks, OpenContext(OpenMethod.OTHER, player.eyeLocation, null))
                )
            }
        }
    }

    private fun canOpenAndNotify(player: Player, held: ItemStack): Boolean = when {
        !player.hasPermission(permission) -> {
            player.sendMessage(plugin.langYml.getMessage("no-pouch-permission").replace("%name%", name))
            false
        }

        player.isOpeningCrate || OpenSessions.get(player) != null -> {
            false
        }

        hasRanOutOfRewards(player) -> {
            player.sendMessage(plugin.langYml.getMessage("all-rewards-used"))
            false
        }

        // Failing conditions report through their own not-met-effects.
        !openConditions.areMetAndTrigger(
            TriggerData(
                player = player,
                location = player.location,
                item = held
            ).dispatch(player.toDispatcher())
        ) -> {
            false
        }

        !price.canAfford(player) -> {
            player.sendMessage(
                plugin.langYml.getMessage("pouch-cannot-afford")
                    .replace("%name%", name)
                    .replace("%price%", price.getDisplay(player))
            )
            false
        }

        else -> {
            true
        }
    }

    override fun handleFinish(player: Player, reward: Reward) {
        deliverPick(player = player, pick = OpenPick(number = 1, reward = reward, isBonus = false), amount = 1)
    }

    internal fun deliverPick(player: Player, pick: OpenPick, amount: Int) {
        if (!player.isOnline) {
            PendingRewards.queue(player, this, pick.reward)
            return
        }

        val event = PouchRewardEvent(player, this, pick.reward, PickInfo(pick.number, pick.isBonus))
        Bukkit.getPluginManager().callEvent(event)
        pick.reward = event.reward

        val pickValues = listOf(
            NamedValue("reward_number", pick.number),
            NamedValue("reward_amount", amount)
        )

        finishEffects?.trigger(
            TriggerData(player = player)
                .dispatch(player.toDispatcher())
                .apply {
                    addPlaceholders(
                        pouchValues + listOf(
                            NamedValue("reward", event.reward.name),
                            NamedValue("reward_id", event.reward.id)
                        ) + pickValues
                    )
                }
        )

        event.reward.giveTo(player = player, sourceName = name, sourceId = id, extraPlaceholders = pickValues)
    }

    internal fun finishOpen(player: Player, rewards: List<Reward>) {
        allFinishEffects?.trigger(
            TriggerData(player = player)
                .dispatch(player.toDispatcher())
                .apply {
                    addPlaceholders(
                        pouchValues + listOf(
                            NamedValue("rewards", rewards.joinToString(", ") { it.name }),
                            NamedValue("reward_amount", rewards.size)
                        )
                    )
                }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Pouch) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }

    override fun toString(): String {
        return "Pouch{id=$id}"
    }

    override fun getID(): String {
        return id
    }
}
