package com.willfp.ecocrates.envoy

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory

object Envoys : RegistrableCategory<EnvoyCategory>("envoy", "envoys") {

    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(EnvoyCategory(id, config))
    }
}
