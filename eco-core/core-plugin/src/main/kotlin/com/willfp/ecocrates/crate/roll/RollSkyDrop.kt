package com.willfp.ecocrates.crate.roll

import com.willfp.eco.util.NumberUtils
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector

class RollSkyDrop private constructor(
    override val reward: Reward,
    override val crate: Crate,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    private val riseVelocity = plugin.configYml.getDouble("rolls.sky_drop.rise-velocity")
    private val dropVelocity = plugin.configYml.getDouble("rolls.sky_drop.drop-velocity")
    private val launchHeight = plugin.configYml.getDouble("rolls.sky_drop.launch-height")
    private val pauseTime = plugin.configYml.getInt("rolls.sky_drop.pause")
    private val settleTime = plugin.configYml.getInt("rolls.sky_drop.settle")
    private val decoyCount = plugin.configYml.getInt("rolls.sky_drop.decoy-count")
    private val decoySpread = plugin.configYml.getDouble("rolls.sky_drop.decoy-spread")

    // The item is moved by velocity, so it can overshoot its target and orbit it forever.
    // This caps the whole animation so a player can never be stuck mid-open.
    private val timeout = plugin.configYml.getInt("rolls.sky_drop.timeout")

    private val apex = location.toVector().add(Vector(0.0, launchHeight, 0.0))
    private val landingPoint = location.toVector().add(Vector(0.0, 0.1, 0.0))

    private lateinit var rewardItem: Item
    private val decoys = mutableListOf<Item>()

    private var state = SkyDropState.RISE
    private var timeSpentPaused = 0
    private var timeSpentSettling = 0
    private var hasPlayedLandingSound = false

    @Suppress("DEPRECATION")
    override fun roll() {
        val world = location.world!!

        rewardItem = world.dropItem(location, reward.getDisplay(player, crate))
        rewardItem.pickupDelay = Int.MAX_VALUE
        rewardItem.setGravity(false)
        rewardItem.isCustomNameVisible = true
        rewardItem.customName = reward.displayName
        rewardItem.setMetadata("ecocrates-roll-item", plugin.metadataValueFactory.create(true))

        // Decoys keep their gravity: they burst out of the crate and fall back down.
        for (filler in crate.getRandomRewards(player, decoyCount)) {
            val decoy = world.dropItem(location, filler.getDisplay(player, crate))

            decoy.pickupDelay = Int.MAX_VALUE
            decoy.velocity = Vector(
                NumberUtils.randFloat(-decoySpread, decoySpread),
                NumberUtils.randFloat(0.25, 0.45),
                NumberUtils.randFloat(-decoySpread, decoySpread)
            )
            decoy.setMetadata("ecocrates-roll-item", plugin.metadataValueFactory.create(true))
            decoys.add(decoy)
        }

        player.closeInventory()
    }

    override fun tick(tick: Int) {
        if (!rewardItem.isValid) {
            state = SkyDropState.DONE
            return
        }

        when (state) {
            SkyDropState.RISE -> {
                if (rewardItem.location.toVector().distance(apex) <= 0.2) {
                    rewardItem.teleport(apex.toLocation(rewardItem.world))
                    rewardItem.velocity = Vector(0, 0, 0)
                    state = SkyDropState.PAUSE
                } else {
                    rewardItem.velocity = apex.clone()
                        .subtract(rewardItem.location.toVector())
                        .normalize()
                        .multiply(riseVelocity)
                }
            }

            SkyDropState.PAUSE -> {
                timeSpentPaused++

                if (timeSpentPaused > pauseTime) {
                    state = SkyDropState.DROP
                }
            }

            SkyDropState.DROP -> {
                if (rewardItem.location.toVector().distance(landingPoint) <= 0.15) {
                    rewardItem.teleport(landingPoint.toLocation(rewardItem.world))
                    rewardItem.velocity = Vector(0, 0, 0)
                    timeSpentSettling++

                    if (timeSpentSettling > settleTime) {
                        state = SkyDropState.DONE
                    }
                } else {
                    rewardItem.velocity = landingPoint.clone()
                        .subtract(rewardItem.location.toVector())
                        .normalize()
                        .multiply(dropVelocity)
                }

                if (!hasPlayedLandingSound && rewardItem.location.toVector().distance(landingPoint) <= 0.7) {
                    player.playSound(player.location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.1f)
                    hasPlayedLandingSound = true
                }
            }

            SkyDropState.DONE -> return
        }

        if (tick % 5 == 0) {
            val pitch = when (state) {
                SkyDropState.RISE -> 0.8f
                SkyDropState.PAUSE -> 1.2f
                SkyDropState.DROP -> 0.6f
                SkyDropState.DONE -> 1.0f
            }

            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, pitch)
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return state != SkyDropState.DONE && tick < timeout
    }

    override fun onFinish() {
        rewardItem.remove()
        decoys.forEach { it.remove() }
    }

    private enum class SkyDropState {
        RISE,
        PAUSE,
        DROP,
        DONE
    }

    object Factory : RollFactory<RollSkyDrop>("sky_drop") {
        override fun create(options: RollOptions): RollSkyDrop =
            RollSkyDrop(
                options.reward,
                options.crate,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
