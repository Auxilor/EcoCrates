package com.willfp.ecocrates.reward

import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.PermissionMultipliers
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.util.RewardWeightEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import wiki.ecohub.config.Config
import wiki.ecohub.config.ViolationContext
import wiki.ecohub.core.registry.KRegistrable
import wiki.ecohub.core.stdlib.NamedValue
import wiki.ecohub.core.stdlib.toNiceString
import wiki.ecohub.data.PersistentDataKey
import wiki.ecohub.data.PersistentDataKeyType
import wiki.ecohub.data.profile.ServerProfile
import wiki.ecohub.game.entity.player.profile
import wiki.ecohub.game.entity.toDispatcher
import wiki.ecohub.game.item.FastItemStack
import wiki.ecohub.game.item.Items
import wiki.ecohub.game.item.builder.ItemStackBuilder
import wiki.ecohub.game.item.lookup.EmptyTestableItem
import wiki.ecohub.game.item.lookup.MaterialTestableItem
import wiki.ecohub.rules.chain.NormalExecutorFactory
import wiki.ecohub.rules.effect.Effects
import wiki.ecohub.rules.trigger.TriggerData
import wiki.ecohub.ui.placeholder.type.PlayerPlaceholder
import wiki.ecohub.ui.placeholder.type.PlayerlessPlaceholder
import wiki.ecohub.ui.text.formatEco
import java.util.Locale
import java.util.Objects

/**
 * A configured reward: its display item/lore, weight expression, win cap,
 * and the win-effects triggered when a player receives it.
 *
 * @param id The reward's config-file ID.
 * @param config The reward's parsed config section.
 */
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

    private val permission: Permission =
        Bukkit.getPluginManager().getPermission("ecocrates.rewards.$id") ?: Permission(
            "ecocrates.rewards.$id",
            "Allows getting $id as a reward",
            PermissionDefault.TRUE
        ).apply {
            Bukkit.getPluginManager().getPermission("ecocrates.rewards.*")?.let { addParent(it, true) }
            Bukkit.getPluginManager().addPermission(this)
        }

    val maxWins = config.getInt("max-wins")

    private val winsKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_wins"),
        PersistentDataKeyType.INT,
        0
    )

    private val totalWinsKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_total"),
        PersistentDataKeyType.INT,
        0
    )

    private val canPermissionMultiply = config.getBool("weight.permission-multipliers")

    private val baseDisplay = ItemStackBuilder(
        Items.lookup(config.getString("display.item"))
            .let { if (it is EmptyTestableItem) MaterialTestableItem(Material.STONE) else it }
    ).setDisplayName(config.getString("display.name"))
        .build()

    fun getDisplay(player: Player, source: RewardSource): ItemStack {
        val item = baseDisplay.clone()
        val fis = FastItemStack.wrap(item)
        val weights = DisplayRewardWeights.get(player, source)
        val rewardWeight = weights.find { it.reward == this } ?: getRewardWeight(player)
        val chance = weights.sumOf { it.effectiveWeight }.let { totalWeight ->
            if (totalWeight <= 0.0) 0.0 else rewardWeight.effectiveWeight / totalWeight * 100
        }
        val lore = config.getStrings("display.lore").map { line ->
            line.replace(
                "%chance%",
                formatChance(chance)
            ).replace(
                "%weight%",
                rewardWeight.weight.toNiceString()
            ).formatEco(player, formatPlaceholders = true)
        }

        if (config.getBool("display.dont-keep-lore")) {
            fis.setLore(lore)
        } else {
            fis.setLore(fis.getLore() + lore)
        }

        return fis.unwrap()
    }

    /** If opening [source] can give [player] this reward. Nothing is obtainable from a source that refuses to open. */
    fun isObtainable(player: Player, source: RewardSource): Boolean {
        val drawableWeights = DisplayRewardWeights.get(player, source).filter { it.weight > 0 }
        val rewardWeight = drawableWeights.find { it.reward == this } ?: return false

        return rewardWeight.effectiveWeight > 0 || drawableWeights.none { it.effectiveWeight > 0 }
    }

    /** Kept for binary compatibility with addons compiled against the crate-only signature. */
    fun getDisplay(player: Player, crate: Crate): ItemStack =
        getDisplay(player, crate as RewardSource)

    fun getDisplay(): ItemStack {
        return baseDisplay.clone()
    }

    fun getWeight(player: Player): Double {
        val weight = config.getDoubleFromExpression("weight.value", player)
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

    fun getEffectiveWeight(player: Player): Double =
        applyPermissionMultiplier(player, this.getWeight(player))

    fun getRewardWeight(player: Player): RewardWeight {
        val weight = this.getWeight(player)
        return RewardWeight(this, weight, applyPermissionMultiplier(player, weight))
    }

    private fun applyPermissionMultiplier(player: Player, baseWeight: Double): Double {
        var weight = baseWeight

        if (canPermissionMultiply) {
            weight *= PermissionMultipliers.getForPlayer(player).multiplier
        }

        return weight
    }

    fun getPercentageChance(player: Player, among: Collection<Reward>): Double {
        val weight = this.getEffectiveWeight(player)
        val totalWeight = among.sumOf { it.getEffectiveWeight(player) }

        if (totalWeight <= 0.0) {
            return 0.0
        }

        return (weight / totalWeight) * 100
    }

    private fun formatChance(chance: Double): String {
        val places = plugin.configYml.getInt("chance-decimal-places").coerceAtLeast(0)
        return String.format(Locale.ROOT, "%.${places}f", chance)
    }

    init {
        PlayerPlaceholder(
            plugin,
            "${id}_wins",
        ) { getWins(it).toString() }.register()

        PlayerlessPlaceholder(
            plugin,
            "${id}_total",
        ) { getTotalWins().toString() }.register()
    }

    /**
     * Give this reward, attributing it to a source (a crate or an envoy category)
     * by display name and id, for the win-effect placeholders.
     *
     * [extraPlaceholders] lets the caller inject source-specific values - envoys
     * pass their rarity through here so a reward's own win-effects can use
     * %envoy_rarity% alongside %reward% and %player%.
     */
    fun giveTo(
        player: Player,
        sourceName: String,
        sourceId: String,
        extraPlaceholders: List<NamedValue> = emptyList()
    ) {
        winEffects?.trigger(
            TriggerData(player = player)
                .dispatch(player.toDispatcher())
                .apply {
                    addPlaceholders(
                        listOf(
                            NamedValue("reward", name),
                            NamedValue("reward_id", id),
                            NamedValue("crate", sourceName),
                            NamedValue("crate_id", sourceId)
                        ) + extraPlaceholders
                    )
                }
        )

        if (maxWins > 0) {
            player.profile.write(winsKey, player.profile.read(winsKey) + 1)
        }

        ServerProfile.load().compute(totalWinsKey) { it + 1 }
    }

    /** [giveTo] attributed to a reward source directly. */
    fun giveTo(player: Player, source: RewardSource) =
        giveTo(player, source.name, source.id)

    /** Kept for binary compatibility with addons compiled against the crate-only signature. */
    fun giveTo(player: Player, crate: Crate) =
        giveTo(player, crate as RewardSource)

    fun getWins(player: OfflinePlayer): Int {
        return if (maxWins > 0) player.profile.read(winsKey) else 0
    }

    fun resetWins(player: OfflinePlayer) {
        if (maxWins > 0) {
            player.profile.write(winsKey, 0)
        }
    }

    fun getTotalWins(): Int = ServerProfile.load().read(totalWinsKey)

    fun resetTotalWins() {
        ServerProfile.load().write(totalWinsKey, 0)
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
