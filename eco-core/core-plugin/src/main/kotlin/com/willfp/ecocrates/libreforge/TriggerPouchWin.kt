package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.event.PouchRewardEvent
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.event.EventHandler

object TriggerPouchWin : Trigger("pouch_win") {
    override val description = "Fires when the player wins a reward from a pouch."

    override val categories = setOf("inventory")

    override val parameterDescriptions = mapOf(
        TriggerParameter.VALUE to "The percentage chance of winning this reward out of all rewards in the pouch."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.EVENT,
        TriggerParameter.VALUE
    )

    @EventHandler
    fun PouchRewardEvent.handle() {
        dispatch(
            player.toDispatcher(),
            TriggerData(
                player = player,
                event = this,
                value = reward.getPercentageChance(player, pouch.rewards)
            )
        )
    }
}
