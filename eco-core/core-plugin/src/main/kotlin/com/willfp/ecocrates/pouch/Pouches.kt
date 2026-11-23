package com.willfp.ecocrates.pouch

import wiki.ecohub.config.Config
import wiki.ecohub.config.category.RegistrableCategory
import wiki.ecohub.core.plugin.EcoPlugin
import wiki.ecohub.modules.recipe.Recipes

object Pouches : RegistrableCategory<Pouch>("pouch", "pouches") {
    override fun clear(plugin: EcoPlugin) {
        for (pouch in registry.values()) {
            pouch.item.recipe?.let { Recipes.unregister(it) }
        }
        registry.clear()
    }

    override fun acceptConfig(plugin: EcoPlugin, id: String, config: Config) {
        registry.register(Pouch(id, config))
    }
}
