package com.willfp.ecocrates.envoy

import wiki.ecohub.config.Config
import wiki.ecohub.config.category.RegistrableCategory
import wiki.ecohub.core.plugin.EcoPlugin
import wiki.ecohub.modules.recipe.Recipes

/** Registry of [EnvoyCategory] configs, loaded from the `envoys/` config folder. */
object Envoys : RegistrableCategory<EnvoyCategory>("envoy", "envoys") {

    override fun clear(plugin: LibreforgePlugin) {
        for (category in registry.values()) {
            category.flare?.recipe?.let { Recipes.unregister(it) }
            category.compass?.recipe?.let { Recipes.unregister(it) }
        }
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(EnvoyCategory(id, config))
    }
}
