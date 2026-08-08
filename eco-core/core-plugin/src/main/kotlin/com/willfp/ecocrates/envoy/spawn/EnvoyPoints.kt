package com.willfp.ecocrates.envoy.spawn

import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.StaticBaseConfig
import com.willfp.ecocrates.plugin
import org.bukkit.Bukkit
import org.bukkit.Location

private object EnvoyPointsYml : StaticBaseConfig(
    "envoypoints",
    plugin,
    ConfigType.YAML
)

/**
 * Serialised as `worldname@x,y,z`. Used by the session store, which needs a
 * flat single-string form for its spawn list.
 */
internal fun Location.toEnvoyString(): String =
    "${world!!.name.lowercase()}@$blockX,$blockY,$blockZ"

internal fun envoyLocationFromString(string: String): Location? {
    val split = string.split("@")

    if (split.size != 2) {
        return null
    }

    val coords = split[1].split(",")

    if (coords.size != 3) {
        return null
    }

    val world = Bukkit.getWorlds().firstOrNull { it.name.equals(split[0], true) } ?: return null
    val x = coords[0].toIntOrNull() ?: return null
    val y = coords[1].toIntOrNull() ?: return null
    val z = coords[2].toIntOrNull() ?: return null

    return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
}

/**
 * Spawn points for points-mode envoy categories. Kept out of the category
 * configs so that /ecocrates reload (and shipping a new default config)
 * can never wipe points an admin has set.
 *
 * Layout matches the reference category yml: category -> world -> number ->
 * x/y/z. Numbers are unique across all worlds within a category, so the
 * unset command only needs a number.
 */
object EnvoyPoints {
    /** Adds a point, returning the number it was assigned. */
    fun add(categoryId: String, location: Location): Int {
        val world = location.world?.name?.lowercase() ?: return -1
        val used = usedNumbers(categoryId)
        var number = 1

        while (number in used) {
            number++
        }

        val path = "points.$categoryId.$world.$number"
        EnvoyPointsYml.set("$path.x", location.blockX)
        EnvoyPointsYml.set("$path.y", location.blockY)
        EnvoyPointsYml.set("$path.z", location.blockZ)
        EnvoyPointsYml.save()

        return number
    }

    /** Removes a point. Returns false if that number was not set in any world. */
    fun remove(categoryId: String, number: Int): Boolean {
        val categorySection = EnvoyPointsYml.getSubsectionOrNull("points.$categoryId") ?: return false

        for (world in categorySection.getKeys(false)) {
            if (categorySection.getSubsectionOrNull("$world.$number") == null) {
                continue
            }

            EnvoyPointsYml.set("points.$categoryId.$world.$number", null)
            EnvoyPointsYml.save()

            return true
        }

        return false
    }

    /**
     * All set points for a category, by point number. Points in worlds that
     * are not currently loaded are omitted from the result but left in the
     * file, so unloading a world never destroys an admin's points.
     */
    fun get(categoryId: String): Map<Int, Location> {
        val categorySection = EnvoyPointsYml.getSubsectionOrNull("points.$categoryId") ?: return emptyMap()
        val result = mutableMapOf<Int, Location>()

        for (worldName in categorySection.getKeys(false)) {
            val worldSection = categorySection.getSubsectionOrNull(worldName) ?: continue
            val world = Bukkit.getWorlds().firstOrNull { it.name.equals(worldName, true) } ?: continue

            for (key in worldSection.getKeys(false)) {
                val number = key.toIntOrNull() ?: continue
                val point = worldSection.getSubsectionOrNull(key) ?: continue

                result[number] = Location(
                    world,
                    point.getInt("x").toDouble(),
                    point.getInt("y").toDouble(),
                    point.getInt("z").toDouble()
                )
            }
        }

        return result
    }

    fun randomFor(categoryId: String): Location? =
        get(categoryId).values.randomOrNull()

    /**
     * Point numbers already in use for a category, scanned directly from the
     * raw config across every world subsection - including worlds that are
     * not currently loaded. Unlike [get], this must not filter by loaded
     * worlds: [add] uses this to decide the next free number, and a number
     * hidden by an unloaded world being invisible here would let it be
     * handed out twice, corrupting the file.
     */
    private fun usedNumbers(categoryId: String): Set<Int> {
        val categorySection = EnvoyPointsYml.getSubsectionOrNull("points.$categoryId") ?: return emptySet()
        val result = mutableSetOf<Int>()

        for (worldName in categorySection.getKeys(false)) {
            val worldSection = categorySection.getSubsectionOrNull(worldName) ?: continue

            for (key in worldSection.getKeys(false)) {
                key.toIntOrNull()?.let { result.add(it) }
            }
        }

        return result
    }
}
