@file:Suppress("DEPRECATION")

package com.willfp.ecocrates.converters.impl

import com.hazebyte.crate.api.CrateAPI
import com.hazebyte.crate.api.crate.reward.Reward
import com.willfp.ecocrates.converters.Converter
import com.willfp.ecocrates.converters.util.ConversionHelpers
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.placed.PlacedCrates
import com.willfp.ecocrates.crate.roll.Rolls
import com.willfp.ecocrates.plugin
import org.bukkit.configuration.file.YamlConfiguration
import wiki.ecohub.config.Config
import wiki.ecohub.config.section.BuildableConfig
import wiki.ecohub.config.section.TransientConfig
import wiki.ecohub.game.item.toLookupString
import java.io.File

object CrateReloadedConverter : Converter {
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
            File("${plugin.dataFolder}${File.pathSeparator}crates", "${crate.id}.yml").writeText(
                crate.config.toPlaintext()
            )

            for (reward in crate.rewards) {
                File("${plugin.dataFolder}${File.pathSeparator}rewards", "${reward.id}.yml").writeText(
                    reward.config.toPlaintext()
                )
            }
        }

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

        crateConfig["name"] = buildingCrate.config.getString("display-name")

        val roll = Rolls.get(buildingCrate.config.getString("animation").lowercase()) ?: Rolls.CSGO

        crateConfig["roll"] = roll.id

        val frame = BuildableConfig()
            .add(path = "tick", obj = 0)
            .add(path = "lines", obj = buildingCrate.config.getStrings("holographic"))

        crateConfig["placed.hologram.frames"] = listOf(frame)

        val crateToConvert = CrateAPI.getCrateRegistrar().getCrate(id)

        crateConfig["pay-to-open.enabled"] = crateToConvert.isBuyable
        crateConfig["pay-to-open.price"] = crateToConvert.cost

        crateConfig["key"] = BuildableConfig()
            .add(
                path = "item",
                obj = "tripwire_hook unbreaking:1 hide_enchants name:\"${crateConfig.getString("name")} Key\""
            )
            .add(
                path = "lore",
                obj = listOf(
                    "&fUse this key to open",
                    "&fthe ${crateConfig.getString("name")}"
                )
            )

        crateConfig["keygui"] = BuildableConfig()
            .add(path = "enabled", obj = true)
            .add(
                path = "item",
                obj = "tripwire_hook unbreaking:1 hide_enchants name:\"${crateConfig.getString("name")}\""
            )
            .add(
                path = "lore", obj = listOf(
                    crateConfig.getString("name"),
                    "&fYou have %keys% keys",
                    "&fGet more at &astore.example.net"
                )
            )
            .add(path = "row", obj = 2)
            .add(path = "column", obj = 3)
            .add(path = "right-click-previews", obj = true)
            .add(path = "left-click-opens", obj = true)
            .add(
                path = "shift-left-click-messsage",
                obj = listOf("Buy a ${crateConfig.getString("name")} key here! &astore.example.net")
            )

        crateConfig["open"] = BuildableConfig()
            .add(path = "messages", obj = listOf("Good luck!"))
            .add(path = "broadcasts", obj = listOf("%player%&f is opening the ${crateConfig.getString("name")}!"))
            .add(path = "commands", obj = listOf<String>())
            .add(
                path = "sounds", obj = listOf(
                    BuildableConfig()
                        .add(path = "sound", obj = "entity_villager_yes")
                        .add(path = "volume", obj = 10)
                        .add(path = "pitch", obj = 1)
                )
            )

        crateConfig["finish"] = BuildableConfig()
            .add(path = "messages", obj = listOf("You won %reward%&f!"))
            .add(
                path = "broadcasts",
                obj = listOf("%player%&f won %reward%&f from the ${crateConfig.getString("name")}!")
            )
            .add(path = "commands", obj = listOf<String>())
            .add(
                path = "fireworks", obj = listOf(
                    BuildableConfig()
                        .add(path = "power", obj = 2)
                        .add(path = "type", obj = "ball_large")
                        .add(path = "colors", obj = listOf("ffffff"))
                        .add(path = "fade-colors", obj = listOf("ffffff"))
                        .add(path = "trail", obj = true)
                        .add(path = "flicker", obj = true)
                )
            )
            .add(
                path = "sounds", obj = listOf(
                    BuildableConfig()
                        .add(path = "sound", obj = "entity_generic_explode")
                        .add(path = "volume", obj = 10)
                        .add(path = "pitch", obj = 1)
                )
            )

        var row = 2
        var col = 2
        var counter = 1

        val newRewards = mutableListOf<ConvertedRewardConfig>()

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

        crateConfig["rewards"] = newRewards.map { it.id }

        return ConvertedCrateConfig(id, crateConfig, newRewards)
    }

    @Suppress("DEPRECATION")
    private fun convertReward(
        reward: Reward,
        salt: String,
        row: Int,
        col: Int,
        crateConfig: Config
    ): ConvertedRewardConfig {
        val resultConfig = ConversionHelpers.createEmptyReward()

        resultConfig["commands"] = reward.commands.map {
        it.replace("/", "")
            .replace("{player}", "%player%")
    }

        resultConfig["items"] = reward.items.map { it.toLookupString() }

        val messages = mutableListOf<String>()

        reward.messages.values.forEach {
            messages.addAll(it)
        }

        resultConfig["messages"] = messages.map { it.replace("{player}", "%player%") }
        resultConfig["weight.display"] = reward.chance
        resultConfig["weight.actual"] = reward.chance
        resultConfig["display.name"] = reward.displayItem.itemMeta?.displayName
        resultConfig["display.item"] = reward.displayItem.toLookupString()
        resultConfig["display.lore"] = reward.displayItem.itemMeta?.lore

        val rewards = crateConfig.getSubsections("preview.rewards").toMutableList()
        rewards.add(
            BuildableConfig()
                .add(path = "id", obj = salt)
                .add(path = "row", obj = row)
                .add(path = "column", obj = col)
        )
        crateConfig["preview.rewards"] = rewards

        return ConvertedRewardConfig(salt, resultConfig)
    }

    private data class BuildingCrate(val id: String, val config: Config)
}
