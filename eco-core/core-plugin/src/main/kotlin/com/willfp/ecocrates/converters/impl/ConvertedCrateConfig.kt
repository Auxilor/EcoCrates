package com.willfp.ecocrates.converters.impl

import wiki.ecohub.config.Config

data class ConvertedCrateConfig(
    val id: String,
    val config: Config,
    val rewards: List<ConvertedRewardConfig>
)
