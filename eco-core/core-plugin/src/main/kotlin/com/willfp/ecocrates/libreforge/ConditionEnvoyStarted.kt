package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.envoy.session.EnvoySessions
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.config.arguments
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.condition.Condition
import wiki.ecohub.rules.holder.Dispatcher
import wiki.ecohub.rules.holder.ProvidedHolder

/** Libreforge condition `envoy_started`: [description]. */
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
