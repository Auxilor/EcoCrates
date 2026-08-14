package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.registry.Registry

object Rolls : Registry<RollFactory<*>>() {
    val CSGO: RollFactory<*> = RollCSGO.Factory
    val SLOT_MACHINE: RollFactory<*> = RollSlotMachine.Factory
    val FLASH: RollFactory<*> = RollFlash.Factory
    val CYCLE: RollFactory<*> = RollCycle.Factory
    val ENCIRCLE: RollFactory<*> = RollEncircle.Factory
    val ORBIT_COLLAPSE: RollFactory<*> = RollOrbitCollapse.Factory
    val SKY_DROP: RollFactory<*> = RollSkyDrop.Factory
    val VORTEX: RollFactory<*> = RollVortex.Factory
    val ROULETTE: RollFactory<*> = RollRoulette.Factory
    val STRIKE: RollFactory<*> = RollStrike.Factory
    val DELIVERY: RollFactory<*> = RollDelivery.Factory
    val HOLOGRAM: RollFactory<*> = RollHologram.Factory
    val ELIMINATION: RollFactory<*> = RollElimination.Factory
    val PICK: RollFactory<*> = RollPick.Factory
    val CHOOSE: RollFactory<*> = RollChoose.Factory
    val MATCH: RollFactory<*> = RollMatch.Factory
    val QUICK: RollFactory<*> = RollQuick.Factory
    val INSTANT: RollFactory<*> = RollInstant.Factory
    val SEMI_INSTANT: RollFactory<*> = RollSemiInstant.Factory
}
