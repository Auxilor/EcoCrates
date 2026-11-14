package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.event.EnvoyOpenEvent
import org.bukkit.event.EventHandler
import wiki.ecohub.game.entity.toDispatcher
import wiki.ecohub.rules.trigger.Trigger
import wiki.ecohub.rules.trigger.TriggerData
import wiki.ecohub.rules.trigger.TriggerParameter

/** Libreforge trigger `open_envoy`: [description]. */
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
