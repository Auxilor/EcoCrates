package com.willfp.ecocrates.envoy

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.recipe.Recipes
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory

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
