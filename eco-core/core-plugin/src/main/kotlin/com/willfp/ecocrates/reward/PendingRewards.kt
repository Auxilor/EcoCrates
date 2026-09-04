package com.willfp.ecocrates.reward

import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.Crates
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

    fun queue(player: OfflinePlayer, crate: Crate, reward: Reward) {
        player.profile.write(pendingKey, player.profile.read(pendingKey) + "${crate.id}:${reward.id}")
    }

    fun grantPending(player: Player) {
        val pending = player.profile.read(pendingKey)

        if (pending.isEmpty()) {
            return
        }

        player.profile.write(pendingKey, emptyList())

        for (entry in pending) {
            val crate = Crates.getByID(entry.substringBefore(':')) ?: continue
            val reward = Rewards.getByID(entry.substringAfter(':')) ?: continue

            crate.handleFinish(player, reward)

            player.sendMessage(
                plugin.langYml.getMessage("offline-reward-received")
                    .replace("%reward%", reward.name)
                    .replace("%crate%", crate.name)
            )
        }
    }
}
