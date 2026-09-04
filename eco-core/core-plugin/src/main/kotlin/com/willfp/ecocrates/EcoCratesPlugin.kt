package com.willfp.ecocrates

import com.willfp.eco.core.bstats.EcoMetricsChart
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.integrations.IntegrationLoader
import com.willfp.ecocrates.commands.CommandEcoCrates
import com.willfp.ecocrates.commands.CommandEcoEnvoy
import com.willfp.ecocrates.converters.Converters
import com.willfp.ecocrates.converters.impl.CrateReloadedConverter
import com.willfp.ecocrates.converters.impl.CrazyCratesConverter
import com.willfp.ecocrates.converters.impl.ExcellentCratesConverter
import com.willfp.ecocrates.converters.impl.SpecializedCratesConverter
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.Keys
import com.willfp.ecocrates.crate.KeyGUI
import com.willfp.ecocrates.crate.placed.CrateDisplay
import com.willfp.ecocrates.crate.placed.PlacedCrates
import com.willfp.ecocrates.crate.placed.particle.ParticleAnimations
import com.willfp.ecocrates.display.KeyDisplay
import com.willfp.ecocrates.envoy.CompassListener
import com.willfp.ecocrates.envoy.EnvoyListener
import com.willfp.ecocrates.envoy.EnvoyPlaceholders
import com.willfp.ecocrates.envoy.Envoys
import com.willfp.ecocrates.envoy.FlareListener
import com.willfp.ecocrates.envoy.compass.EnvoyCompasses
import com.willfp.ecocrates.envoy.session.EnvoySessions
import com.willfp.ecocrates.envoy.session.EnvoyTicker
import com.willfp.ecocrates.libreforge.ConditionEnvoyStarted
import com.willfp.ecocrates.libreforge.EffectEndEnvoy
import com.willfp.ecocrates.libreforge.EffectGiveVirtualKey
import com.willfp.ecocrates.libreforge.EffectResetRewardWins
import com.willfp.ecocrates.libreforge.EffectRewardWeightMultiplier
import com.willfp.ecocrates.libreforge.EffectStartEnvoy
import com.willfp.ecocrates.libreforge.FilterCrate
import com.willfp.ecocrates.libreforge.FilterCrateReward
import com.willfp.ecocrates.libreforge.FilterEnvoyReward
import com.willfp.ecocrates.libreforge.FilterEnvoyType
import com.willfp.ecocrates.libreforge.TriggerCrateOpen
import com.willfp.ecocrates.libreforge.TriggerCrateWin
import com.willfp.ecocrates.libreforge.TriggerOpenEnvoy
import com.willfp.ecocrates.reward.PendingRewards
import com.willfp.ecocrates.reward.Rewards
import com.willfp.ecocrates.util.CrateKeyListener
import com.willfp.ecocrates.util.PlacedCrateListener
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.filters.Filters
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.triggers.Triggers
import org.bukkit.event.Listener

internal lateinit var plugin: EcoCratesPlugin
    private set

/**
 * Plugin entry point. Registers libreforge effects/conditions/filters/triggers,
 * config categories, commands, listeners and display modules on enable, and
 * tears down active envoy state (compasses, sessions) on disable/reload.
 */
class EcoCratesPlugin : LibreforgePlugin() {
    init {
        plugin = this
    }

    override fun handleEnable() {
        Effects.register(EffectRewardWeightMultiplier)
        Effects.register(EffectGiveVirtualKey)
        Effects.register(EffectResetRewardWins)
        Effects.register(EffectStartEnvoy)
        Effects.register(EffectEndEnvoy)
        Conditions.register(ConditionEnvoyStarted)
        Filters.register(FilterCrate)
        Filters.register(FilterCrateReward)
        Filters.register(FilterEnvoyType)
        Filters.register(FilterEnvoyReward)
        Triggers.register(TriggerCrateOpen)
        Triggers.register(TriggerCrateWin)
        Triggers.register(TriggerOpenEnvoy)

        EnvoyPlaceholders.register()
        PendingRewards.register()
    }

    override fun handleDisable() {
        PlacedCrates.removeAll()
        EnvoyCompasses.deactivateAll()
        EnvoySessions.shutdown()
    }

    override fun handleReload() {
        KeyGUI.update()
        PlacedCrates.reload()
        CrateDisplay.start()

        // Rebuild the session against the freshly loaded categories.
        EnvoyCompasses.deactivateAll()
        EnvoySessions.shutdown()
        EnvoySessions.restore()
        EnvoyTicker.start()
    }

    override fun loadConfigCategories(): List<ConfigCategory> {
        return listOf(
            Keys,
            Crates,
            Rewards,
            Envoys
        )
    }

    override fun loadPluginCommands(): List<PluginCommand> {
        return listOf(
            CommandEcoCrates,
            CommandEcoEnvoy
        )
    }

    override fun loadListeners(): List<Listener> {
        return listOf(
            PlacedCrateListener,
            CrateKeyListener,
            EnvoyListener,
            FlareListener,
            CompassListener
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

    override fun getCustomCharts() = listOf(
        EcoMetricsChart.SingleLine("total_crates") { Crates.values().size },
        EcoMetricsChart.SingleLine("total_keys") { Keys.values().size },
        EcoMetricsChart.SingleLine("total_rewards") { Rewards.values().size },
        EcoMetricsChart.SingleLine("total_particle_animations") { ParticleAnimations.values().size },
        EcoMetricsChart.SingleLine("placed_crates") { PlacedCrates.values().size },
        EcoMetricsChart.SingleLine("total_envoys") { Envoys.values().size },
        EcoMetricsChart.SingleLine("active_envoy_crates") { EnvoySessions.remaining() }
    )
}
