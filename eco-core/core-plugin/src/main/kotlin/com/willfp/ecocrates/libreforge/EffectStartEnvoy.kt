package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.envoy.Envoys
import com.willfp.ecocrates.envoy.session.EnvoySessions
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.config.arguments
import wiki.ecohub.config.section.getFormattedString
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.effect.Effect
import wiki.ecohub.rules.trigger.TriggerData

/** Libreforge effect `start_envoy`: [description]. */
object EffectStartEnvoy : Effect<NoCompileData>("start_envoy") {
    override val description = "Starts an envoy session for the specified envoy category."

    override val categories = setOf("special")

    override val arguments = arguments {
        require(
            "category",
            "You must specify the envoy category!",
            description = "The ID of the envoy to start.",
            type = ArgType.STRING,
            example = "demo_envoy"
        )
    }

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val category = Envoys[config.getFormattedString("category", data)] ?: return false

        return EnvoySessions.start(category)
    }
}
