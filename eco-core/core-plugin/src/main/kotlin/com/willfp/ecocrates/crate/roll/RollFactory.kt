package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.registry.Registrable

abstract class RollFactory<T : Roll>(
    val id: String
) : Registrable {
    /** Whether this roll runs entirely in an inventory GUI, so it needs no world location. */
    open val isGuiRoll: Boolean = false

    /** The most rewards one open of this roll can give. [Int.MAX_VALUE] means unlimited. */
    open val maxRewards: Int = 1

    init {
        Rolls.register(this)
    }

    abstract fun create(options: RollOptions): T

    override fun getID(): String {
        return id
    }
}
