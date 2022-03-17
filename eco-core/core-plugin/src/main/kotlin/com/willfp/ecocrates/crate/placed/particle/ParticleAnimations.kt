package com.willfp.ecocrates.crate.placed.particle

import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList

@Suppress("UNUSED")
object ParticleAnimations {
    private val BY_ID = HashBiMap.create<String, ParticleAnimation>()

    val SPIRAL: ParticleAnimation = SpiralParticleAnimation()
    val DOUBLE_SPIRAL: ParticleAnimation = DoubleSpiralParticleAnimation()
    val CIRCLE: ParticleAnimation = CircleParticleAnimation()
    val TWIRL: ParticleAnimation = TwirlParticleAnimation()

    /**
     * Get animation matching id.
     *
     * @param id The id to query.
     * @return The matching animation, or null if not found.
     */
    @JvmStatic
    fun getByID(id: String): ParticleAnimation? {
        return BY_ID[id]
    }

    /**
     * Add a new animation.
     *
     * @param animation The animation.
     */
    @JvmStatic
    fun addNewAnimation(animation: ParticleAnimation) {
        BY_ID[animation.id] = animation
    }

    /**
     * List of all registered animations.
     *
     * @return The animations.
     */
    @JvmStatic
    fun values(): List<ParticleAnimation> {
        return ImmutableList.copyOf(BY_ID.values)
    }
}
