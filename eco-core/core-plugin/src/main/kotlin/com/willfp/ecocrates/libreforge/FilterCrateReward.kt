package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.event.CrateRewardEvent
import com.willfp.ecocrates.event.PouchRewardEvent
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.filter.Filter
import wiki.ecohub.rules.trigger.TriggerData

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
