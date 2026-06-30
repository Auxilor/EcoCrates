package com.willfp.ecocrates.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecocrates.crate.Crates
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.getFormattedString
import com.willfp.libreforge.getIntFromExpression
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter

object EffectGiveVirtualKey : Effect<NoCompileData>("give_virtual_key") {
    override val description = "Gives the player a number of virtual keys for the specified crate."

    override val categories = setOf("inventory")

    override val parameters = setOf(
        TriggerParameter.PLAYER
    )

    override val arguments = arguments {
        require(
            "crate",
            "You must specify the crate!",
            description = "The ID of the crate to give virtual keys for.",
            type = ArgType.STRING
        )
        require(
            "amount",
            "You must specify the amount!",
            description = "The number of virtual keys to give.",
            type = ArgType.EXPRESSION
        )
    }

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val player = data.player ?: return false

        val crate = Crates[config.getFormattedString("crate", data)] ?: return false

        val amount = config.getIntFromExpression("amount", data)

        crate.adjustVirtualKeys(player, amount)

        return true
    }
}