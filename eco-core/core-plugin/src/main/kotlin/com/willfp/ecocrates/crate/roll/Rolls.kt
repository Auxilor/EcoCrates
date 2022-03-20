package com.willfp.ecocrates.crate.roll

import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList

@Suppress("UNUSED")
object Rolls {
    private val BY_ID = HashBiMap.create<String, RollFactory<*>>()

    val CSGO: RollFactory<*> = RollCSGO.Factory
    val FLASH: RollFactory<*> = RollFlash.Factory
    val ENCIRCLE: RollFactory<*> = RollEncircle.Factory
    val QUICK: RollFactory<*> = RollQuick.Factory
    val INSTANT: RollFactory<*> = RollInstant.Factory
    val SEMI_INSTANT: RollFactory<*> = RollSemiInstant.Factory

    /**
     * Get roll factory matching id.
     *
     * @param id The id to query.
     * @return The matching roll factory, or null if not found.
     */
    @JvmStatic
    fun getByID(id: String): RollFactory<*>? {
        return BY_ID[id]
    }

    /**
     * Add new roll factory.
     *
     * @param factory The factory.
     */
    @JvmStatic
    fun addNewFactory(factory: RollFactory<*>) {
        BY_ID[factory.id] = factory
    }

    /**
     * List of all registered roll factories.
     *
     * @return The roll factories.
     */
    @JvmStatic
    fun values(): List<RollFactory<*>> {
        return ImmutableList.copyOf(BY_ID.values)
    }
}
