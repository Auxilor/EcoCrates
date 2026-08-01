package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.event.EnvoyOpenEvent
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.event.EventHandler

object TriggerOpenEnvoy : Trigger("open_envoy") {
    override val description = "Fires when the player collects an envoy crate."

    override val categories = setOf("special")

    override val parameterDescriptions = mapOf(
        TriggerParameter.TEXT to "The ID of the reward the player won."
    )

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.EVENT,
        TriggerParameter.LOCATION,
        TriggerParameter.TEXT
    )

    @EventHandler
    fun EnvoyOpenEvent.handle() {
        dispatch(
            player.toDispatcher(),
            TriggerData(
                player = player,
                event = this,
                location = location,
                text = reward.id
            )
        )
    }
}
