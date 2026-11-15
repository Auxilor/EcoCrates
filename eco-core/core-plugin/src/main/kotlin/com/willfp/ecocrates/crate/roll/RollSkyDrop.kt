package com.willfp.ecocrates.crate.roll

import com.willfp.eco.util.NumberUtils
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.teleportCompat
import com.willfp.ecocrates.util.RollItems
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

private const val APEX_DISTANCE = 0.2
private const val LANDING_DISTANCE = 0.15
private const val LANDING_SOUND_DISTANCE = 0.7
private const val LANDING_VOLUME = 0.8f
private const val LANDING_PITCH = 1.1f
private const val RISE_PITCH = 0.8f
private const val PAUSE_PITCH = 1.2f
private const val DROP_PITCH = 0.6f
private const val DONE_PITCH = 1.0f

class RollSkyDrop private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    override val reward: Reward = rewards.first()

    private val riseVelocity = plugin.configYml.getDouble("rolls.sky_drop.rise-velocity")
    private val dropVelocity = plugin.configYml.getDouble("rolls.sky_drop.drop-velocity")
    private val launchHeight = plugin.configYml.getDouble("rolls.sky_drop.launch-height")
    private val pauseTime = plugin.configYml.getInt("rolls.sky_drop.pause")
    private val settleTime = plugin.configYml.getInt("rolls.sky_drop.settle")
    private val decoyCount = plugin.configYml.getInt("rolls.sky_drop.decoy-count")
    private val decoySpread = plugin.configYml.getDouble("rolls.sky_drop.decoy-spread")

    // The item is moved by velocity, so it can overshoot its target and orbit it forever.
    // This caps the whole animation so a player can never be stuck mid-open.
    private val timeout = RewardStagger.lockTick(
        index = rewards.size - 1,
        firstLock = plugin.configYml.getInt("rolls.sky_drop.timeout"),
        gap = pauseTime
    )

    private val launches = CopyOnWriteArrayList<Launch>()
    private val decoys = CopyOnWriteArrayList<Item>()

    /**
     * One reward's launch: it leaves the crate on [launchTick], rises to its apex,
     * pauses, and drops onto its own landing point beside the others.
     */
    private inner class Launch(
        private val itemReward: Reward,
        offset: LineOffset,
        private val launchTick: Int
    ) {
        private val apex = location.toVector().add(Vector(offset.x, launchHeight, offset.z))
        private val landingPoint = location.toVector().add(Vector(offset.x, 0.1, offset.z))

        private val rewardItem = AtomicReference<Item?>(null)
        private val state = AtomicReference(SkyDropState.RISE)
        private val timeSpentPaused = AtomicInteger(0)
        private val timeSpentSettling = AtomicInteger(0)
        private val hasPlayedLandingSound = AtomicBoolean(false)

        val isDone: Boolean
            get() = state.get() == SkyDropState.DONE

        @Suppress("DEPRECATION")
        fun spawn() {
            rewardItem.set(
                location.world!!.dropItem(location, itemReward.getDisplay(player, source)).apply {
                    pickupDelay = Int.MAX_VALUE
                    setGravity(false)
                    isCustomNameVisible = true
                    customName = itemReward.displayName
                    RollItems.mark(this)
                }
            )
        }

        /**
         * Moves this launch on by one tick, launching it first once [launchTick] is reached.
         *
         * @return The state after the tick, or null if this launch did not move.
         */
        fun tick(tick: Int): SkyDropState? {
            if (rewardItem.get() == null && tick >= launchTick) {
                spawn()
            }

            val item = rewardItem.get()?.takeUnless { isDone } ?: return null

            if (item.isValid) {
                advance(item)
            } else {
                state.set(SkyDropState.DONE)
            }

            return state.get().takeIf { item.isValid }
        }

        private fun advance(item: Item) {
            when (state.get()) {
                SkyDropState.RISE -> rise(item)
                SkyDropState.PAUSE -> pause()
                SkyDropState.DROP -> drop(item)
                else -> Unit
            }
        }

        private fun rise(item: Item) {
            if (item.location.toVector().distance(apex) <= APEX_DISTANCE) {
                item.teleportCompat(apex.toLocation(item.world))
                item.velocity = Vector(0, 0, 0)
                state.set(SkyDropState.PAUSE)
            } else {
                item.velocity = apex.clone()
                    .subtract(item.location.toVector())
                    .normalize()
                    .multiply(riseVelocity)
            }
        }

        private fun pause() {
            if (timeSpentPaused.incrementAndGet() > pauseTime) {
                state.set(SkyDropState.DROP)
            }
        }

        private fun drop(item: Item) {
            if (item.location.toVector().distance(landingPoint) <= LANDING_DISTANCE) {
                item.teleportCompat(landingPoint.toLocation(item.world))
                item.velocity = Vector(0, 0, 0)

                if (timeSpentSettling.incrementAndGet() > settleTime) {
                    state.set(SkyDropState.DONE)
                }
            } else {
                item.velocity = landingPoint.clone()
                    .subtract(item.location.toVector())
                    .normalize()
                    .multiply(dropVelocity)
            }

            if (item.location.toVector().distance(landingPoint) <= LANDING_SOUND_DISTANCE &&
                hasPlayedLandingSound.compareAndSet(false, true)
            ) {
                player.playSound(player.location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, LANDING_VOLUME, LANDING_PITCH)
            }
        }

        fun remove() {
            rewardItem.get()?.remove()
        }
    }

    override fun roll() {
        val world = location.world!!

        RewardLine.offsets(
            count = rewards.size,
            yaw = player.location.yaw.toDouble(),
            spacing = decoySpread.coerceAtLeast(RewardLine.DEFAULT_SPACING)
        )
            .zip(rewards)
            .forEachIndexed { index, (offset, itemReward) ->
                launches.add(
                    Launch(itemReward, offset, RewardStagger.lockTick(index = index, firstLock = 0, gap = pauseTime))
                )
            }

        launches.first().spawn()

        // Decoys keep their gravity: they burst out of the crate and fall back down.
        for (filler in source.getRandomRewards(player, decoyCount)) {
            val decoy = world.dropItem(location, filler.getDisplay(player, source))

            decoy.pickupDelay = Int.MAX_VALUE
            decoy.velocity = Vector(
                NumberUtils.randFloat(-decoySpread, decoySpread),
                NumberUtils.randFloat(0.25, 0.45),
                NumberUtils.randFloat(-decoySpread, decoySpread)
            )
            RollItems.mark(decoy)
            decoys.add(decoy)
        }

        player.closeInventory()
    }

    override fun tick(tick: Int) {
        launches.mapNotNull { it.tick(tick) }
            .lastOrNull()
            ?.takeIf { tick % 5 == 0 }
            ?.let { player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, pitchFor(it)) }
    }

    private fun pitchFor(state: SkyDropState): Float =
        when (state) {
            SkyDropState.RISE -> RISE_PITCH
            SkyDropState.PAUSE -> PAUSE_PITCH
            SkyDropState.DROP -> DROP_PITCH
            SkyDropState.DONE -> DONE_PITCH
        }

    override fun shouldContinueTicking(tick: Int): Boolean =
        launches.any { !it.isDone } && tick < timeout

    override fun onFinish() {
        launches.forEach { it.remove() }
        decoys.forEach { it.remove() }
    }

    private enum class SkyDropState {
        RISE,
        PAUSE,
        DROP,
        DONE
    }

    object Factory : RollFactory<RollSkyDrop>("sky_drop") {
        override val maxRewards = 5

        override fun create(options: RollOptions): RollSkyDrop =
            RollSkyDrop(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
