package com.willfp.ecocrates.converters.cratereloaded

import com.hazebyte.crate.api.CrateAPI
import com.hazebyte.crate.api.crate.reward.Reward
import com.willfp.eco.core.config.TransientConfig
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecocrates.EcoCratesPlugin
import com.willfp.ecocrates.converters.Converter
import com.willfp.ecocrates.converters.util.CrateSchema
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
                CrateAPI.getInstance()
                    .dataFolder.resolve("crates").resolve("crate.yml")
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
                CrateAPI.getBlockCrateRegistrar().getCrates(it)
                    .forEach { i1 -> CrateAPI.getBlockCrateRegistrar().removeCrate(it, i1) }
                PlacedCrates.setAsCrate(it, crate)
            }
        }
    }

    private class BuildingCrate(val id: String, val config: Config)

    private fun convertCrate(buildingCrate: BuildingCrate): Config {
        val id = buildingCrate.id

        val result = CrateSchema.createDefaultCrate()

        result.set("id", id)

        result.set("name", buildingCrate.config.getString("display-name"))

        val roll = Rolls.getByID(buildingCrate.config.getString("animation").lowercase()) ?: Rolls.CSGO

        result.set("roll", roll.id)

        val frame = TransientConfig().apply {
            this.set("tick", 0)
            this.set("lines", buildingCrate.config.getStrings("holographic"))
        }

        result.set("placed.hologram.frames", mutableListOf(frame))

        val crateToConvert = CrateAPI.getCrateRegistrar().getCrate(id)

        result.set("pay-to-open.enabled", crateToConvert.isBuyable)
        result.set("pay-to-open.price", crateToConvert.cost)

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

        result.set("rewards", newRewards.map { it.getString("id") })

        result.set("key", TransientConfig().apply {
            this.set("item", "tripwire_hook unbreaking:1 hide_enchants name:\"${result.getString("name")} Key\"")
            this.set(
                "lore",
                mutableListOf(
                    "&fUse this key to open",
                    "&fthe ${result.getString("name")}"
                )
            )
        })

        result.set("keygui", TransientConfig().apply {
            this.set("enabled", true)
            this.set("item", "tripwire_hook unbreaking:1 hide_enchants name:\"${result.getString("name")}\"")
            this.set(
                "lore", mutableListOf(
                    result.getString("name"),
                    "&fYou have %keys% keys",
                    "&fGet more at &astore.example.net"
                )
            )
            this.set("row", 2)
            this.set("column", 3)
            this.set("right-click-previews", true)
            this.set("left-click-opens", true)
            this.set(
                "shift-left-click-messsage",
                mutableListOf("Buy a ${result.getString("name")} key here! &astore.example.net")
            )
        })

        result.set("open", TransientConfig().apply {
            this.set("messages", mutableListOf("Good luck!"))
            this.set("broadcasts", mutableListOf("%player%&f is opening the ${result.getString("name")}!"))
            this.set("commands", mutableListOf<String>())
            val sound = TransientConfig().apply {
                this.set("sound", "entity_villager_yes")
                this.set("volume", 10)
                this.set("pitch", 1)
            }
            this.set("sounds", mutableListOf(sound))
        })

        result.set("finish", TransientConfig().apply {
            this.set("messages", mutableListOf("You won %reward%&f!"))
            this.set("broadcasts", mutableListOf("%player%&f won %reward%&f from the ${result.getString("name")}!"))
            this.set("commands", mutableListOf<String>())
            val firework = TransientConfig().apply {
                this.set("power", 2)
                this.set("type", "ball_large")
                this.set("colors", mutableListOf("00ffff", "00ff00"))
                this.set("fade-colors", mutableListOf("ffffff", "999999"))
                this.set("trail", true)
                this.set("flicker", true)
            }
            this.set("fireworks", mutableListOf(firework))
            val sound = TransientConfig().apply {
                this.set("sound", "entity_generic_explode")
                this.set("volume", 10)
                this.set("pitch", 1)
            }
            this.set("sounds", mutableListOf(sound))
        })

        val rewards = plugin.rewardsYml.getSubsections("rewards").toMutableList()

        rewards.addAll(newRewards)

        plugin.rewardsYml.set("rewards", rewards)

        return result
    }

    private fun convertReward(reward: Reward, salt: String, row: Int, col: Int): Config {
        val result = CrateSchema.createDefaultReward()

        result.set("id", salt)
        result.set("commands", reward.commands.map {
            it.replace("/", "")
                .replace("{player}", "%player%")
        })

        result.set("items", reward.items.map { it.toLookupString() })

        val messages = mutableListOf<String>()

        reward.messages.values.forEach {
            messages.addAll(it)
        }

        result.set("messages", messages.map { it.replace("{player}", "%player%") })
        result.set("weight.display", reward.chance)
        result.set("weight.actual", reward.chance)
        result.set("display.name", reward.displayItem.itemMeta?.displayName)
        result.set("display.item", reward.displayItem.toLookupString())
        result.set("display.lore", reward.displayItem.itemMeta?.lore)
        result.set("display.row", row)
        result.set("display.column", col)

        return result
    }
}
