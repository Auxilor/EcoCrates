package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.event.EnvoyOpenEvent
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.filter.Filter
import wiki.ecohub.rules.trigger.TriggerData

/** Libreforge filter `envoy_type`: [description]. */
object FilterEnvoyType : Filter<NoCompileData, Collection<String>>("envoy_type") {
    override val description =
        "Matches when the envoy rarity involved in the triggering event is one of the given rarity IDs."

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

        return value.any { it.equals(event.rarity.id, ignoreCase = true) }
    }
}
