package com.willfp.ecocrates.converters.cratereloaded

import com.hazebyte.crate.api.CrateAPI
import com.hazebyte.crate.api.crate.reward.Reward
import com.willfp.eco.core.config.TransientConfig
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecocrates.EcoCratesPlugin
import com.willfp.ecocrates.converters.Converter
import com.willfp.ecocrates.converters.util.ConversionHelpers
import com.willfp.ecocrates.converters.util.toLookupString
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.placed.PlacedCrates
import com.willfp.ecocrates.crate.roll.Rolls
import org.bukkit.configuration.file.YamlConfiguration

@Suppress("UNCHECKED_CAST")
class CrateReloadedConverter(
    private val plugin: EcoCratesPlugin
) : Converter {
    override val id = "CrateReloaded"

    override fun convert() {
        val crateConfig = TransientConfig(
            YamlConfiguration.loadConfiguration(
                CrateAPI.getInstance().dataFolder
                    .resolve("crates")
                    .resolve("crate.yml")
            )
        )

        val newCrates = crateConfig.getKeys(false)
            .map { BuildingCrate(it, crateConfig.getSubsection(it)) }
            .map { convertCrate(it) }

        val crates = plugin.cratesYml.getSubsections("crates").toMutableList()

        crates.addAll(newCrates)

        plugin.cratesYml.set("crates", crates)
        plugin.cratesYml.save()
        plugin.rewardsYml.save()
        plugin.reload()

        CrateAPI.getBlockCrateRegistrar().locations.forEach {
            val id = CrateAPI.getBlockCrateRegistrar().getFirstCrate(it).uuid.split(":")[0]
            val crate = Crates.getByID(id)
            if (crate != null) {
                CrateAPI.getBlockCrateRegistrar().getCrates(it).toList()
                    .forEach { i1 -> CrateAPI.getBlockCrateRegistrar().removeCrate(it, i1) }
                PlacedCrates.setAsCrate(it, crate)
            }
        }
    }

    private fun convertCrate(buildingCrate: BuildingCrate): Config {
        val id = buildingCrate.id

        val crateConfig = ConversionHelpers.createEmptyCrate()

        crateConfig.set("id", id)

        crateConfig.set("name", buildingCrate.config.getString("display-name"))

        val roll = Rolls.getByID(buildingCrate.config.getString("animation").lowercase()) ?: Rolls.CSGO

        crateConfig.set("roll", roll.id)

        val frame = TransientConfig().apply {
            set("tick", 0)
            set("lines", buildingCrate.config.getStrings("holographic"))
        }

        crateConfig.set("placed.hologram.frames", listOf(frame))

        val crateToConvert = CrateAPI.getCrateRegistrar().getCrate(id)

        crateConfig.set("pay-to-open.enabled", crateToConvert.isBuyable)
        crateConfig.set("pay-to-open.price", crateToConvert.cost)

        var row = 2
        var col = 2
        var counter = 1

        val newRewards = mutableListOf<Config>()

        crateToConvert.rewards.forEach {
            val salt = id + "_" + counter
            newRewards.add(convertReward(it, salt, row, col))
            col++
            if (col >= 8) {
                col = 2
                row++
            }
            counter++
        }

        crateConfig.set("rewards", newRewards.map { it.getString("id") })

        crateConfig.set("key", TransientConfig().apply {
            set("item", "tripwire_hook unbreaking:1 hide_enchants name:\"${crateConfig.getString("name")} Key\"")
            set(
                "lore",
                listOf(
                    "&fUse this key to open",
                    "&fthe ${crateConfig.getString("name")}"
                )
            )
        })

        crateConfig.set("keygui", TransientConfig().apply {
            set("enabled", true)
            set("item", "tripwire_hook unbreaking:1 hide_enchants name:\"${crateConfig.getString("name")}\"")
            set(
                "lore", listOf(
                    crateConfig.getString("name"),
                    "&fYou have %keys% keys",
                    "&fGet more at &astore.example.net"
                )
            )
            set("row", 2)
            set("column", 3)
            set("right-click-previews", true)
            set("left-click-opens", true)
            set(
                "shift-left-click-messsage",
                listOf("Buy a ${crateConfig.getString("name")} key here! &astore.example.net")
            )
        })

        crateConfig.set("open", TransientConfig().apply {
            set("messages", listOf("Good luck!"))
            set("broadcasts", listOf("%player%&f is opening the ${crateConfig.getString("name")}!"))
            set("commands", listOf<String>())
            val sound = TransientConfig().apply {
                set("sound", "entity_villager_yes")
                set("volume", 10)
                set("pitch", 1)
            }
            set("sounds", listOf(sound))
        })

        crateConfig.set("finish", TransientConfig().apply {
            set("messages", listOf("You won %reward%&f!"))
            set("broadcasts", listOf("%player%&f won %reward%&f from the ${crateConfig.getString("name")}!"))
            set("commands", listOf<String>())
            val firework = TransientConfig().apply {
                set("power", 2)
                set("type", "ball_large")
                set("colors", listOf("ffffff"))
                set("fade-colors", listOf("ffffff"))
                set("trail", true)
                set("flicker", true)
            }
            set("fireworks", listOf(firework))
            val sound = TransientConfig().apply {
                set("sound", "entity_generic_explode")
                set("volume", 10)
                set("pitch", 1)
            }
            set("sounds", listOf(sound))
        })

        val rewards = plugin.rewardsYml.getSubsections("rewards").toMutableList()

        rewards.addAll(newRewards)

        plugin.rewardsYml.set("rewards", rewards)

        return crateConfig
    }

    private fun convertReward(reward: Reward, salt: String, row: Int, col: Int): Config {
        val resultConfig = ConversionHelpers.createEmptyReward()

        resultConfig.set("id", salt)
        resultConfig.set("commands", reward.commands.map {
            it.replace("/", "")
                .replace("{player}", "%player%")
        })

        resultConfig.set("items", reward.items.map { it.toLookupString() })

        val messages = mutableListOf<String>()

        reward.messages.values.forEach {
            messages.addAll(it)
        }

        resultConfig.set("messages", messages.map { it.replace("{player}", "%player%") })
        resultConfig.set("weight.display", reward.chance)
        resultConfig.set("weight.actual", reward.chance)
        resultConfig.set("display.name", reward.displayItem.itemMeta?.displayName)
        resultConfig.set("display.item", reward.displayItem.toLookupString())
        resultConfig.set("display.lore", reward.displayItem.itemMeta?.lore)
        resultConfig.set("display.row", row)
        resultConfig.set("display.column", col)

        return resultConfig
    }

    private data class BuildingCrate(val id: String, val config: Config)
}
