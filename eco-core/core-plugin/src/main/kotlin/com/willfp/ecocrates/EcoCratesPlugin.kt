package com.willfp.ecocrates

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.TransientConfig
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.integrations.IntegrationLoader
import com.willfp.ecocrates.commands.CommandEcoCrates
import com.willfp.ecocrates.config.CrateConfig
import com.willfp.ecocrates.config.RewardsYml
import com.willfp.ecocrates.converters.Converters
import com.willfp.ecocrates.converters.impl.CrateReloadedConverter
import com.willfp.ecocrates.converters.impl.CrazyCratesConverter
import com.willfp.ecocrates.converters.impl.ExcellentCratesConverter
import com.willfp.ecocrates.converters.impl.SpecializedCratesConverter
import com.willfp.ecocrates.crate.CrateKeyListener
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.placed.CrateDisplay
import com.willfp.ecocrates.crate.placed.HologramBugFixer
import com.willfp.ecocrates.crate.placed.PlacedCrates
import com.willfp.ecocrates.display.KeyDisplay
import com.willfp.ecocrates.reward.Rewards
import com.willfp.ecocrates.util.PlacedCrateListener
import org.bukkit.event.Listener
import java.io.File
import java.util.zip.ZipFile

class EcoCratesPlugin : EcoPlugin() {
    val rewardsYml = RewardsYml(this)

    init {
        instance = this
    }

    override fun handleEnable() {
        copyConfigs("crates")
    }

    override fun handleDisable() {
        PlacedCrates.removeAll()
    }

    override fun handleReload() {
        // Extra reload
        this.scheduler.runLater(2) {
            Rewards.update(this)
            Crates.update(this)
        }

        CrateDisplay(this).start()
    }

    private fun getDefaultConfigNames(directory: String): Collection<String> {
        val files = mutableListOf<String>()

        try {
            for (entry in ZipFile(this.file).entries().asIterator()) {
                if (entry.name.startsWith("$directory/")) {
                    files.add(entry.name.removePrefix("$directory/"))
                }
            }
        } catch (_: Exception) {
            // Sometimes, ZipFile likes to completely fail. No idea why, but here's the 'solution'!
        }

        files.removeIf { !it.endsWith(".yml") }
        files.replaceAll { it.replace(".yml", "") }

        return files
    }

    fun copyConfigs(directory: String) {
        val folder = File(this.dataFolder, directory)
        if (!folder.exists()) {
            val files = getDefaultConfigNames(directory)

            for (configName in files) {
                CrateConfig(configName, directory, this)
            }
        }
    }

    fun getCrateConfigs(directory: String): Map<String, Config> {
        val configs = mutableMapOf<String, Config>()

        for (file in File(this.dataFolder, directory).walk()) {
            if (file.nameWithoutExtension == "_example") {
                continue
            }

            if (!file.name.endsWith(".yml")) {
                continue
            }

            val id = file.nameWithoutExtension
            val config = TransientConfig(file, ConfigType.YAML)
            configs[id] = config
        }

        return configs
    }

    override fun loadPluginCommands(): List<PluginCommand> {
        return listOf(
            CommandEcoCrates(this)
        )
    }

    override fun loadListeners(): List<Listener> {
        return listOf(
            PlacedCrateListener(this),
            CrateKeyListener(),
            HologramBugFixer
        )
    }

    override fun createDisplayModule(): DisplayModule {
        return KeyDisplay(this)
    }

    override fun loadIntegrationLoaders(): MutableList<IntegrationLoader> {
        return mutableListOf(
            IntegrationLoader("CrateReloaded") {
                Converters.registerConverter(CrateReloadedConverter(this))
            },
            IntegrationLoader("ExcellentCrates") {
                Converters.registerConverter(ExcellentCratesConverter(this))
            },
            IntegrationLoader("CrazyCrates") {
                Converters.registerConverter(CrazyCratesConverter(this))
            },
            IntegrationLoader("SpecializedCrates") {
                Converters.registerConverter(SpecializedCratesConverter(this))
            }
        )
    }

    override fun getMinimumEcoVersion(): String {
        return "6.43.2"
    }

    companion object {
        /** Instance of the plugin. */
        lateinit var instance: EcoCratesPlugin
            private set
    }
}
