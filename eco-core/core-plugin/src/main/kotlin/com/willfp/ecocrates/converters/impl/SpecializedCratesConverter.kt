package com.willfp.ecocrates.converters.impl

import com.willfp.eco.core.config.BuildableConfig
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.toLookupString
import com.willfp.ecocrates.EcoCratesPlugin
import com.willfp.ecocrates.converters.Converter
import com.willfp.ecocrates.converters.util.ConversionHelpers
import com.willfp.ecocrates.crate.Crates
import me.PM2.customcrates.crates.Crate
import me.PM2.customcrates.crates.PlacedCrate
import me.PM2.customcrates.crates.options.rewards.Reward
import org.bukkit.Location
import java.io.File

class SpecializedCratesConverter(private val plugin: EcoCratesPlugin) : Converter {
    override val id: String = "SpecializedCrates"

    override fun convert() {
        val newCrates = Crate.getLoadedCrates().map { convertCrate(it.value) }

        for (crate in newCrates) {
            File(plugin.dataFolder, "${crate.id}.yml").writeText(
                crate.config.toPlaintext()
            )
        }

        plugin.rewardsYml.save()
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

        result.set("name", crate.name)
        result.set("preview.title", crate.settings.crateInventoryName)
        result.set("key.item", crate.settings.keyItemHandler.item.stack.toLookupString())
        result.set("key.lore", crate.settings.keyItemHandler.item.lore)
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
        result.set("placed.hologram.height", crate.settings.hologramOffset)
        result.set(
            "placed.hologram.frames", mutableListOf(
                BuildableConfig()
                    .add("tick", 0)
                    .add("lines", crate.settings.hologram.lines)
            )
        )
        result.set("open.broadcasts", mutableListOf("%player%&f is opening the ${crate.name}!"))
        result.set("finish.broadcasts", mutableListOf("%player%&f won %reward%&f from the ${crate.name}!"))

        if (crate.settings.reward != null) {
            val newRewards = mutableListOf<Config>()
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

            val rewards = plugin.rewardsYml.getSubsections("rewards").toMutableList()

            rewards.addAll(newRewards)

            plugin.rewardsYml.set("rewards", rewards)
            result.set("rewards", newRewards.map { it.getString("id") })
        }

        return ConvertedCrateConfig(id, result)
    }

    private fun convertReward(reward: Reward, salt: String, row: Int, col: Int, crateConfig: Config): Config {
        val result = ConversionHelpers.createEmptyReward()

        result.set("id", salt)

        result.set("commands", reward.commands
            .map {
                it.replace(
                    "{name}", "%player%"
                )
            }
        )
        if (reward.isGiveDisplayItem) {
            val item = reward.displayBuilder.stack
            val meta = item.itemMeta!!
            meta.lore = if (reward.isGiveDisplayItemLore) {
                reward.displayBuilder.lore
            } else {
                mutableListOf()
            }
            meta.setDisplayName(reward.displayBuilder.getDisplayName(true))
            result.set("items", mutableListOf(item.toLookupString()))
        }

        result.set("weight.actual", reward.chance)
        result.set("weight.display", reward.chance)

        result.set("display.name", reward.displayBuilder.getDisplayName(false))
        result.set("display.item", reward.displayBuilder.stack.toLookupString())
        result.set("display.lore", reward.displayBuilder.lore)

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
