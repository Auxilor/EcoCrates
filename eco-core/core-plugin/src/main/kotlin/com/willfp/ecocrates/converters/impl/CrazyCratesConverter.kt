package com.willfp.ecocrates.converters.impl

import com.badbones69.crazycrates.api.CrazyManager
import com.badbones69.crazycrates.api.objects.Crate
import com.badbones69.crazycrates.api.objects.Prize
import com.willfp.ecocrates.converters.Converter
import com.willfp.ecocrates.converters.util.ConversionHelpers
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.placed.PlacedCrates
import com.willfp.ecocrates.plugin
import wiki.ecohub.config.Config
import wiki.ecohub.config.section.BuildableConfig
import wiki.ecohub.game.item.toLookupString
import java.io.File

@Suppress("DEPRECATION")
object CrazyCratesConverter : Converter {
    override val id = "CrazyCrates"

    override fun convert() {
        val newCrates = CrazyManager.getInstance().crates.map { convertCrate(it) }

        for (crate in newCrates) {
            File(plugin.dataFolder, "${crate.id}.yml").writeText(
                crate.config.toPlaintext()
            )
        }

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

        result["name"] = crate.name
        result["preview.title"] = crate.crateInventoryName
        result["key.item"] = crate.key.toLookupString()
        result["key.lore"] = crate.key.itemMeta?.lore
        result["keygui.item"] = "tripwire_hook unbreaking:1 hide_enchants name:\"${crate.name}\""
        result["keygui.lore"] = mutableListOf(
            "<g:#56ab2f>${crate.name}</g:#a8e063>",
            "&fYou have %keys% keys",
            "&fGet more at &astore.example.net"
        )
        result["keygui.shift-left-click-messsage"] = mutableListOf(
            "Buy a ${crate.name}&r key here! &astore.example.net"
        )
        result["placed.hologram.height"] = crate.hologram.height
        result["placed.hologram.frames"] = mutableListOf(
            BuildableConfig()
                .add(path = "tick", obj = 0)
                .add(path = "lines", obj = crate.hologram.messages)
        )
        result["open.broadcasts"] = mutableListOf("%player%&f is opening the ${crate.name}!")
        result["finish.broadcasts"] = mutableListOf("%player%&f won %reward%&f from the ${crate.name}!")

        val newRewards = mutableListOf<ConvertedRewardConfig>()
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

        result["rewards"] = newRewards.map { it.id }

        return ConvertedCrateConfig(id, result, newRewards)
    }

    private fun convertReward(
        reward: Prize,
        salt: String,
        row: Int,
        col: Int,
        crateConfig: Config
    ): ConvertedRewardConfig {
        val result = ConversionHelpers.createEmptyReward()

        result["commands"] = reward.commands
        result["items"] = reward.items.map { it.toLookupString() }

        result["weight.actual"] = reward.chance
        result["weight.display"] = reward.chance

        val meta = reward.displayItem.itemMeta

        result["display.name"] = reward.displayItem.itemMeta?.displayName ?: reward.displayItem.type.name
        result["display.item"] = reward.displayItem.toLookupString()
        result["display.lore"] = meta?.lore

        val rewards = crateConfig.getSubsections("preview.rewards").toMutableList()
        rewards.add(
            BuildableConfig()
                .add(path = "id", obj = salt)
                .add(path = "row", obj = row)
                .add(path = "column", obj = col)
        )
        crateConfig["preview.rewards"] = rewards

        return ConvertedRewardConfig(salt, result)
    }
}