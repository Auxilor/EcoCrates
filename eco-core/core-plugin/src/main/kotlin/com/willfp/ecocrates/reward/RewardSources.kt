package com.willfp.ecocrates.reward

import com.willfp.ecocrates.crate.Crates

/** Looks up a loaded reward source by its type and ID. */
object RewardSources {
    fun resolve(sourceType: String, id: String): RewardSource? =
        when (sourceType) {
            SourceTypes.CRATE -> Crates[id]
            else -> null
        }
}
