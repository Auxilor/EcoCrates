package com.willfp.ecocrates

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.integrations.IntegrationLoader
import com.willfp.ecocrates.commands.CommandEcoCrates
import com.willfp.ecocrates.config.CratesYml
import com.willfp.ecocrates.config.RewardsYml
import com.willfp.ecocrates.converters.Converters
import com.willfp.ecocrates.converters.cratereloaded.CrateReloadedConverter
import com.willfp.ecocrates.converters.excellentcrates.ExcellentCratesConverter
import com.willfp.ecocrates.crate.CrateKeyListener
import com.willfp.ecocrates.crate.placed.CrateDisplay
import com.willfp.ecocrates.crate.placed.PlacedCrates
import com.willfp.ecocrates.display.KeyDisplay
import com.willfp.ecocrates.util.PlacedCrateListener
import org.bukkit.event.Listener

class EcoCratesPlugin : EcoPlugin() {
    val cratesYml = CratesYml(this)
    val rewardsYml = RewardsYml(this)

    init {
        instance = this
    }

    override fun handleDisable() {
        PlacedCrates.removeAll()
    }

    override fun handleReload() {
        CrateDisplay(this).start()
    }

    override fun loadPluginCommands(): List<PluginCommand> {
        return listOf(
            CommandEcoCrates(this)
        )
    }

    override fun loadListeners(): List<Listener> {
        return listOf(
            PlacedCrateListener(this),
            CrateKeyListener()
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
            }
        )
    }

    override fun getMinimumEcoVersion(): String {
        return "6.30.0"
    }

    companion object {
        /**
         * Instance of the plugin.
         */
        lateinit var instance: EcoCratesPlugin
            private set
    }
}
