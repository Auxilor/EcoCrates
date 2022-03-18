package com.willfp.ecocrates.reward

import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.updating.ConfigUpdater
import com.willfp.ecocrates.EcoCratesPlugin
import com.willfp.ecocrates.crate.Crates

@Suppress("UNUSED")
object Rewards {
    private val BY_ID = HashBiMap.create<String, Reward>()

    /**
     * Get reward matching id.
     *
     * @param id The id to query.
     * @return The matching reward, or null if not found.
     */
    @JvmStatic
    fun getByID(id: String): Reward? {
        return BY_ID[id]
    }

    /**
     * List of all registered rewards.
     *
     * @return The rewards.
     */
    @JvmStatic
    fun values(): List<Reward> {
        return ImmutableList.copyOf(BY_ID.values)
    }

    @JvmStatic
    @ConfigUpdater
    fun update(plugin: EcoCratesPlugin) {
        BY_ID.clear()

        for (config in plugin.rewardsYml.getSubsections("rewards")) {
            val reward = Reward(plugin, config)
            BY_ID[reward.id] = reward
        }

        Crates.update(plugin)
    }
}
