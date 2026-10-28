package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.event.CrateOpenEvent
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

object TriggerCrateOpen : Trigger("crate_open") {
    override val description = "Fires when the player opens a crate."

    override val categories = setOf("inventory")

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.EVENT
    )

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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
