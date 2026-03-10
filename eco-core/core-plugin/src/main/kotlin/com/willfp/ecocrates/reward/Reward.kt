package com.willfp.ecocrates.reward

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.fast.FastItemStack
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import com.willfp.eco.core.recipe.parts.MaterialTestableItem
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.toNiceString
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.PermissionMultipliers
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.util.RewardWeightEvent
import com.willfp.libreforge.NamedValue
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.effects.executors.impl.NormalExecutorFactory
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.TriggerData
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import java.util.Objects

class Reward(
    override val id: String,
    private val config: Config
) : KRegistrable {
    private val winEffects = Effects.compileChain(
        config.getSubsections("win-effects"),
        NormalExecutorFactory.create(),
        ViolationContext(plugin, "Reward $id Win Effects")
    )

    val name = config.getFormattedString("name")
    val displayName = config.getFormattedString("display.name")

    private val permission = Permission(
        "ecocrates.reward.$id",
        "Allows getting $id as a reward",
        PermissionDefault.TRUE
    ).apply {
        if (Bukkit.getPluginManager().getPermission("ecocrates.reward.*") == null) {
            addParent("ecocrates.reward.*", true)
        }
        if (Bukkit.getPluginManager().getPermission("ecocrates.reward.$id") == null) {
            Bukkit.getPluginManager().addPermission(this)
        }
    }

    val maxWins = config.getInt("max-wins")

    private val winsKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_wins"),
        PersistentDataKeyType.INT,
        0
    )

    private val canPermissionMultiply = config.getBool("weight.permission-multipliers")

    private val baseDisplay = ItemStackBuilder(
        Items.lookup(config.getString("display.item"))
            .let { if (it is EmptyTestableItem) MaterialTestableItem(Material.STONE) else it }
    ).setDisplayName(config.getString("display.name"))
        .build()

    fun getDisplay(player: Player, crate: Crate): ItemStack {
        val item = baseDisplay.clone()
        val fis = FastItemStack.wrap(item)
        val lore = config.getStrings("display.lore").map {
            it.replace(
                "%chance%",
                getPercentageChance(player, crate.rewards).toNiceString()
            ).replace(
                "%weight%",
                this.getWeight(player).toNiceString()
            )
                // legacy + illegal
                .replace(
                    "%actual_chance%",
                    getPercentageChance(player, crate.rewards).toNiceString()
                ).replace(
                    "%actual_weight%",
                    this.getWeight(player).toNiceString()
                ).formatEco(player)
        }

        if (config.getBool("display.dont-keep-lore")) {
            fis.lore = lore
        } else {
            fis.lore = fis.lore + lore
        }

        return fis.unwrap()
    }

    fun getDisplay(): ItemStack {
        return baseDisplay.clone()
    }

    fun getWeight(player: Player): Double {

        val weight =
            if (config.has("weight.value"))
                config.getDoubleFromExpression("weight.value", player)
            // legacy
            else config.getDoubleFromExpression("weight.actual", player)
        if (maxWins > 0) {
            if (player.profile.read(winsKey) >= maxWins) {
                return 0.0
            }
        }
        if (!player.hasPermission(permission)) {
            return 0.0
        }

        val event = RewardWeightEvent(player, this, weight)
        Bukkit.getPluginManager().callEvent(event)

        return event.weight
    }

    fun getPercentageChance(player: Player, among: Collection<Reward>): Double {
        val others = among.toMutableList()
        others.remove(this)

        var weight = this.getWeight(player)

        if (canPermissionMultiply) {
            weight *= PermissionMultipliers.getForPlayer(player).multiplier
        }

        var totalWeight = weight
        for (other in others) {
            totalWeight += other.getWeight(player)
        }

        return (weight / totalWeight) * 100
    }

    // Legacy
    @Deprecated("Use previewReward instead.")
    val displayRow = config.getIntOrNull("display.row")

    @Deprecated("Use previewReward instead.")
    val displayColumn = config.getIntOrNull("display.column")

    @Deprecated("Use winEffects instead.")
    private val commands = config.getStrings("commands")

    @Deprecated("Use winEffects instead.")
    private val items = config.getStrings("items").map { Items.lookup(it) }.filterNot { it is EmptyTestableItem }

    @Deprecated("Use winEffects instead.")
    private val messages = config.getFormattedStrings("messages")

    init {
        PlayerPlaceholder(
            plugin,
            "${id}_wins",
        ) { getWins(it).toString() }.register()

        if (config.has("display.row")) {
            plugin.logger.warning(
                "Reward '$id' uses deprecated 'display.row'."
            )
        }
        if (config.has("display.column")) {
            plugin.logger.warning(
                "Reward '$id' uses deprecated 'display.column'."
            )
        }
        if (config.has("commands")) {
            plugin.logger.warning(
                "Reward '$id' uses deprecated 'commands'. Please switch to 'win-effects'."
            )
        }
        if (config.has("items")) {
            plugin.logger.warning(
                "Reward '$id' uses deprecated 'items'. Please switch to 'win-effects'."
            )
        }
        if (config.has("messages")) {
            plugin.logger.warning(
                "Reward '$id' uses deprecated 'messages'. Please switch to 'win-effects'."
            )
        }
    }

    @Suppress("DEPRECATION")
    fun giveTo(player: Player, crate: Crate) {
        winEffects?.trigger(
            TriggerData(player = player)
                .dispatch(player.toDispatcher())
                .apply {
                    addPlaceholders(
                        listOf(
                            NamedValue("reward", name),
                            NamedValue("reward_id", id),
                            NamedValue("crate", crate.name),
                            NamedValue("crate_id", crate.id)
                        )
                    )
                }
        )

        // Legacy
        for (command in commands) {
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                command.replace("%player%", player.name)
                    .replace("%reward%", name)
                    .replace("%reward_id%", id)
                    .replace("%crate%", crate.name)
                    .replace("%crate_id%", crate.id)
            )
        }

        DropQueue(player)
            .addItems(items.map { it.item })
            .forceTelekinesis()
            .push()

        messages.map {
            it.replace("%player%", player.name)
                .replace("%reward%", name)
                .replace("%reward_id%", id)
                .replace("%crate%", crate.name)
                .replace("%crate_id%", crate.id)
        }.forEach { player.sendMessage(plugin.langYml.prefix + it) }

        if (maxWins > 0) {
            player.profile.write(winsKey, player.profile.read(winsKey) + 1)
        }
    }

    fun getWins(player: OfflinePlayer): Int {
        return if (maxWins > 0) player.profile.read(winsKey) else 0
    }

    fun resetWins(player: OfflinePlayer) {
        if (maxWins > 0) {
            player.profile.write(winsKey, 0)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Reward) {
            return false
        }
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    override fun getID(): String {
        return id
    }
}
