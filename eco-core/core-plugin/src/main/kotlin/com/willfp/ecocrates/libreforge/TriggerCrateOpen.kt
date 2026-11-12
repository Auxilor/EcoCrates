package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.event.CrateOpenEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import wiki.ecohub.game.entity.toDispatcher
import wiki.ecohub.rules.trigger.Trigger
import wiki.ecohub.rules.trigger.TriggerData
import wiki.ecohub.rules.trigger.TriggerParameter

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
