package com.willfp.ecocrates.envoy

import com.willfp.ecocrates.envoy.session.EnvoySessions
import com.willfp.ecocrates.envoy.session.SpawnedEnvoy
import com.willfp.ecocrates.event.EnvoyOpenEvent
import com.willfp.ecocrates.plugin
import com.willfp.libreforge.NamedValue
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent

object EnvoyListener : Listener {
    @EventHandler
    fun handleClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val block = event.clickedBlock ?: return
        val session = EnvoySessions.active ?: return
        val spawn = session.spawnAt(block.location) ?: return

        event.isCancelled = true

        collect(event.player, spawn)
    }

    /** Envoy crates can't be mined - they're removed by collecting them. */
    @EventHandler
    fun handleBreak(event: BlockBreakEvent) {
        val session = EnvoySessions.active ?: return

        session.spawnAt(event.block.location) ?: return

        event.isCancelled = true
    }

    private fun collect(player: Player, spawn: SpawnedEnvoy) {
        val rarity = spawn.rarity
        val cooldown = EnvoyCooldowns.remainingCollection(player, rarity)

        if (cooldown > 0) {
            player.sendMessage(
                plugin.langYml.getMessage("envoy-on-cooldown")
                    .replace("%seconds%", Math.ceil(cooldown / 20.0).toInt().toString())
                    .withEnvoyPlaceholders(spawn.category, rarity)
            )
            return
        }

        val reward = rarity.randomReward(player)

        if (reward == null) {
            player.sendMessage(
                plugin.langYml.getMessage("envoy-no-rewards")
                    .withEnvoyPlaceholders(spawn.category, rarity)
            )
            return
        }

        val event = EnvoyOpenEvent(player, spawn.category, rarity, reward, spawn.centeredLocation)
        Bukkit.getPluginManager().callEvent(event)

        EnvoyCooldowns.applyCollection(player, rarity)
        EnvoySessions.active?.recordCollection(player)

        // Remove the crate before giving the reward so effects that teleport
        // or move the player can't race the despawn. This does not end the
        // session yet, even if it was the last crate - see below.
        val sessionEmptied = EnvoySessions.collect(spawn)

        rarity.triggerOpenEffects(player, spawn.centeredLocation, event.reward)

        // Pass the envoy placeholders down so the reward's OWN win-effects can
        // use %envoy_rarity% the same way they already use %reward%.
        event.reward.giveTo(
            player,
            spawn.category.name,
            spawn.category.id,
            listOf(
                NamedValue("envoy_category", spawn.category.name),
                NamedValue("envoy_rarity", rarity.displayName)
            )
        )

        // Only now, after the collecting player's own effects and reward have
        // gone out, do we let the session's end-effects (the "envoy is over"
        // broadcast) fire - otherwise they'd beat the collector's own message
        // on the final crate.
        if (sessionEmptied) {
            EnvoySessions.end()
        }
    }
}
