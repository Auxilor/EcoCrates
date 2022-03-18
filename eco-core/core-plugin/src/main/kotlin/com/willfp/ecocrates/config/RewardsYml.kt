package com.willfp.ecocrates.config

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.BaseConfig
import com.willfp.eco.core.config.ConfigType

class RewardsYml(
    plugin: EcoPlugin
) : BaseConfig(
    "rewards",
    plugin,
    true,
    ConfigType.YAML
)
