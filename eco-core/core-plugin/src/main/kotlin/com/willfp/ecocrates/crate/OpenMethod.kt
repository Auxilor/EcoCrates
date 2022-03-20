package com.willfp.ecocrates.crate

import com.willfp.eco.core.integrations.economy.EconomyManager
import com.willfp.ecocrates.EcoCratesPlugin
import org.bukkit.entity.Player
import java.util.Objects

abstract class OpenMethod(
    val id: String
) {
    abstract fun canUseAndNotify(crate: Crate, player: Player): Boolean
    abstract fun useMethod(crate: Crate, player: Player)

    override fun equals(other: Any?): Boolean {
        if (other !is OpenMethod) {
            return false
        }

        return other.id == this.id
    }

    override fun hashCode(): Int {
        return Objects.hashCode(this.id)
    }

    companion object {
        private val plugin = EcoCratesPlugin.instance

        val PHYSICAL_KEY = object : OpenMethod("physical_key") {
            override fun canUseAndNotify(crate: Crate, player: Player): Boolean {
                val hasKey = crate.hasPhysicalKey(player)
                if (!hasKey) {
                    player.sendMessage(plugin.langYml.getMessage("not-enough-keys").replace("%crate%", crate.name))
                }

                return hasKey
            }

            override fun useMethod(crate: Crate, player: Player) {
                crate.usePhysicalKey(player)
            }
        }

        val VIRTUAL_KEY = object : OpenMethod("virtual_key") {
            override fun canUseAndNotify(crate: Crate, player: Player): Boolean {
                val hasKey = crate.hasVirtualKey(player)
                if (!hasKey) {
                    player.sendMessage(plugin.langYml.getMessage("not-enough-keys").replace("%crate%", crate.name))
                }

                return hasKey
            }

            override fun useMethod(crate: Crate, player: Player) {
                crate.adjustVirtualKeys(player, -1)
            }
        }

        val MONEY = object : OpenMethod("money") {
            override fun canUseAndNotify(crate: Crate, player: Player): Boolean {
                val hasAmount = EconomyManager.hasAmount(player, crate.priceToOpen)

                if (!hasAmount) {
                    player.sendMessage(plugin.langYml.getMessage("cannot-afford").replace("%crate%", crate.name))
                }

                return hasAmount
            }

            override fun useMethod(crate: Crate, player: Player) {
                EconomyManager.removeMoney(player, crate.priceToOpen)
            }
        }

        val OTHER = object : OpenMethod("other") {
            override fun canUseAndNotify(crate: Crate, player: Player) = true

            override fun useMethod(crate: Crate, player: Player) {}
        }
    }
}