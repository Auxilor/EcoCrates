package com.willfp.ecocrates.pouch

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.recipe.Recipes
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory

object Pouches : RegistrableCategory<Pouch>("pouch", "pouches") {
    override fun clear(plugin: LibreforgePlugin) {
        for (pouch in registry.values()) {
            pouch.item.recipe?.let { Recipes.unregister(it) }
        }
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(Pouch(id, config))
    }
}
