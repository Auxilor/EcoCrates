@file:Suppress("DEPRECATION")

package com.willfp.ecocrates.converters.impl

import com.willfp.ecocrates.converters.Converter
import com.willfp.ecocrates.converters.util.ConversionHelpers
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.plugin
import me.PM2.customcrates.crates.Crate
import me.PM2.customcrates.crates.PlacedCrate
import me.PM2.customcrates.crates.options.rewards.Reward
import org.bukkit.Location
import wiki.ecohub.config.Config
import wiki.ecohub.config.section.BuildableConfig
import wiki.ecohub.game.item.toLookupString
import java.io.File

object SpecializedCratesConverter : Converter {
    override val id: String = "SpecializedCrates"

    override fun convert() {
        val newCrates = Crate.getLoadedCrates().map { convertCrate(it.value) }

        for (crate in newCrates) {
            File(plugin.dataFolder, "${crate.id}.yml").writeText(
                crate.config.toPlaintext()
            )
        }

        plugin.reload()

        val pCrates = PlacedCrate.getPlacedCrates()
            .mapNotNull { PCrate(it.key, Crates.getByID(it.value.crate.name.lowercase()) ?: return@mapNotNull null) }

        val toDelete = ArrayList(PlacedCrate.getPlacedCrates().values)

        toDelete.forEach { it.delete() }

        pCrates.forEach {
            com.willfp.ecocrates.crate.placed.PlacedCrates.setAsCrate(it.location, it.crate)
        }
    }

    private data class PCrate(val location: Location, val crate: com.willfp.ecocrates.crate.Crate)

    private fun convertCrate(crate: Crate): ConvertedCrateConfig {
        val result = ConversionHelpers.createEmptyCrate()

        val id = crate.name.lowercase()

        result["name"] = crate.name
        result["preview.title"] = crate.settings.crateInventoryName
        result["key.item"] = crate.settings.keyItemHandler.item.stack.toLookupString()
        result["key.lore"] = crate.settings.keyItemHandler.item.lore
        result["keygui.item"] = "tripwire_hook unbreaking:1 hide_enchants name:\"${crate.name}\""
        result["keygui.lore"] = mutableListOf(
            "<g:#56ab2f>${crate.name}</g:#a8e063>",
            "&fYou have %keys% keys",
            "&fGet more at &astore.example.net"
        )
        result["keygui.shift-left-click-messsage"] = mutableListOf(
            "Buy a ${crate.name}&r key here! &astore.example.net"
        )
        result["placed.hologram.height"] = crate.settings.hologramOffset
        result["placed.hologram.frames"] = mutableListOf(
            BuildableConfig()
                .add(path = "tick", obj = 0)
                .add(path = "lines", obj = crate.settings.hologram.lines)
        )
        result["open.broadcasts"] = mutableListOf("%player%&f is opening the ${crate.name}!")
        result["finish.broadcasts"] = mutableListOf("%player%&f won %reward%&f from the ${crate.name}!")

        val newRewards = mutableListOf<ConvertedRewardConfig>()

        if (crate.settings.reward != null) {
            var row = 2
            var col = 2
            var counter = 1
            crate.settings.reward.crateRewards.forEach {
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
        }

        return ConvertedCrateConfig(id, result, newRewards)
    }

    private fun convertReward(
        reward: Reward,
        salt: String,
        row: Int,
        col: Int,
        crateConfig: Config
    ): ConvertedRewardConfig {
        val result = ConversionHelpers.createEmptyReward()

        result["commands"] = reward.commands
            .map {
                it.replace(
                    "{name}", "%player%"
                )
            }
        if (reward.isGiveDisplayItem) {
            val item = reward.displayBuilder.stack
            val meta = item.itemMeta!!
            meta.lore = if (reward.isGiveDisplayItemLore) {
                reward.displayBuilder.lore
            } else {
                mutableListOf()
            }
            meta.setDisplayName(reward.displayBuilder.getDisplayName(true))
            result["items"] = mutableListOf(item.toLookupString())
        }

        result["weight.actual"] = reward.chance
        result["weight.display"] = reward.chance

        result["display.name"] = reward.displayBuilder.getDisplayName(false)
        result["display.item"] = reward.displayBuilder.stack.toLookupString()
        result["display.lore"] = reward.displayBuilder.lore

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
