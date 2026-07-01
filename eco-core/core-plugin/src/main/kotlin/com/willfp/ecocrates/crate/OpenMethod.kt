package com.willfp.ecocrates.crate

import com.willfp.eco.core.price.Prices
import com.willfp.ecocrates.plugin
import org.bukkit.entity.Player
import java.util.Objects

abstract class OpenMethod(
    val id: String
) {
    abstract fun canUseAndNotify(crate: Crate, player: Player): Boolean
    abstract fun useMethod(crate: Crate, player: Player)

    open fun getBulkAmount(crate: Crate, player: Player): Int = 1

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

            override fun getBulkAmount(crate: Crate, player: Player): Int {
                if (!crate.hasPhysicalKey(player)) {
                    return 0
                }

                return player.inventory.itemInMainHand.amount
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

            override fun getBulkAmount(crate: Crate, player: Player): Int {
                return crate.getVirtualKeys(player)
            }
        }

        val MONEY = object : OpenMethod("money") {
            override fun canUseAndNotify(crate: Crate, player: Player): Boolean {
                val hasAmount = Prices.create(crate.priceToOpen.toString(), crate.currencyType).canAfford(player)
                if (!hasAmount) {
                    player.sendMessage(plugin.langYml.getMessage("cannot-afford").replace("%crate%", crate.name))
                }

                return hasAmount
            }

            override fun useMethod(crate: Crate, player: Player) {
                Prices.create(crate.priceToOpen.toString(), crate.currencyType).pay(player)
            }
        }

        val OTHER = object : OpenMethod("other") {
            override fun canUseAndNotify(crate: Crate, player: Player) = true

            override fun useMethod(crate: Crate, player: Player) {}
        }
    }
}