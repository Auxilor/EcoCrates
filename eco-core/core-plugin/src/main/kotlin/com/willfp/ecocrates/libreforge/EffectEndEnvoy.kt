package com.willfp.ecocrates.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecocrates.envoy.session.EnvoySessions
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.triggers.TriggerData

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
