package com.willfp.ecocrates

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.integrations.IntegrationLoader
import com.willfp.ecocrates.commands.CommandEcoCrates
import com.willfp.ecocrates.converters.Converters
import com.willfp.ecocrates.converters.impl.CrateReloadedConverter
import com.willfp.ecocrates.converters.impl.CrazyCratesConverter
import com.willfp.ecocrates.converters.impl.ExcellentCratesConverter
import com.willfp.ecocrates.converters.impl.SpecializedCratesConverter
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.KeyGUI
import com.willfp.ecocrates.crate.placed.CrateDisplay
import com.willfp.ecocrates.crate.placed.PlacedCrates
import com.willfp.ecocrates.display.KeyDisplay
import com.willfp.ecocrates.libreforge.EffectGiveVirtualKey
import com.willfp.ecocrates.libreforge.EffectResetRewardWins
import com.willfp.ecocrates.libreforge.EffectRewardWeightMultiplier
import com.willfp.ecocrates.libreforge.FilterCrate
import com.willfp.ecocrates.libreforge.FilterCrateReward
import com.willfp.ecocrates.libreforge.TriggerCrateOpen
import com.willfp.ecocrates.libreforge.TriggerCrateWin
import com.willfp.ecocrates.reward.Rewards
import com.willfp.ecocrates.util.CrateKeyListener
import com.willfp.ecocrates.util.PlacedCrateListener
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.filters.Filters
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.triggers.Triggers
import org.bukkit.event.Listener

internal lateinit var plugin: EcoCratesPlugin
    private set

class EcoCratesPlugin : LibreforgePlugin() {
    init {
        plugin = this
    }

    override fun handleEnable() {
        Effects.register(EffectRewardWeightMultiplier)
        Effects.register(EffectGiveVirtualKey)
        Effects.register(EffectResetRewardWins)
        Filters.register(FilterCrate)
        Filters.register(FilterCrateReward)
        Triggers.register(TriggerCrateOpen)
        Triggers.register(TriggerCrateWin)
    }

    override fun handleDisable() {
        PlacedCrates.removeAll()
    }

    override fun handleReload() {
        KeyGUI.update()
        PlacedCrates.reload()
        CrateDisplay.start()
    }

    override fun loadConfigCategories(): List<ConfigCategory> {
        return listOf(
            Crates,
            Rewards
        )
    }

    override fun loadPluginCommands(): List<PluginCommand> {
        return listOf(
            CommandEcoCrates
        )
    }

    override fun loadListeners(): List<Listener> {
        return listOf(
            PlacedCrateListener,
            CrateKeyListener
        )
    }

    override fun loadDisplayModules(): List<DisplayModule> {
        return listOf(
            KeyDisplay
        )
    }

    override fun loadIntegrationLoaders(): MutableList<IntegrationLoader> {
        return mutableListOf(
            IntegrationLoader("CrateReloaded") { Converters.register(CrateReloadedConverter) },
            IntegrationLoader("ExcellentCrates") { Converters.register(ExcellentCratesConverter) },
            IntegrationLoader("CrazyCrates") { Converters.register(CrazyCratesConverter) },
            IntegrationLoader("SpecializedCrates") { Converters.register(SpecializedCratesConverter) }
        )
    }
}
