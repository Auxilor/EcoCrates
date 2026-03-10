package com.willfp.ecocrates.converters

import com.willfp.eco.core.registry.Registrable

interface Converter : Registrable {
    val id: String
    fun convert()

    override fun getID(): String {
        return id
    }
}
