package com.willfp.ecocrates.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecocrates.envoy.session.EnvoySessions
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.Dispatcher
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.ProvidedHolder
import com.willfp.libreforge.arguments
import com.willfp.libreforge.conditions.Condition

object ConditionEnvoyStarted : Condition<NoCompileData>("envoy_started") {
    override val description =
        "Passes while an envoy session is running. Specify a category to only match that envoy."

    override val categories = setOf("special")

    override val arguments = arguments {
        optional(
            "category",
            description = "The ID of the envoy category to check. If omitted, matches any active envoy.",
            type = ArgType.STRING,
            example = "demo_envoy"
        )
    }

    override fun isMet(
        dispatcher: Dispatcher<*>,
        config: Config,
        holder: ProvidedHolder,
        compileData: NoCompileData
    ): Boolean {
        val category = config.getStringOrNull("category")?.takeIf { it.isNotBlank() }

        return EnvoySessions.isActive(category)
    }
}
