package com.willfp.ecocrates.crate

import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.TransientConfig
import com.willfp.ecocrates.EcoCratesPlugin
import com.willfp.ecocrates.crate.placed.PlacedCrates
import java.io.File

@Suppress("UNUSED")
object Crates {
    private val BY_ID = HashBiMap.create<String, Crate>()

    /**
     * Get crate matching id.
     *
     * @param id The id to query.
     * @return The matching crate, or null if not found.
     */
    @JvmStatic
    fun getByID(id: String): Crate? {
        return BY_ID[id]
    }

    /**
     * List of all registered crates.
     *
     * @return The crates.
     */
    @JvmStatic
    fun values(): List<Crate> {
        return ImmutableList.copyOf(BY_ID.values)
    }

    @JvmStatic
    internal fun update(plugin: EcoCratesPlugin) {
        BY_ID.clear()

        val cratesYml = TransientConfig(File(plugin.dataFolder, "crates.yml"), ConfigType.YAML)

        for (config in cratesYml.getSubsections("crates")) {
            val crate = Crate(config.getString("id"), config, plugin)
            BY_ID[crate.id] = crate
        }

        for ((id, config) in plugin.getCrateConfigs("crates")) {
            val crate = Crate(id, config, plugin)
            BY_ID[id] = crate
        }

        PlacedCrates.reload()
    }
}
