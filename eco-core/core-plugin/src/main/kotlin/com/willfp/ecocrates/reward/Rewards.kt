package com.willfp.ecocrates.reward

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.LegacyLocation
import com.willfp.libreforge.loader.configs.RegistrableCategory

object Rewards : RegistrableCategory<Reward>("reward", "rewards") {

    override val legacyLocation = LegacyLocation(
        "rewards.yml",
        "rewards"
    )

    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(Reward(id, config))
    }
}
