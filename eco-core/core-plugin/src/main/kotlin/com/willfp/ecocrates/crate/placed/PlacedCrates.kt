package com.willfp.ecocrates.crate.placed

import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.StaticBaseConfig
import com.willfp.ecocrates.EcoCratesPlugin
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.Crates
import org.bukkit.Bukkit
import org.bukkit.Location

private val plugin = EcoCratesPlugin.instance

private object YamlStorage : StaticBaseConfig(
    "placedcrates",
    plugin,
    ConfigType.YAML
)

private fun Location.toShortString(): String {
    return "${world!!.name.lowercase()}@$blockX,$blockY,$blockZ"
}

private fun locationFromShortString(string: String): Location? {
    val split = string.split("@")
    if (split.size != 2) {
        return null
    }
    val coords = split[1].split(",")
    if (coords.size != 3) {
        return null
    }

    val world = Bukkit.getWorld(split[0]) ?: return null
    val x = coords[0].toIntOrNull() ?: return null
    val y = coords[1].toIntOrNull() ?: return null
    val z = coords[2].toIntOrNull() ?: return null

    return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
}

object PlacedCrates {
    private val loaded = mutableMapOf<Location, PlacedCrate>()

    fun getCrateAt(location: Location): Crate? {
        return loaded[location]?.crate
    }

    fun setAsCrate(location: Location, crate: Crate) {
        loaded[location] = PlacedCrate(crate, location)
        saveCrate(location, crate)
    }

    private fun saveCrate(location: Location, crate: Crate) {
        YamlStorage.set("crates.${location.toShortString()}", crate.id)
        YamlStorage.save()
    }

    fun removeCrate(location: Location) {
        loaded[location]?.onRemove()
        loaded.remove(location)

        YamlStorage.set("crates.${location.toShortString()}", null)
        YamlStorage.save()
    }

    internal fun reload() {
        for ((location, crate) in loaded) {
            saveCrate(location, crate.crate)
        }

        removeAll()

        for (shortString in YamlStorage.getSubsection("crates").getKeys(false)) {
            val location = locationFromShortString(shortString) ?: continue
            val id = YamlStorage.getString("crates.$shortString")
            val crate = Crates.getByID(id) ?: continue
            loaded[location] = PlacedCrate(crate, location)
        }
    }

    internal fun removeAll() {
        for (crate in loaded.values) {
            crate.onRemove()
        }

        loaded.clear()
    }

    fun values(): List<PlacedCrate> {
        return loaded.values.toList()
    }
}
