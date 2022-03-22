package com.willfp.ecocrates.converters

object Converters {
    private val BY_ID = mutableMapOf<String, Converter>()

    @JvmStatic
    fun registerConverter(converter: Converter) {
        BY_ID[converter.id.lowercase()] = converter
    }

    @JvmStatic
    fun unregisterConverter(id: String) {
        BY_ID.remove(id)
    }

    @JvmStatic
    fun getById(id: String): Converter? {
        return BY_ID[id.lowercase()]
    }

    @JvmStatic
    fun values(): List<Converter> {
        return BY_ID.values.toList()
    }
}
