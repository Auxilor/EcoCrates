package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.registry.Registrable

abstract class RollFactory<T : Roll>(
    val id: String
) : Registrable {
    init {
        Rolls.register(this)
    }

    abstract fun create(options: RollOptions): T

    override fun getID(): String {
        return id
    }
}
