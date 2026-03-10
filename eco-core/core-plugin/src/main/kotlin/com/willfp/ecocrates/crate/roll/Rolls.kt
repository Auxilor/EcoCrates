package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.registry.Registry

object Rolls : Registry<RollFactory<*>>() {
    val CSGO: RollFactory<*> = RollCSGO.Factory
    val FLASH: RollFactory<*> = RollFlash.Factory
    val ENCIRCLE: RollFactory<*> = RollEncircle.Factory
    val QUICK: RollFactory<*> = RollQuick.Factory
    val INSTANT: RollFactory<*> = RollInstant.Factory
    val SEMI_INSTANT: RollFactory<*> = RollSemiInstant.Factory
}
