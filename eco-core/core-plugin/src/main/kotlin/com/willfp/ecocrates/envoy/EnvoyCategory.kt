package com.willfp.ecocrates.envoy

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.ecocrates.envoy.spawn.RadiusBox
import com.willfp.ecocrates.envoy.spawn.SpawnLocationMode
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.util.weightedRandom
import com.willfp.libreforge.GlobalDispatcher
import com.willfp.libreforge.NamedValue
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.effects.executors.impl.NormalExecutorFactory
import com.willfp.libreforge.triggers.TriggerData
import java.util.Objects
import kotlin.random.Random

class EnvoyCategory(
    override val id: String,
    private val config: Config
) : KRegistrable {
    val name = config.getFormattedString("name")

    /** How long a session lasts, in ticks. */
    val duration = config.getInt("duration")

    val schedule = EnvoySchedule.fromConfig(config)

    val locationMode = SpawnLocationMode.fromString(config.getString("location-type"))

    val radiusBox: RadiusBox? = if (locationMode == SpawnLocationMode.RADIUS) {
        RadiusBox.fromConfig(config)
    } else {
        null
    }

    val minSpawns = config.getInt("min-spawns").coerceAtLeast(0)

    val maxSpawns = config.getInt("max-spawns").coerceAtLeast(minSpawns)

    val rarities = config.getSubsections("rarities").map { EnvoyRarity(id, it) }

    init {
        if (rarities.isEmpty()) {
            plugin.logger.warning("Envoy '$id' has no rarities - it will never spawn anything.")
        }

        if (locationMode == SpawnLocationMode.RADIUS && radiusBox == null) {
            plugin.logger.warning(
                "Envoy '$id' uses location-type 'radius' but has no valid " +
                    "radius.center.world - it will never spawn anything."
            )
        }
    }

    /** Total number of crates to spawn for one session. Rolled once, per session. */
    fun rollSpawnCount(random: Random = Random.Default): Int =
        if (maxSpawns <= minSpawns) minSpawns else random.nextInt(minSpawns, maxSpawns + 1)

    fun randomRarity(random: Random = Random.Default): EnvoyRarity? =
        rarities.weightedRandom(random) { it.weight }

    fun getRarity(rarityId: String): EnvoyRarity? =
        rarities.firstOrNull { it.id.equals(rarityId, ignoreCase = true) }

    private val startEffects = Effects.compileChain(
        config.getSubsections("start-effects"),
        NormalExecutorFactory.create(),
        ViolationContext(plugin, "Envoy $id Start Effects")
    )

    private val endEffects = Effects.compileChain(
        config.getSubsections("end-effects"),
        NormalExecutorFactory.create(),
        ViolationContext(plugin, "Envoy $id End Effects")
    )

    /**
     * Session-wide effects have no single player, so they dispatch on
     * GlobalDispatcher. Player-scoped effects like send_message therefore
     * can't be used directly - use `broadcast`, or wrap them in
     * `all_players`, which re-dispatches per online player and carries these
     * placeholders through. Both are shown in the example config.
     */
    fun triggerStartEffects(crateCount: Int) {
        startEffects?.trigger(
            TriggerData()
                .dispatch(GlobalDispatcher)
                .apply {
                    addPlaceholders(
                        listOf(
                            NamedValue("envoy_category", name),
                            NamedValue("envoy_crates", crateCount)
                        )
                    )
                }
        )
    }

    fun triggerEndEffects(
        topCollector: String,
        topCollectorAmount: Int,
        collected: Int,
        remaining: Int
    ) {
        endEffects?.trigger(
            TriggerData()
                .dispatch(GlobalDispatcher)
                .apply {
                    addPlaceholders(
                        listOf(
                            NamedValue("envoy_category", name),
                            NamedValue("envoy_top_collector", topCollector),
                            NamedValue("envoy_top_collector_amount", topCollectorAmount),
                            NamedValue("envoy_crates_collected", collected),
                            NamedValue("envoy_crates_remaining", remaining)
                        )
                    )
                }
        )
    }

    override fun equals(other: Any?) = other is EnvoyCategory && other.id == this.id

    override fun hashCode() = Objects.hash(id)

    override fun toString() = "EnvoyCategory{id=$id}"

    override fun getID() = id
}
