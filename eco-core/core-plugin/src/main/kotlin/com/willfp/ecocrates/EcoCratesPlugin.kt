package com.willfp.ecocrates

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecocrates.commands.CommandEcoCrates
import com.willfp.ecocrates.crate.placed.CrateDisplay
import com.willfp.ecocrates.crate.placed.PlacedCrates
import com.willfp.ecocrates.util.PlacedCrateListener
import org.bukkit.event.Listener

class EcoCratesPlugin : EcoPlugin(0, 0, "&#6dd5ed") {
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
            PlacedCrateListener(this)
        )
    }

    companion object {
        /**
         * Instance of the plugin.
         */
        lateinit var instance: EcoCratesPlugin
            private set
    }
}
