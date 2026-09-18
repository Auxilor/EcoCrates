package com.willfp.ecocrates.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecocrates.event.PouchOpenEvent
import com.willfp.ecocrates.event.PouchRewardEvent
import com.willfp.ecocrates.pouch.Pouch
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.filters.Filter
import com.willfp.libreforge.triggers.TriggerData

/** The pouch behind a pouch event, or null for any other event. */
internal fun pouchOf(data: TriggerData): Pouch? =
    (data.event as? PouchOpenEvent)?.pouch
        ?: (data.event as? PouchRewardEvent)?.pouch

object FilterPouch : Filter<NoCompileData, Collection<String>>("pouch") {
    override val description = "Matches when the pouch involved in the triggering event is one of the given pouch IDs."

    override val categories = setOf("inventory")

    override val valueType = ArgType.STRING_LIST

    override val additionalInfo = listOf(
        "Passes automatically if the triggering event is not pouch related."
    )

    override fun getValue(config: Config, data: TriggerData?, key: String): Collection<String> {
        return config.getFormattedStrings(key)
    }

    override fun isMet(data: TriggerData, value: Collection<String>, compileData: NoCompileData): Boolean {
        val pouch = pouchOf(data) ?: return true

        return value.any { it.equals(pouch.id, ignoreCase = true) }
    }
}

object FilterPouchRarity : Filter<NoCompileData, Collection<String>>("pouch_rarity") {
    override val description = "Matches when the pouch involved has one of the given rarities (colour codes ignored)."

    override val categories = setOf("inventory")

    override val valueType = ArgType.STRING_LIST

    override val additionalInfo = listOf(
        "Passes automatically if the triggering event is not pouch related."
    )

    override fun getValue(config: Config, data: TriggerData?, key: String): Collection<String> {
        return config.getFormattedStrings(key)
    }

    override fun isMet(data: TriggerData, value: Collection<String>, compileData: NoCompileData): Boolean {
        val pouch = pouchOf(data) ?: return true

        return value.any { it.equals(pouch.plainRarity, ignoreCase = true) }
    }
}
