package com.willfp.ecocrates.converters.impl

import com.willfp.eco.core.config.BuildableConfig
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.toLookupString
import com.willfp.ecocrates.EcoCratesPlugin
import com.willfp.ecocrates.converters.Converter
import com.willfp.ecocrates.converters.util.ConversionHelpers
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.placed.PlacedCrates
import org.bukkit.Location
import su.nightexpress.excellentcrates.ExcellentCrates
import su.nightexpress.excellentcrates.ExcellentCratesAPI
import su.nightexpress.excellentcrates.api.OpenCostType
import su.nightexpress.excellentcrates.api.crate.ICrate
import su.nightexpress.excellentcrates.api.crate.ICrateReward
import java.io.File

@Suppress("UNCHECKED_CAST")
class ExcellentCratesConverter(private val plugin: EcoCratesPlugin) : Converter {
    override val id = "ExcellentCrates"

    override fun convert() {
        val newCrates = ExcellentCratesAPI.getCrateManager().crates.map { convertCrate(it) }

        for (crate in newCrates) {
            File(plugin.dataFolder, "${crate.id}.yml").writeText(
                crate.config.toPlaintext()
            )
        }

        plugin.rewardsYml.save()
        plugin.reload()

        ExcellentCratesAPI.getCrateManager().crates.forEach {
            val jank = mutableListOf<Location>()
            jank.addAll(it.blockLocations)
            jank.forEach { it1 -> it.removeBlockLocation(it1) }
            jank.forEach { it1 -> PlacedCrates.setAsCrate(it1, Crates.getByID(it.id)!!) }
        }
    }

    private fun convertCrate(crate: ICrate): ConvertedCrateConfig {
        val result = ConversionHelpers.createEmptyCrate()

        val id = crate.id

        result.set("name", crate.name)

        if (crate.getOpenCost(OpenCostType.MONEY) > 0.0) {
            result.set("pay-to-open.enabled", true)
            result.set("pay-to-open.price", crate.getOpenCost(OpenCostType.MONEY))
        }

        if (crate.keyIds.isNotEmpty()) {
            val key = ExcellentCratesAPI.getKeyManager().getKeyById(crate.keyIds.first())!!
            result.set("key.item", key.item.toLookupString())
            result.set("key.lore", key.item.itemMeta?.lore)
        } else {
            result.set("key.item", "tripwire_hook unbreaking:1 hide_enchants name:\"${crate.name} Key\"")
            result.set("key.lore", mutableListOf("&fUse this key to open", "&fthe ${crate.name}"))
        }

        result.set("keygui.item", crate.item.toLookupString())
        result.set("keygui.lore", crate.item.itemMeta?.lore)

        result.set("placed.hologram.height", crate.blockHologramOffsetY)
        result.set(
            "placed.hologram.frames", mutableListOf(
                BuildableConfig()
                    .add("tick", 0)
                    .add("lines", crate.blockHologramText)
            )
        )

        result.set("preview.title", crate.name)

        val newRewards = mutableListOf<Config>()
        var row = 2
        var col = 2
        var counter = 1
        crate.rewards.forEach {
            val salt = id + "_" + counter
            newRewards.add(convertReward(it, salt, row, col, result))
            col++
            if (col >= 8) {
                col = 2
                row++
            }
            counter++
        }

        val rewards = plugin.rewardsYml.getSubsections("rewards").toMutableList()

        rewards.addAll(newRewards)

        plugin.rewardsYml.set("rewards", rewards)

        result.set("open.broadcasts", mutableListOf("%player%&f is opening the ${crate.name}!"))

        result.set("finish.broadcasts", mutableListOf("%player%&f won %reward%&f from the ${crate.name}!"))

        result.set("rewards", newRewards.map { it.getString("id") })

        return ConvertedCrateConfig(id, result)
    }

    private fun convertReward(reward: ICrateReward, salt: String, row: Int, col: Int, crateConfig: Config): Config {
        val result = ConversionHelpers.createEmptyReward()

        result.set("id", salt)

        result.set("commands", reward.commands.map {
            it.replace("[CONSOLE]", "")
                .replace("[CONSOLE] ", "")
                .replace("[PLAYER]", "")
                .replace("[PLAYER] ", "")
        })
        result.set("items", reward.items.map { it.toLookupString() })

        result.set("max-wins", reward.winLimitAmount)

        result.set("weight.actual", reward.chance)
        result.set("weight.display", reward.chance)

        val meta = reward.preview.itemMeta

        result.set("display.name", reward.name)
        result.set("display.item", reward.preview.toLookupString())
        result.set("display.lore", meta?.lore)

        val rewards = crateConfig.getSubsections("preview.rewards").toMutableList()
        rewards.add(
            BuildableConfig()
                .add("id", salt)
                .add("row", row)
                .add("column", col)
        )
        crateConfig.set("preview.rewards", rewards)

        return result
    }
}