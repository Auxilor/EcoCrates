package com.willfp.ecocrates.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecocrates.event.EnvoyOpenEvent
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.filters.Filter
import com.willfp.libreforge.triggers.TriggerData

/** Libreforge filter `envoy_reward`: [description]. */
object FilterEnvoyReward : Filter<NoCompileData, Collection<String>>("envoy_reward") {
    override val description = "Matches when the reward involved in the triggering event is one of the given reward IDs."

    override val categories = setOf("special")

    override val valueType = ArgType.STRING_LIST

    override val additionalInfo = listOf(
        "Passes automatically if the triggering event is not envoy related."
    )

    override fun getValue(config: Config, data: TriggerData?, key: String): Collection<String> {
        return config.getFormattedStrings(key)
    }

    override fun isMet(data: TriggerData, value: Collection<String>, compileData: NoCompileData): Boolean {
        val event = data.event as? EnvoyOpenEvent ?: return true

        return value.any { id ->
            id.equals(event.reward.id, ignoreCase = true)
        }
    }
}
