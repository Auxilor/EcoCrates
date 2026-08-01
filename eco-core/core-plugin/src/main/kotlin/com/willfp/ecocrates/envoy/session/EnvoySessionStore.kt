package com.willfp.ecocrates.envoy.session

import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.StaticBaseConfig
import com.willfp.ecocrates.envoy.Envoys
import com.willfp.ecocrates.envoy.spawn.envoyLocationFromString
import com.willfp.ecocrates.envoy.spawn.toEnvoyString
import com.willfp.ecocrates.plugin
import java.util.UUID

private object EnvoySessionYml : StaticBaseConfig(
    "envoysession",
    plugin,
    ConfigType.YAML
)

/**
 * Mirrors the live session to disk so a reload or restart mid-session doesn't
 * leave orphaned blocks in the world with nothing tracking them.
 *
 * Layout:
 *   session:
 *     category: demo_envoy
 *     ticks-remaining: 2400
 *     spawns:
 *       - location: world@100,64,-30
 *         rarity: common
 */
object EnvoySessionStore {
    fun save(session: EnvoySession?) {
        if (session == null) {
            clear()
            return
        }

        EnvoySessionYml.set("session.category", session.category.id)
        EnvoySessionYml.set("session.ticks-remaining", session.ticksRemaining)
        EnvoySessionYml.set("session.total-spawned", session.totalSpawned)
        EnvoySessionYml.set(
            "session.spawns",
            session.spawns.map {
                mapOf(
                    "location" to it.blockLocation.toEnvoyString(),
                    "rarity" to it.rarity.id
                )
            }
        )

        // Collection counts drive the top-collector placeholder and the
        // end-effects, so a reload mid-session must not reset the scoreboard.
        for ((uuid, amount) in session.collectionEntries()) {
            EnvoySessionYml.set("session.collections.$uuid", amount)
        }

        EnvoySessionYml.save()
    }

    fun clear() {
        EnvoySessionYml.set("session", null)
        EnvoySessionYml.save()
    }

    /**
     * Rebuilds a session from disk without touching the world. The caller is
     * responsible for placing the blocks and starting the visuals.
     *
     * Spawns whose category or rarity no longer exists are dropped with a
     * warning, since there's nothing sensible left to give for them.
     */
    fun load(): EnvoySession? {
        val categoryId = EnvoySessionYml.getStringOrNull("session.category") ?: return null
        val category = Envoys[categoryId]

        if (category == null) {
            plugin.logger.warning(
                "Saved envoy session references unknown envoy '$categoryId' - discarding it."
            )
            clear()
            return null
        }

        val session = EnvoySession(category, EnvoySessionYml.getInt("session.ticks-remaining"))
        session.totalSpawned = EnvoySessionYml.getInt("session.total-spawned")

        EnvoySessionYml.getSubsectionOrNull("session.collections")?.let { collections ->
            for (key in collections.getKeys(false)) {
                val uuid = runCatching { UUID.fromString(key) }.getOrNull() ?: continue
                session.restoreCollection(uuid, collections.getInt(key))
            }
        }

        for (entry in EnvoySessionYml.getSubsections("session.spawns")) {
            val location = envoyLocationFromString(entry.getString("location"))

            if (location == null) {
                plugin.logger.warning(
                    "Saved envoy session has a spawn in an unloaded or unknown world - skipping it."
                )
                continue
            }

            val rarityId = entry.getString("rarity")
            val rarity = category.getRarity(rarityId)

            if (rarity == null) {
                plugin.logger.warning(
                    "Saved envoy session references unknown rarity '$rarityId' " +
                        "in envoy '$categoryId' - skipping that crate."
                )
                continue
            }

            session.addSpawn(SpawnedEnvoy(category, rarity, location))
        }

        return session
    }
}
