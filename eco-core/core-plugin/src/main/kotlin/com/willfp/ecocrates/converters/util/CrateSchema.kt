package com.willfp.ecocrates.converters.util

import com.willfp.eco.core.config.TransientConfig
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.Items
import org.bukkit.inventory.ItemStack

object CrateSchema {
    fun createDefaultReward(): Config {
        val result = TransientConfig()

        result.set("id", "default")
        result.set("commands", mutableListOf<String>())
        result.set("items", mutableListOf("stone"))
        result.set("messages", mutableListOf<String>())
        result.set("weight",
            TransientConfig().apply {
                this.set("permission-multipliers", true)
                this.set("actual", 1)
                this.set("display", 25)
            }
        )
        result.set("max-wins", -1)
        result.set("display",
            TransientConfig().apply {
                this.set("name", "Unknown Reward")
                this.set("item", "stone")
                this.set("lore", emptyList<String>())
                this.set("row", 1)
                this.set("column", 1)
            }
        )

        return result
    }

    fun createDefaultCrate(): Config {
        val result = TransientConfig()

        result.set("id", "default")
        result.set("name", "default")
        result.set("roll", "csgo")
        result.set("can-reroll", false)

        val preview = TransientConfig().apply {
            this.set("title", "Default Crate")
            this.set("rows", 6)
            val mask = TransientConfig().apply {
                this.set("items", emptyList<String>())
                this.set(
                    "pattern", mutableListOf(
                        "000000000",
                        "000000000",
                        "000000000",
                        "000000000",
                        "000000000",
                        "000000000"
                    )
                )
            }
            this.set("mask", mask)
        }

        result.set("preview", preview)

        val key = TransientConfig().apply {
            this.set("item", "tripwire_hook unbreaking:1 hide_enchants name:\"&aDefault Crate Key\"")
            this.set("lore", emptyList<String>())
        }

        result.set("key", key)

        val keygui = TransientConfig().apply {
            this.set("enabled", true)
            this.set("item", "tripwire_hook unbreaking:1 hide_enchants name:\"Default Crate\"")
            this.set("lore", emptyList<String>())
            this.set("row", 2)
            this.set("column", 3)
            this.set("right-click-previews", true)
            this.set("left-click-opens", true)
            this.set("shift-left-click-messsage", emptyList<String>())
        }

        result.set("keygui", keygui)

        val payToOpen = TransientConfig().apply {
            this.set("enabled", false)
            this.set("price", 5000)
        }

        result.set("pay-to-open", payToOpen)

        val placed = TransientConfig().apply {
            val randomReward = TransientConfig().apply {
                this.set("enabled", true)
                this.set("height", 1.5)
                this.set("delay", 30)
                this.set("name", "")
            }

            this.set("random-reward", randomReward)

            val particle = TransientConfig().apply {
                this.set("particle", "flame")
                this.set("animation", "spiral")
            }

            this.set("particles", mutableListOf(particle))

            val hologram = TransientConfig().apply {
                this.set("height", 1.5)
                this.set("ticks", 100)
                this.set("frames", emptyList<Config>())
            }

            this.set("hologram", hologram)
        }

        result.set("placed", placed)

        val open = TransientConfig().apply {
            this.set("messages", mutableListOf("Good luck!"))
            this.set("broadcasts", mutableListOf("%player%&f is opening the Default Crate!"))
            this.set("commands", mutableListOf<String>())
            this.set("sounds", emptyList<Config>())
        }

        result.set("open", open)

        val finish = TransientConfig().apply {
            this.set("messages", mutableListOf("You won %reward%&f!"))
            this.set("broadcasts", mutableListOf("%player%&f won %reward%&f from the Default Crate!"))
            this.set("commands", mutableListOf<String>())
            this.set("fireworks", emptyList<Config>())
            this.set("sounds", emptyList<Config>())
        }

        result.set("finish", finish)

        result.set("rewards", emptyList<String>())

        return result
    }
}

fun ItemStack.toLookupString(): String {
    if (Items.isCustomItem(this)) {
        val custom = Items.getCustomItem(this)!!
        return "${custom.key.namespace}:${custom.key.key}"
    }

    var result = "${this.type.name.lowercase()} ${this.amount}"

    val meta = this.itemMeta ?: return result

    meta.enchants.forEach {
        result += " ${it.key.key.key}:${it.value}"
    }

    if (meta.hasDisplayName()) {
        result += " name:\"${meta.displayName}\""
    }

    if (meta.hasCustomModelData()) {
        result += " custom-model-data:${meta.customModelData}"
    }

    meta.itemFlags.forEach {
        result += " ${it.name.lowercase()}"
    }

    return result
}
