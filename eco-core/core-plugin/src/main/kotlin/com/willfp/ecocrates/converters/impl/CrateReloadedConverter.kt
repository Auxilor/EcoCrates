package com.willfp.ecocrates.converters.impl

import com.hazebyte.crate.api.CrateAPI
import com.hazebyte.crate.api.crate.reward.Reward
import com.willfp.eco.core.config.BuildableConfig
import com.willfp.eco.core.config.TransientConfig
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.toLookupString
import com.willfp.ecocrates.EcoCratesPlugin
import com.willfp.ecocrates.converters.Converter
import com.willfp.ecocrates.converters.util.ConversionHelpers
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.placed.PlacedCrates
import com.willfp.ecocrates.crate.roll.Rolls
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

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

        for (crate in newCrates) {
            File(plugin.dataFolder, "${crate.id}.yml").writeText(
                crate.config.toPlaintext()
            )
        }

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

    private fun convertCrate(buildingCrate: BuildingCrate): ConvertedCrateConfig {
        val id = buildingCrate.id

        val crateConfig = ConversionHelpers.createEmptyCrate()

        crateConfig.set("name", buildingCrate.config.getString("display-name"))

        val roll = Rolls.getByID(buildingCrate.config.getString("animation").lowercase()) ?: Rolls.CSGO

        crateConfig.set("roll", roll.id)

        val frame = BuildableConfig()
            .add("tick", 0)
            .add("lines", buildingCrate.config.getStrings("holographic"))

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
            newRewards.add(convertReward(it, salt, row, col, crateConfig))
            col++
            if (col >= 8) {
                col = 2
                row++
            }
            counter++
        }

        crateConfig.set("rewards", newRewards.map { it.getString("id") })

        crateConfig.set(
            "key", BuildableConfig()
                .add("item", "tripwire_hook unbreaking:1 hide_enchants name:\"${crateConfig.getString("name")} Key\"")
                .add(
                    "lore",
                    listOf(
                        "&fUse this key to open",
                        "&fthe ${crateConfig.getString("name")}"
                    )
                )
        )

        crateConfig.set(
            "keygui", BuildableConfig()
                .add("enabled", true)
                .add("item", "tripwire_hook unbreaking:1 hide_enchants name:\"${crateConfig.getString("name")}\"")
                .add(
                    "lore", listOf(
                        crateConfig.getString("name"),
                        "&fYou have %keys% keys",
                        "&fGet more at &astore.example.net"
                    )
                )
                .add("row", 2)
                .add("column", 3)
                .add("right-click-previews", true)
                .add("left-click-opens", true)
                .add(
                    "shift-left-click-messsage",
                    listOf("Buy a ${crateConfig.getString("name")} key here! &astore.example.net")
                )
        )

        crateConfig.set(
            "open", BuildableConfig()
                .add("messages", listOf("Good luck!"))
                .add("broadcasts", listOf("%player%&f is opening the ${crateConfig.getString("name")}!"))
                .add("commands", listOf<String>())
                .add(
                    "sounds", listOf(
                        BuildableConfig()
                            .add("sound", "entity_villager_yes")
                            .add("volume", 10)
                            .add("pitch", 1)
                    )
                )
        )

        crateConfig.set(
            "finish", BuildableConfig()
                .add("messages", listOf("You won %reward%&f!"))
                .add("broadcasts", listOf("%player%&f won %reward%&f from the ${crateConfig.getString("name")}!"))
                .add("commands", listOf<String>())
                .add(
                    "fireworks", listOf(
                        BuildableConfig()
                            .add("power", 2)
                            .add("type", "ball_large")
                            .add("colors", listOf("ffffff"))
                            .add("fade-colors", listOf("ffffff"))
                            .add("trail", true)
                            .add("flicker", true)
                    )
                )
                .add(
                    "sounds", listOf(
                        BuildableConfig()
                            .add("sound", "entity_generic_explode")
                            .add("volume", 10)
                            .add("pitch", 1)
                    )
                )
        )

        val rewards = plugin.rewardsYml.getSubsections("rewards").toMutableList()

        rewards.addAll(newRewards)

        plugin.rewardsYml.set("rewards", rewards)

        return ConvertedCrateConfig(id, crateConfig)
    }

    private fun convertReward(reward: Reward, salt: String, row: Int, col: Int, crateConfig: Config): Config {
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

        val rewards = crateConfig.getSubsections("preview.rewards").toMutableList()
        rewards.add(
            BuildableConfig()
                .add("id", salt)
                .add("row", row)
                .add("column", col)
        )
        crateConfig.set("preview.rewards", rewards)

        return resultConfig
    }

    private data class BuildingCrate(val id: String, val config: Config)
}
