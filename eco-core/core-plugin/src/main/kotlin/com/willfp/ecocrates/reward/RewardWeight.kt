package com.willfp.ecocrates.reward

import com.willfp.ecocrates.plugin
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * A reward's weight for one player, worked out once so a whole pick or
 * render pass fires one [com.willfp.ecocrates.util.RewardWeightEvent] per reward.
 *
 * @property reward The reward.
 * @property weight The weight from [Reward.getWeight].
 * @property effectiveWeight The weight from [Reward.getEffectiveWeight].
 */
class RewardWeight(
    val reward: Reward,
    val weight: Double,
    val effectiveWeight: Double
)

/**
 * Reward weights for display lore, kept until the next tick so that rendering
 * a page of rewards from one source works out each weight once, not once per slot.
 */
internal object DisplayRewardWeights {
    private data class Key(val player: UUID, val sourceType: String, val sourceId: String)

    private val cache = ConcurrentHashMap<Key, List<RewardWeight>>()

    fun get(player: Player, source: RewardSource): List<RewardWeight> {
        val key = Key(player = player.uniqueId, sourceType = source.sourceType, sourceId = source.id)

        cache[key]?.let { return it }

        return source.getRewardWeights(player).also { weights ->
            cache[key] = weights
            plugin.scheduler.global().run { cache.remove(key) }
        }
    }
}
