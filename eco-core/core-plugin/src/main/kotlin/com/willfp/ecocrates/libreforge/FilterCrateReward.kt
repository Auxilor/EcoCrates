package com.willfp.ecocrates.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecocrates.event.CrateRewardEvent
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.filters.Filter
import com.willfp.libreforge.triggers.TriggerData

object FilterCrateReward : Filter<NoCompileData, Collection<String>>("crate_reward") {
    override fun getValue(config: Config, data: TriggerData?, key: String): Collection<String> {
        return config.getFormattedStrings(key)
    }

    override fun isMet(data: TriggerData, value: Collection<String>, compileData: NoCompileData): Boolean {
        val event = data.event as? CrateRewardEvent ?: return true

        return value.any { id ->
            id.equals(event.crate.id, ignoreCase = true)
        }
    }
}
