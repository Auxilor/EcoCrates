package com.willfp.ecocrates.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecocrates.pouch.Pouches
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.getFormattedString
import com.willfp.libreforge.getIntFromExpression
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter

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
        require(
            "amount",
            "You must specify the amount!",
            description = "The number of pouches to give.",
            type = ArgType.EXPRESSION,
            example = "1"
        )
    }

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val player = data.player ?: return false

        val pouch = Pouches[config.getFormattedString("id", data)] ?: return false

        val amount = config.getIntFromExpression("amount", data)

        if (amount <= 0) {
            return false
        }

        pouch.item.give(player, amount)

        return true
    }
}
