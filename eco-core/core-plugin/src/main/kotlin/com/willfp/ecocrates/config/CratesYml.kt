package com.willfp.ecocrates.config

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.BaseConfig
import com.willfp.eco.core.config.ConfigType

class CratesYml(
    plugin: EcoPlugin
) : BaseConfig(
    "crates",
    plugin,
    true,
    ConfigType.YAML
)
