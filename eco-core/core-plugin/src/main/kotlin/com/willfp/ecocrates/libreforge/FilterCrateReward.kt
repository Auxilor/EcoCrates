package com.willfp.ecocrates.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecocrates.event.CrateRewardEvent
import com.willfp.ecocrates.event.PouchRewardEvent
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.filters.Filter
import com.willfp.libreforge.triggers.TriggerData

object FilterCrateReward : Filter<NoCompileData, Collection<String>>("crate_reward") {
    override val description = "Matches when the reward involved in the triggering event is one of the given reward IDs."

    override val categories = setOf("inventory")

    override val valueType = ArgType.STRING_LIST

    override val additionalInfo = listOf(
        "Passes automatically if the triggering event is not reward related."
    )

    override fun getValue(config: Config, data: TriggerData?, key: String): Collection<String> {
        return config.getFormattedStrings(key)
    }

    override fun isMet(data: TriggerData, value: Collection<String>, compileData: NoCompileData): Boolean {
        val reward = (data.event as? CrateRewardEvent)?.reward
            ?: (data.event as? PouchRewardEvent)?.reward
            ?: return true

        return value.any { id ->
            id.equals(reward.id, ignoreCase = true)
        }
    }
}
