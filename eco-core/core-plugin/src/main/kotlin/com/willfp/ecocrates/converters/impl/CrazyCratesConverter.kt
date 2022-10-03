package com.willfp.ecocrates.converters.impl

import com.badbones69.crazycrates.api.CrazyManager
import com.badbones69.crazycrates.api.objects.Crate
import com.badbones69.crazycrates.api.objects.Prize
import com.willfp.eco.core.config.BuildableConfig
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.toLookupString
import com.willfp.ecocrates.EcoCratesPlugin
import com.willfp.ecocrates.converters.Converter
import com.willfp.ecocrates.converters.util.ConversionHelpers
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.placed.PlacedCrates
import java.io.File

class CrazyCratesConverter(private val plugin: EcoCratesPlugin) : Converter {
    override val id = "CrazyCrates"

    override fun convert() {
        val newCrates = CrazyManager.getInstance().crates.map { convertCrate(it) }

        for (crate in newCrates) {
            File(plugin.dataFolder, "${crate.id}.yml").writeText(
                crate.config.toPlaintext()
            )
        }

        plugin.rewardsYml.save()
        plugin.reload()

        val jank = ArrayList(CrazyManager.getInstance().crateLocations)

        jank.forEach {
            CrazyManager.getInstance().removeCrateLocation(it.id)
        }

        jank.forEach {
            val crate = Crates.getByID(it.crate.name.lowercase())!!
            PlacedCrates.setAsCrate(it.location, crate)
        }
    }

    private fun convertCrate(crate: Crate): ConvertedCrateConfig {
        val result = ConversionHelpers.createEmptyCrate()

        val id = crate.name.lowercase()

        result.set("name", crate.name)
        result.set("preview.title", crate.crateInventoryName)
        result.set("key.item", crate.key.toLookupString())
        result.set("key.lore", crate.key.itemMeta?.lore)
        result.set("keygui.item", "tripwire_hook unbreaking:1 hide_enchants name:\"${crate.name}\"")
        result.set(
            "keygui.lore", mutableListOf(
                "<g:#56ab2f>${crate.name}</g:#a8e063>",
                "&fYou have %keys% keys",
                "&fGet more at &astore.example.net"
            )
        )
        result.set(
            "keygui.shift-left-click-messsage", mutableListOf(
                "Buy a ${crate.name}&r key here! &astore.example.net"
            )
        )
        result.set("placed.hologram.height", crate.hologram.height)
        result.set(
            "placed.hologram.frames", mutableListOf(
                BuildableConfig()
                    .add("tick", 0)
                    .add("lines", crate.hologram.messages)
            )
        )
        result.set("open.broadcasts", mutableListOf("%player%&f is opening the ${crate.name}!"))
        result.set("finish.broadcasts", mutableListOf("%player%&f won %reward%&f from the ${crate.name}!"))

        val newRewards = mutableListOf<Config>()
        var row = 2
        var col = 2
        var counter = 1
        crate.prizes.forEach {
            val salt = id + "_" + counter
            newRewards.add(convertReward(it, salt, row, col, result))
            col++
            if (col >= 8) {
                col = 2
                row++
            }
            if (row >= 5) {
                row = 2
            }
            counter++
        }

        val rewards = plugin.rewardsYml.getSubsections("rewards").toMutableList()

        rewards.addAll(newRewards)

        plugin.rewardsYml.set("rewards", rewards)
        result.set("rewards", newRewards.map { it.getString("id") })

        return ConvertedCrateConfig(id, result)
    }

    private fun convertReward(reward: Prize, salt: String, row: Int, col: Int, crateConfig: Config): Config {
        val result = ConversionHelpers.createEmptyReward()

        result.set("id", salt)

        result.set("commands", reward.commands)
        result.set("items", reward.items.map { it.toLookupString() })

        result.set("weight.actual", reward.chance)
        result.set("weight.display", reward.chance)

        val meta = reward.displayItem.itemMeta

        result.set("display.name", reward.displayItem.itemMeta?.displayName ?: reward.displayItem.type.name)
        result.set("display.item", reward.displayItem.toLookupString())
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