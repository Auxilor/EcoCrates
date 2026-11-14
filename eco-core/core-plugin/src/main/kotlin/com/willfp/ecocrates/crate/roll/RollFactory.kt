package com.willfp.ecocrates.crate.roll

import wiki.ecohub.core.registry.Registrable

abstract class RollFactory<T : Roll>(
    val id: String
) : Registrable {
    /** Whether this roll runs entirely in an inventory GUI, so it needs no world location. */
    open val isGuiRoll: Boolean = false

    init {
        Rolls.register(this)
    }

    abstract fun create(options: RollOptions): T

    override fun getID(): String {
        return id
    }
}
