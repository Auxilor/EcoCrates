package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.event.PouchOpenEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import wiki.ecohub.game.entity.toDispatcher
import wiki.ecohub.rules.trigger.Trigger
import wiki.ecohub.rules.trigger.TriggerData
import wiki.ecohub.rules.trigger.TriggerParameter

object TriggerPouchOpen : Trigger("pouch_open") {
    override val description = "Fires when the player opens a pouch."

    override val categories = setOf("inventory")

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.EVENT
    )

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun PouchOpenEvent.handle() {
        dispatch(
            player.toDispatcher(),
            TriggerData(
                player = player,
                event = this
            )
        )
    }
}
