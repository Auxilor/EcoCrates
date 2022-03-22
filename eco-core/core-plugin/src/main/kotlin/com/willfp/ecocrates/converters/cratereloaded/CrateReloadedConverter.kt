package com.willfp.ecocrates.converters.cratereloaded

import com.hazebyte.crate.api.CrateAPI
import com.hazebyte.crate.api.crate.reward.Reward
import com.willfp.eco.core.Eco
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecocrates.EcoCratesPlugin
import com.willfp.ecocrates.converters.Converter
import com.willfp.ecocrates.converters.util.ConverterUtils
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.placed.PlacedCrate
import com.willfp.ecocrates.crate.placed.PlacedCrates
import com.willfp.ecocrates.crate.roll.Rolls
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

@Suppress("UNCHECKED_CAST")
class CrateReloadedConverter: Converter {
    override fun convert() {
        val crateConfig = YamlConfiguration.loadConfiguration(CrateAPI.getInstance()
            .dataFolder.resolve("crates").resolve("crate.yml"))

        val crates = crateConfig.getKeys(false).map { ACrate(it, crateConfig.getConfigurationSection(it)!!) }
            .map { convertCrate(it) }

        val preCrates = EcoCratesPlugin.instance.cratesYml.bukkitHandle!!.get("crates")
                as MutableList<YamlConfiguration>

        preCrates.addAll(crates)

        EcoCratesPlugin.instance.cratesYml.set("crates", preCrates)
        EcoCratesPlugin.instance.cratesYml.save()
        EcoCratesPlugin.instance.rewardsYml.save()
        EcoCratesPlugin.instance.reload()
        CrateAPI.getBlockCrateRegistrar().locations.forEach {
            val id = CrateAPI.getBlockCrateRegistrar().getFirstCrate(it).uuid.split(":")[0]
            val crate = Crates.getByID(id)
            if (crate != null) {
                CrateAPI.getBlockCrateRegistrar().getCrates(it)
                    .forEach { i1 -> CrateAPI.getBlockCrateRegistrar().removeCrate(it, i1)}
                PlacedCrates.setAsCrate(it, crate)
            }
        }
    }

    class ACrate(val id: String, val config: ConfigurationSection)

    private fun convertCrate(aCrate: ACrate): YamlConfiguration {

        val id = aCrate.id

        val result = ConverterUtils.createDefaultCrate()

        result.set("id", id)

        result.set("name", aCrate.config.getString("display-name"))

        val roll = Rolls.getByID(aCrate.config.getString("animation")!!.lowercase())?: Rolls.CSGO

        result.set("roll", roll.id)

        val frame = ConverterUtils.emptyBukkit().apply {
            this.set("tick", 0)
            this.set("lines", aCrate.config.getStringList("holographic"))
        }

        result.set("placed.hologram.frames", mutableListOf(frame))

        val handle = CrateAPI.getCrateRegistrar().getCrate(id)

        result.set("pay-to-open.enabled", handle.isBuyable)
        result.set("pay-to-open.price", handle.cost)

        var row = 2
        var col = 2
        var counter = 1

        val rewards = mutableListOf<YamlConfiguration>()

        handle.rewards.forEach {
            val salt = id+"_"+counter
            rewards.add(convertReward(it, salt, row, col))
            col++
            if (col >= 8) {
                col = 2
                row++
            }
            counter++
        }

        result.set("rewards", rewards.map { it.getString("id") })

        result.set("key", ConverterUtils.emptyBukkit().apply {
            this.set("item", "tripwire_hook unbreaking:1 hide_enchants name:\"${result.getString("name")} Key\"")
            this.set("lore",
                mutableListOf(
                    "&fUse this key to open",
                    "&fthe ${result.getString("name")}"
                )
            )
        })

        result.set("keygui", ConverterUtils.emptyBukkit().apply {
            this.set("enabled", true)
            this.set("item", "tripwire_hook unbreaking:1 hide_enchants name:\"${result.getString("name")}\"")
            this.set("lore", mutableListOf(
                "${result.getString("name")}",
                "&fYou have %keys% keys",
                "&fGet more at &astore.example.net"
            ))
            this.set("row", 2)
            this.set("column", 3)
            this.set("right-click-previews", true)
            this.set("left-click-opens", true)
            this.set("shift-left-click-messsage",
                mutableListOf("Buy a ${result.getString("name")} key here! &astore.example.net"))
        })

        result.set("open", ConverterUtils.emptyBukkit().apply {
            this.set("messages", mutableListOf("Good luck!"))
            this.set("broadcasts", mutableListOf("%player%&f is opening the ${result.getString("name")}!"))
            this.set("commands", mutableListOf<String>())
            val sound = ConverterUtils.emptyBukkit().apply {
                this.set("sound", "entity_villager_yes")
                this.set("volume", 10)
                this.set("pitch", 1)
            }
            this.set("sounds", mutableListOf(ConverterUtils.convertToBukkit(sound)))
        })

        result.set("finish", ConverterUtils.emptyBukkit().apply {
            this.set("messages", mutableListOf("You won %reward%&f!"))
            this.set("broadcasts", mutableListOf("%player%&f won %reward%&f from the ${result.getString("name")}!"))
            this.set("commands", mutableListOf<String>())
            val firework = ConverterUtils.emptyBukkit().apply {
                this.set("power", 2)
                this.set("type", "ball_large")
                this.set("colors", mutableListOf("00ffff", "00ff00"))
                this.set("fade-colors", mutableListOf("ffffff", "999999"))
                this.set("trail", true)
                this.set("flicker", true)
            }
            this.set("fireworks", mutableListOf(ConverterUtils.convertToBukkit(firework)))
            val sound = ConverterUtils.emptyBukkit().apply {
                this.set("sound", "entity_generic_explode")
                this.set("volume", 10)
                this.set("pitch", 1)
            }
            this.set("sounds", mutableListOf(ConverterUtils.convertToBukkit(sound)))
        })

        val preRewards = EcoCratesPlugin.instance.rewardsYml.bukkitHandle!!.get("rewards")
                as MutableList<YamlConfiguration>

        preRewards.addAll(rewards)

        EcoCratesPlugin.instance.rewardsYml.set("rewards", preRewards)

        return result
    }

    private fun convertReward(reward: Reward, salt: String, row: Int, col: Int): YamlConfiguration {
        val result = ConverterUtils.createDefaultReward()

        result.set("id", salt)
        result.set("commands", reward.commands.map { it.replace("/", "")
            .replace("{player}", "%player%") })
        result.set("items", reward.items.map { ConverterUtils.itemToStringSimplified(it) })
        val messages = mutableListOf<String>()
        reward.messages.values.forEach {
            messages.addAll(it)
        }
        result.set("messages", messages.map {it.replace("{player}", "%player%") })
        result.set("weight.display", reward.chance)
        result.set("weight.actual", reward.chance)
        result.set("display.name", reward.displayItem.itemMeta?.displayName)
        result.set("display.item", ConverterUtils.itemToStringSimplified(reward.displayItem))
        result.set("display.lore", reward.displayItem.itemMeta?.lore)
        result.set("display.row", row)
        result.set("display.column", col)

        return result
    }

}