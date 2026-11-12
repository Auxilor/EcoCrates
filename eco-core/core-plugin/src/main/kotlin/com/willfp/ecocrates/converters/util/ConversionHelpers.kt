@file:Suppress("DEPRECATION")

package com.willfp.ecocrates.converters.util

import wiki.ecohub.config.Config
import wiki.ecohub.config.section.TransientConfig

object ConversionHelpers {
    fun createEmptyReward(): Config {
        val result = TransientConfig()

        result["commands"] = mutableListOf<String>()
        result["items"] = mutableListOf("stone")
        result["messages"] = mutableListOf<String>()
        result["weight"] = TransientConfig().apply {
            this["permission-multipliers"] = true
            this["actual"] = 1
            this["display"] = 25
        }
        result["max-wins"] = -1
        result["display"] = TransientConfig().apply {
            this["name"] = "Unknown Reward"
            this["item"] = "stone"
            this["lore"] = emptyList<String>()
        }

        return result
    }

    fun createEmptyCrate(): Config {
        val result = TransientConfig()

        result["name"] = "default"
        result["roll"] = "csgo"
        result["can-reroll"] = false

        val preview = TransientConfig().apply {
            this["title"] = "Default Crate"
            this["rows"] = 6
            val mask = TransientConfig().apply {
                this["items"] = emptyList<String>()
                this["pattern"] = mutableListOf(
                    "000000000",
                    "000000000",
                    "000000000",
                    "000000000",
                    "000000000",
                    "000000000"
                )
            }
            this["mask"] = mask
            this["rewards"] = emptyList<Config>()
        }

        result["preview"] = preview

        val key = TransientConfig().apply {
            this["item"] = "tripwire_hook unbreaking:1 hide_enchants name:\"&aDefault Crate Key\""
            this["lore"] = emptyList<String>()
        }

        result["key"] = key

        val keygui = TransientConfig().apply {
            this["enabled"] = true
            this["item"] = "tripwire_hook unbreaking:1 hide_enchants name:\"Default Crate\""
            this["lore"] = emptyList<String>()
            this["row"] = 2
            this["column"] = 3
            this["right-click-previews"] = true
            this["left-click-opens"] = true
            this["shift-left-click-messsage"] = emptyList<String>()
        }

        result["keygui"] = keygui

        val payToOpen = TransientConfig().apply {
            this["enabled"] = false
            this["price"] = 5000
        }

        result["pay-to-open"] = payToOpen

        val placed = TransientConfig().apply {
            val randomReward = TransientConfig().apply {
                this["enabled"] = true
                this["height"] = 1.5
                this["delay"] = 30
                this["name"] = ""
            }

            this["random-reward"] = randomReward

            val particle = TransientConfig().apply {
                this["particle"] = "flame"
                this["animation"] = "spiral"
            }

            this["particles"] = mutableListOf(particle)

            val hologram = TransientConfig().apply {
                this["height"] = 1.5
                this["ticks"] = 100
                this["frames"] = emptyList<Config>()
            }

            this["hologram"] = hologram
        }

        result["placed"] = placed

        val open = TransientConfig().apply {
            this["messages"] = mutableListOf("Good luck!")
            this["broadcasts"] = mutableListOf("%player%&f is opening the Default Crate!")
            this["commands"] = mutableListOf<String>()
            this["sounds"] = emptyList<Config>()
        }

        result["open"] = open

        val finish = TransientConfig().apply {
            this["messages"] = mutableListOf("You won %reward%&f!")
            this["broadcasts"] = mutableListOf("%player%&f won %reward%&f from the Default Crate!")
            this["commands"] = mutableListOf<String>()
            this["fireworks"] = emptyList<Config>()
            this["sounds"] = emptyList<Config>()
        }

        result["finish"] = finish

        result["rewards"] = emptyList<String>()

        return result
    }
}
