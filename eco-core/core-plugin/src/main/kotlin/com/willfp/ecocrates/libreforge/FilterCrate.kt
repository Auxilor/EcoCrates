package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.event.CrateEvent
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.filter.Filter
import wiki.ecohub.rules.trigger.TriggerData

object FilterCrate : Filter<NoCompileData, Collection<String>>("crate") {
    override val description = "Matches when the crate involved in the triggering event is one of the given crate IDs."

    override val categories = setOf("inventory")

    override val valueType = ArgType.STRING_LIST

    override val additionalInfo = listOf(
        "Passes automatically if the triggering event is not crate related."
    )

    override fun getValue(config: Config, data: TriggerData?, key: String): Collection<String> {
        return config.getFormattedStrings(key)
    }

    override fun isMet(data: TriggerData, value: Collection<String>, compileData: NoCompileData): Boolean {
        val event = data.event as? CrateEvent ?: return true

        return value.any { id ->
            id.equals(event.crate.id, ignoreCase = true)
        }
    }
}
