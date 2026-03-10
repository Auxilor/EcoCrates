package com.willfp.ecocrates.crate.placed.particle

import com.willfp.eco.core.registry.Registry

object ParticleAnimations : Registry<ParticleAnimation>() {
    val SPIRAL: ParticleAnimation = SpiralParticleAnimation
    val DOUBLE_SPIRAL: ParticleAnimation = DoubleSpiralParticleAnimation
    val CIRCLE: ParticleAnimation = CircleParticleAnimation
    val TWIRL: ParticleAnimation = TwirlParticleAnimation
    val TILTED_RINGS: ParticleAnimation = TiltedRingsParticleAnimation
}
