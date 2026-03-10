package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.event.CrateRewardEvent
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.event.EventHandler

object TriggerCrateWin : Trigger("crate_win") {
    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.EVENT,
        TriggerParameter.VALUE
    )

    @EventHandler
    fun CrateRewardEvent.handle() {
        dispatch(
            player.toDispatcher(),
            TriggerData(
                player = player,
                event = this,
                value = reward.getPercentageChance(player, crate.rewards)
            )
        )
    }
}
