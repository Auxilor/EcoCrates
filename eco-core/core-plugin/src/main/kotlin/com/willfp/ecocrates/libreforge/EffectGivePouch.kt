package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.pouch.Pouches
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.config.arguments
import wiki.ecohub.config.section.getFormattedString
import wiki.ecohub.config.section.getIntFromExpression
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.effect.Effect
import wiki.ecohub.rules.trigger.TriggerData
import wiki.ecohub.rules.trigger.TriggerParameter

object EffectGivePouch : Effect<NoCompileData>("give_pouch") {
    override val description = "Gives the player a number of pouch items."

    override val categories = setOf("inventory")

    override val parameters = setOf(
        TriggerParameter.PLAYER
    )

    override val arguments = arguments {
        require(
            "id",
            "You must specify the pouch ID!",
            description = "The ID of the pouch to give.",
            type = ArgType.STRING
        )
        optional(
            "amount",
            description = "The number of pouches to give. Defaults to 1.",
            type = ArgType.EXPRESSION,
            example = "1"
        )
    }

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val player = data.player ?: return false

        val pouch = Pouches[config.getFormattedString("id", data)] ?: return false

        val amount = if (config.has("amount")) config.getIntFromExpression("amount", data) else 1

        if (amount <= 0) {
            return false
        }

        pouch.item.give(player, amount)

        return true
    }
}
