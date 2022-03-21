package com.willfp.ecocrates.converters.util

import com.willfp.eco.core.Eco
import com.willfp.eco.core.config.BaseConfig
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.Items
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.yaml.snakeyaml.Yaml

class ConverterUtils {
    companion object {
        @JvmStatic
        fun createDefaultReward(): YamlConfiguration {
            val result = emptyBukkit()

            result.set("id", "default")
            result.set("commands", mutableListOf<String>())
            result.set("items", mutableListOf("diamond_sword sharpness:5 unbreaking:3"))
            result.set("messages", mutableListOf<String>())
            result.set("weight",
                emptyBukkit().apply {
                    this.set("permission-multipliers", true)
                    this.set("actual", 1)
                    this.set("display", 25)
                }
            )
            result.set("max-wins", -1)
            result.set("display",
                emptyBukkit().apply {
                    this.set("name", "&bDiamond Sword")
                    this.set("item", "diamond_sword sharpness:5 unbreaking:3")
                    this.set("lore", mutableListOf(
                        "&fDisplay Chance: &a%chance%%",
                        "&fActual Chance: &a%actual_chance%%"
                    ))
                    this.set("row", 3)
                    this.set("column", 2)
                }
            )

            return result
        }

        @JvmStatic
        fun createDefaultCrate(): YamlConfiguration {

            val result = emptyBukkit()

            result.set("id", "default")
            result.set("name", "default")
            result.set("roll", "csgo")
            result.set("can-reroll", true)

            val preview = emptyBukkit().apply {
                this.set("title", "Default Crate")
                this.set("rows", 6)
                val mask = emptyBukkit().apply {
                    this.set("items", mutableListOf("black_stained_glass_pane"))
                    this.set("pattern", mutableListOf(
                        "222222222",
                        "200000002",
                        "200000002",
                        "200000002",
                        "200000002",
                        "222222222"
                    ))
                }
                this.set("mask", convertToBukkit(mask))
            }

            result.set("preview", convertToBukkit(preview))

            val key = emptyBukkit().apply {
                this.set("item", "tripwire_hook unbreaking:1 hide_enchants name:\"&aDefault Crate Key\"")
                this.set("lore", mutableListOf(
                    "&fUse this key to open",
                    "&fthe <g:#56ab2f>Default Crate</g:#a8e063>"
                ))
            }

            result.set("key", convertToBukkit(key))

            val keygui = emptyBukkit().apply {
                this.set("enabled", true)
                this.set("item", "tripwire_hook unbreaking:1 hide_enchants name:\"Default Crate\"")
                this.set("lore", mutableListOf(
                    "<g:#56ab2f>Default Crate</g:#a8e063>",
                    "&fYou have %keys% keys",
                    "&fGet more at &astore.example.net"
                ))
                this.set("row", 2)
                this.set("column", 3)
                this.set("right-click-previews", true)
                this.set("left-click-opens", true)
                this.set("shift-left-click-messsage", mutableListOf("Buy a Demo Crate key here! &astore.example.net"))
            }

            result.set("keygui", convertToBukkit(keygui))

            val payToOpen = emptyBukkit().apply {
                this.set("enabled", false)
                this.set("price", 5000)
            }

            result.set("pay-to-open", convertToBukkit(payToOpen))

            val placed = emptyBukkit().apply {
                val randomReward = emptyBukkit().apply {
                    this.set("enabled", true)
                    this.set("height", 1.5)
                    this.set("delay", 30)
                    this.set("name", "&fYou could win:")
                }

                this.set("random-reward", convertToBukkit(randomReward))

                val particle = emptyBukkit().apply {
                    this.set("particle", "flame")
                    this.set("animation", "spiral")
                }

                this.set("particles", mutableListOf(convertToBukkit(particle)))

                val hologram = emptyBukkit().apply {
                    this.set("height", 1.5)
                    this.set("ticks", 200)
                    val tick0 = emptyBukkit().apply {
                        this.set("tick", 0)
                        this.set("lines", mutableListOf(
                            "<g:#56ab2f>&lDEFAULT CRATE</g:#a8e063>",
                            "&b&lLeft Click to Preview",
                            "&a&lRight click to Open"
                        ))
                    }

                    val tick100 = emptyBukkit().apply {
                        this.set("tick", 100)
                        this.set("lines", mutableListOf(
                            "<g:#56ab2f>&lDEFAULT CRATE</g:#a8e063>",
                            "&a&lLeft Click to Preview",
                            "&b&lRight click to Open"
                        ))
                    }
                    this.set("frames", mutableListOf(convertToBukkit(tick0), convertToBukkit(tick100)))
                }

                this.set("hologram", hologram)
            }

            result.set("placed", convertToBukkit(placed))

            val open = emptyBukkit().apply {
                this.set("messages", mutableListOf("Good luck!"))
                this.set("broadcasts", mutableListOf("%player%&f is opening the Default Crate!"))
                this.set("commands", mutableListOf<String>())
                val sound = emptyBukkit().apply {
                    this.set("sound", "entity_villager_yes")
                    this.set("volume", 10)
                    this.set("pitch", 1)
                }
                this.set("sounds", mutableListOf(convertToBukkit(sound)))
            }

            result.set("open", convertToBukkit(open))

            val finish = emptyBukkit().apply {
                this.set("messages", mutableListOf("You won %reward%&f!"))
                this.set("broadcasts", mutableListOf("%player%&f won %reward%&f from the Default Crate!"))
                this.set("commands", mutableListOf<String>())
                val firework = emptyBukkit().apply {
                    this.set("power", 2)
                    this.set("type", "ball_large")
                    this.set("colors", mutableListOf("00ffff", "00ff00"))
                    this.set("fade-colors", mutableListOf("ffffff", "999999"))
                    this.set("trail", true)
                    this.set("flicker", true)
                }
                this.set("fireworks", mutableListOf(convertToBukkit(firework)))
                val sound = emptyBukkit().apply {
                    this.set("sound", "entity_generic_explode")
                    this.set("volume", 10)
                    this.set("pitch", 1)
                }
                this.set("sounds", mutableListOf(convertToBukkit(sound)))
            }

            result.set("finish", convertToBukkit(finish))

            result.set("rewards", mutableListOf("diamond_sword", "bedrock"))

            return convertToBukkit(result)
        }

        @JvmStatic
        fun emptyBukkit(): YamlConfiguration {
            return YamlConfiguration()
        }

        @JvmStatic
        fun convertToBukkit(config: YamlConfiguration): YamlConfiguration {
            return config
        }

        @JvmStatic
        fun itemToStringSimplified(itemStack: ItemStack): String {

            if (Items.isCustomItem(itemStack)) {
                val custom = Items.getCustomItem(itemStack)!!
                return "${custom.key.namespace}:${custom.key.key}"
            }

            var result = "${itemStack.type.name.lowercase()} ${itemStack.amount}"

            val meta = itemStack.itemMeta?: return result

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
    }
}