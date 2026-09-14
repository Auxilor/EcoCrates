package com.willfp.ecocrates.reward

import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.ecocrates.plugin
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

object PendingRewards {
    private val pendingKey: PersistentDataKey<List<String>> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("pending_rewards"),
        PersistentDataKeyType.STRING_LIST,
        emptyList()
    )

    fun register(): PersistentDataKey<List<String>> = pendingKey

    fun queue(player: OfflinePlayer, source: RewardSource, reward: Reward) {
        val entry = PendingRewardEntry(source.sourceType, source.id, reward.id).serialize()
        player.profile.write(pendingKey, player.profile.read(pendingKey) + entry)
    }

    fun grantPending(player: Player) {
        val pending = player.profile.read(pendingKey)

        if (pending.isEmpty()) {
            return
        }

        player.profile.write(pendingKey, emptyList())

        for (raw in pending) {
            val entry = PendingRewardEntry.parse(raw) ?: continue
            val source = RewardSources.resolve(entry.sourceType, entry.sourceId) ?: continue
            val reward = Rewards.getByID(entry.rewardId) ?: continue

            source.handleFinish(player, reward)

            player.sendMessage(
                plugin.langYml.getMessage("offline-reward-received")
                    .replace("%reward%", reward.name)
                    .replace("%crate%", source.name)
            )
        }
    }
}
