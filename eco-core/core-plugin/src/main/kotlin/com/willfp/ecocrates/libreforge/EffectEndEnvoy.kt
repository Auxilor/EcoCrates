package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.envoy.session.EnvoySessions
import wiki.ecohub.config.Config
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.effect.Effect
import wiki.ecohub.rules.trigger.TriggerData

/** Libreforge effect `end_envoy`: [description]. */
object EffectEndEnvoy : Effect<NoCompileData>("end_envoy") {
    override val description = "Ends the currently active envoy session, if there is one."

    override val categories = setOf("special")

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        if (!EnvoySessions.isActive()) {
            return false
        }

        EnvoySessions.end()

        return true
    }
}
