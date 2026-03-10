package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.event.CrateOpenEvent
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.event.EventHandler

object TriggerCrateOpen : Trigger("crate_open") {
    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.EVENT
    )

    @EventHandler
    fun CrateOpenEvent.handle() {
        dispatch(
            player.toDispatcher(),
            TriggerData(
                player = player,
                event = this
            )
        )
    }
}
