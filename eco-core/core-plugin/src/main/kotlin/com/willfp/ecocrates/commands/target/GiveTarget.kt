package com.willfp.ecocrates.commands.target

import com.willfp.eco.core.drops.DropQueue
import com.willfp.ecocrates.crate.Keys
import com.willfp.ecocrates.envoy.EnvoyItemType
import com.willfp.ecocrates.envoy.EnvoyItems
import com.willfp.ecocrates.envoy.Envoys
import com.willfp.ecocrates.envoy.withEnvoyPlaceholders
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.pouch.Pouches
import com.willfp.ecocrates.util.InventoryTake
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

/** Something the give/giveall/giveoffline/take commands can hand out or remove. */
interface GiveTarget {
    val type: TargetType

    fun ids(): List<String>

    /** A ready-to-send error if [id] or [variant] can't be used, or null if they're fine. */
    fun validationError(id: String, variant: String?): String?

    fun displayName(id: String, variant: String?): String

    /** @return false if this needs the player online and they aren't. */
    fun give(player: OfflinePlayer, id: String, variant: String?, amount: Int): Boolean

    /** Gives to a player who may be offline, queueing physical items for when they join. */
    fun queue(player: OfflinePlayer, id: String, variant: String?, amount: Int)

    /** All-or-nothing. @return false if the player didn't have enough. */
    fun take(player: Player, id: String, variant: String?, amount: Int): Boolean
}

object KeyTarget : GiveTarget {
    override val type = TargetType.KEY

    override fun ids() = Keys.values().map { it.id }

    override fun validationError(id: String, variant: String?): String? =
        if (Keys[id] == null) plugin.langYml.getMessage("invalid-key") else null

    override fun displayName(id: String, variant: String?): String =
        Keys[id]?.displayName ?: id

    override fun give(player: OfflinePlayer, id: String, variant: String?, amount: Int): Boolean {
        val key = Keys[id] ?: return false

        if (variant != "physical") {
            key.adjustVirtualKeys(player, amount)
            return true
        }

        val online = player.player ?: return false

        DropQueue(online)
            .addItems(List(amount) { key.createItem(online) })
            .forceTelekinesis()
            .push()

        return true
    }

    override fun queue(player: OfflinePlayer, id: String, variant: String?, amount: Int) {
        val key = Keys[id] ?: return

        if (variant == "physical") {
            key.setKeysToGet(player, key.getKeysToGet(player) + amount)
        } else {
            key.adjustVirtualKeys(player, amount)
        }
    }

    override fun take(player: Player, id: String, variant: String?, amount: Int): Boolean {
        val key = Keys[id] ?: return false

        if (variant == "physical") {
            return InventoryTake.takeAllOrNothing(player, amount) { key.matches(it) }
        }

        if (key.getVirtualKeys(player) < amount) {
            return false
        }

        key.adjustVirtualKeys(player, -amount)
        return true
    }
}

object EnvoyTarget : GiveTarget {
    override val type = TargetType.ENVOY

    override fun ids() = Envoys.values().map { it.id }

    override fun validationError(id: String, variant: String?): String? {
        val category = Envoys[id] ?: return plugin.langYml.getMessage("invalid-envoy")

        val itemType = EnvoyItemType.fromToken(variant)
            ?: return plugin.langYml.getMessage("invalid-variant")
                .replace("%variants%", type.variants.joinToString(", "))

        if (EnvoyItems.itemFor(category, itemType) == null) {
            return plugin.langYml.getMessage("envoy-item-not-enabled").withEnvoyPlaceholders(category)
        }

        return null
    }

    override fun displayName(id: String, variant: String?): String {
        val category = Envoys[id] ?: return id
        val itemType = EnvoyItemType.fromToken(variant) ?: return id

        return EnvoyItems.itemFor(category, itemType)?.itemMeta?.displayName ?: itemType.token
    }

    override fun give(player: OfflinePlayer, id: String, variant: String?, amount: Int): Boolean {
        val online = player.player ?: return false
        val category = Envoys[id] ?: return false
        val itemType = EnvoyItemType.fromToken(variant) ?: return false

        return EnvoyItems.give(online, category, itemType, amount)
    }

    override fun queue(player: OfflinePlayer, id: String, variant: String?, amount: Int) {
        val category = Envoys[id] ?: return
        val itemType = EnvoyItemType.fromToken(variant) ?: return

        EnvoyItems.adjustToGet(player, category, itemType, amount)
    }

    override fun take(player: Player, id: String, variant: String?, amount: Int): Boolean {
        val category = Envoys[id] ?: return false
        val itemType = EnvoyItemType.fromToken(variant) ?: return false

        return EnvoyItems.take(player, category, itemType, amount)
    }
}

object PouchTarget : GiveTarget {
    override val type = TargetType.POUCH

    override fun ids() = Pouches.values().map { it.id }

    override fun validationError(id: String, variant: String?): String? =
        if (Pouches[id] == null) plugin.langYml.getMessage("invalid-pouch") else null

    override fun displayName(id: String, variant: String?): String =
        Pouches[id]?.item?.displayName ?: id

    override fun give(player: OfflinePlayer, id: String, variant: String?, amount: Int): Boolean {
        val online = player.player ?: return false
        val pouch = Pouches[id] ?: return false

        pouch.item.give(online, amount)
        return true
    }

    override fun queue(player: OfflinePlayer, id: String, variant: String?, amount: Int) {
        Pouches[id]?.item?.adjustToGet(player, amount)
    }

    override fun take(player: Player, id: String, variant: String?, amount: Int): Boolean =
        Pouches[id]?.item?.take(player, amount) ?: false
}

object GiveTargets {
    /** The types give, giveall, giveoffline and take accept. */
    val giveTypes = setOf(TargetType.KEY, TargetType.ENVOY, TargetType.POUCH)

    fun forType(type: TargetType): GiveTarget? = when (type) {
        TargetType.KEY -> KeyTarget
        TargetType.ENVOY -> EnvoyTarget
        TargetType.POUCH -> PouchTarget
        TargetType.CRATE -> null
    }
}
