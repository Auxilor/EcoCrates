package com.willfp.ecocrates.envoy

import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.Rewards
import com.willfp.ecocrates.util.weightedRandom
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.entity.Player
import wiki.ecohub.config.Config
import wiki.ecohub.config.ViolationContext
import wiki.ecohub.core.stdlib.NamedValue
import wiki.ecohub.game.block.Blocks
import wiki.ecohub.game.block.lookup.TestableBlock
import wiki.ecohub.game.entity.toDispatcher
import wiki.ecohub.game.sound.AbstractPlayableSound
import wiki.ecohub.game.sound.PlayableSound
import wiki.ecohub.rules.chain.NormalExecutorFactory
import wiki.ecohub.rules.effect.Effects
import wiki.ecohub.rules.trigger.TriggerData

/**
 * One rarity of envoy crate within a category. Rarities are not registered
 * globally - they are only unique within their own category, and are addressed
 * as (categoryId, id).
 */
class EnvoyRarity(
    val categoryId: String,
    private val config: Config
) {
    val id = config.getString("id")

    val displayName = config.getFormattedString("display_name")

    val weight = config.getDouble("weight")

    /** Per-player cooldown, in ticks, between collecting crates of this rarity. */
    val collectionCooldown = config.getInt("collection_cooldown")

    val rewards = config.getStrings("rewards").mapNotNull { Rewards[it] }

    val block: TestableBlock = Blocks.lookup(config.getString("block"))

    val hologramMessage = config.getFormattedString("hologram.message")

    val hologramHeight = config.getDouble("hologram.height")

    val itemDisplayEnabled = config.getBool("item-display.enabled")

    val itemDisplayHeight = config.getDouble("item-display.height")

    val itemDisplayDelay = config.getInt("item-display.delay").coerceAtLeast(1)

    val itemDisplayName = config.getFormattedString("item-display.name")

    val fireworks = EnvoyFireworks.fromConfig(config.getSubsectionOrNull("fireworks"))

    /** Played at the crate's location when it spawns. */
    val spawnSound: AbstractPlayableSound<*>? = config.getSubsectionOrNull("spawn-sound")
        ?.let { PlayableSound.create(it) }

    /** Whether crates of this rarity appear on envoy compasses. */
    val showOnCompass = if (config.has("show_on_compass")) {
        config.getBool("show_on_compass")
    } else {
        true
    }

    /**
     * The locator-bar marker colour for this rarity. Null uses the client's
     * default style colour.
     */
    val compassColor: Color? = config.getStringOrNull("compass_color")
        ?.let { EnvoyFireworks.parseColorOrNull(it) }

    private val openEffects = Effects.compileChain(
        config.getSubsections("open-effects"),
        NormalExecutorFactory.create(),
        ViolationContext(plugin, "Envoy $categoryId rarity $id Open Effects")
    )

    init {
        if (rewards.isEmpty()) {
            plugin.logger.warning(
                "Envoy '$categoryId' rarity '$id' has no valid rewards - " +
                    "crates of this rarity will not be able to give anything."
            )
        }
    }

    fun randomReward(player: Player): Reward? =
        rewards.weightedRandom { it.getEffectiveWeight(player) }

    /**
     * Fires this rarity's open-effects chain. This is where the envoy
     * placeholders live: libreforge's registered triggers cannot carry
     * NamedValues, only compiled chains can.
     */
    fun triggerOpenEffects(player: Player, location: Location, reward: Reward) {
        val category = Envoys[categoryId]

        openEffects?.trigger(
            TriggerData(player = player, location = location)
                .dispatch(player.toDispatcher())
                .apply {
                    addPlaceholders(
                        listOf(
                            NamedValue("envoy_category", category?.name ?: categoryId),
                            NamedValue("envoy_rarity", displayName),
                            NamedValue("reward", reward.name),
                            NamedValue("reward_id", reward.id)
                        )
                    )
                }
        )
    }

    override fun toString() = "EnvoyRarity{category=$categoryId,id=$id}"
}
