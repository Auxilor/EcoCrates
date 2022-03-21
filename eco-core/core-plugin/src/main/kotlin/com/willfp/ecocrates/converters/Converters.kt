package com.willfp.ecocrates.converters

object Converters {

    @JvmStatic
    val BY_ID = mutableMapOf<String, Converter>()

    @JvmStatic
    fun values(): List<Converter> {
        return BY_ID.values.toList()
    }

    @JvmStatic
    fun registerConverter(id: String, converter: Converter) {
        BY_ID[id] = converter
    }

    @JvmStatic
    fun unregisterConverter(id: String) {
        BY_ID.remove(id)
    }

    @JvmStatic
    fun getById(id: String): Converter? {
        return BY_ID.filter { it.key.equals(id, true) }.values.firstOrNull()
    }

}