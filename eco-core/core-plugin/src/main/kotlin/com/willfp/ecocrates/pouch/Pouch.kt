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
import com.willfp.ecocrates.crate.roll.RollFactory
import com.willfp.ecocrates.crate.roll.RollOptions
import com.willfp.ecocrates.crate.roll.RollSession
import com.willfp.ecocrates.crate.roll.RollValidation
import com.willfp.ecocrates.crate.roll.Rolls
import com.willfp.ecocrates.event.PouchOpenEvent
import com.willfp.ecocrates.event.PouchRewardEvent
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.PendingRewards
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.reward.Rewards
import com.willfp.ecocrates.reward.SourceTypes
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
 * a single reward.
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

    private val rollFactory: RollFactory<*> = run {
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

    private val opensKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("pouch_${id}_opens"),
        PersistentDataKeyType.INT,
        0
    )

    private val previewGUI = PreviewGUI.build(config, this)

    init {
        PlayerPlaceholder(
            plugin,
            "pouch_${id}_opens"
        ) { getOpens(it).toString() }.register()
    }

    fun getOpens(player: OfflinePlayer): Int = player.profile.read(opensKey)

    fun previewForPlayer(player: Player) {
        previewGUI.open(player)
    }

    private fun placeholders(): List<NamedValue> = listOf(
        NamedValue("pouch", name),
        NamedValue("pouch_id", id),
        NamedValue("pouch_rarity", rarity)
    )

    /**
     * Opens this pouch for [player], consuming one of [held]. Checks run in
     * order - permission, already opening, rewards left, open-conditions,
     * price, open event - and nothing is taken unless all of them pass.
     */
    fun open(player: Player, held: ItemStack) {
        if (!player.hasPermission(permission)) {
            player.sendMessage(plugin.langYml.getMessage("no-pouch-permission").replace("%name%", name))
            return
        }

        if (player.isOpeningCrate) {
            return
        }

        if (hasRanOutOfRewards(player)) {
            player.sendMessage(plugin.langYml.getMessage("all-rewards-used"))
            return
        }

        // Failing conditions report through their own not-met-effects.
        val conditionsMet = openConditions.areMetAndTrigger(
            TriggerData(
                player = player,
                location = player.location,
                item = held
            ).dispatch(player.toDispatcher())
        )

        if (!conditionsMet) {
            return
        }

        if (!price.canAfford(player)) {
            player.sendMessage(
                plugin.langYml.getMessage("pouch-cannot-afford")
                    .replace("%name%", name)
                    .replace("%price%", price.getDisplay(player))
            )
            return
        }

        val event = PouchOpenEvent(player, this, getRandomReward(player))
        Bukkit.getPluginManager().callEvent(event)

        if (event.isCancelled) {
            return
        }

        price.pay(player)
        held.amount -= 1
        player.profile.write(opensKey, getOpens(player) + 1)

        openEffects?.trigger(
            TriggerData(
                player = player,
                location = player.location
            ).dispatch(player.toDispatcher())
                .apply { addPlaceholders(placeholders()) }
        )

        val roll = rollFactory.create(
            RollOptions(
                event.reward,
                this,
                player,
                player.eyeLocation,
                false,
                OpenMethod.OTHER
            )
        )

        RollSession.start(roll) { finishedRoll, _ ->
            handleFinish(finishedRoll.player, finishedRoll.reward)
        }
    }

    override fun handleFinish(player: Player, reward: Reward) {
        if (!player.isOnline) {
            PendingRewards.queue(player, this, reward)
            return
        }

        val event = PouchRewardEvent(player, this, reward)
        Bukkit.getPluginManager().callEvent(event)

        finishEffects?.trigger(
            TriggerData(player = player)
                .dispatch(player.toDispatcher())
                .apply {
                    addPlaceholders(
                        placeholders() + listOf(
                            NamedValue("reward", event.reward.name),
                            NamedValue("reward_id", event.reward.id)
                        )
                    )
                }
        )

        event.reward.giveTo(player, this)
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
