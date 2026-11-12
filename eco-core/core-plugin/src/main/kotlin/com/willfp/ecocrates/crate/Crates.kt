package com.willfp.ecocrates.crate

import wiki.ecohub.config.Config
import wiki.ecohub.config.category.RegistrableCategory
import wiki.ecohub.core.plugin.EcoPlugin
import wiki.ecohub.modules.recipe.Recipes

object Crates : RegistrableCategory<Crate>("crate", "crates") {

    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(Crate(id, config))
    }
}

object Keys : RegistrableCategory<SharedKey>("key", "keys") {

    override fun clear(plugin: LibreforgePlugin) {
        for (key in registry.values()) {
            key.recipe?.let { Recipes.unregister(it) }
        }
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(SharedKey(id, config))
    }
}
