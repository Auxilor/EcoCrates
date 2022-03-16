package com.willfp.ecocrates.crate.roll

abstract class RollFactory<T : Roll>(
    val id: String
) {
    init {
        register()
    }

    private fun register() {
        Rolls.addNewFactory(this)
    }

    abstract fun create(options: RollOptions): T
}
