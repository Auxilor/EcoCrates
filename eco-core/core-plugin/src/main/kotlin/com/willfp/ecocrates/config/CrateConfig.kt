package com.willfp.ecocrates.config

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.ExtendableConfig

class CrateConfig(
    name: String,
    directory: String,
    plugin: EcoPlugin
) : ExtendableConfig(
    name,
    true,
    plugin,
    plugin::class.java,
    "$directory/",
    ConfigType.YAML
)
