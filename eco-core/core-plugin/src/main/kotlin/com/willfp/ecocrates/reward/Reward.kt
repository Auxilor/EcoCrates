package com.willfp.ecocrates.reward

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.fast.FastItemStack
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import com.willfp.eco.core.recipe.parts.MaterialTestableItem
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.toNiceString
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.PermissionMultipliers
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import java.util.*

class Reward(
    private val plugin: EcoPlugin,
    private val config: Config
) {
    val id = config.getString("id")

    private val commands = config.getStrings("commands")

    private val items = config.getStrings("items").map { Items.lookup(it) }.filterNot { it is EmptyTestableItem }

    private val messages = config.getFormattedStrings("messages")

    private val actualChance = config.getInt("chance.actual")

    private val displayChance = config.getInt("chance.display")


    private val permission = Permission(
        "ecocrates.reward.$id",
        "Allows getting $id as a reward",
        PermissionDefault.TRUE
    ).apply {
        if (Bukkit.getPluginManager().getPermission("ecocrates.reward.*") == null) {
            addParent("ecocrates.reward.*", true)
        }
        if (Bukkit.getPluginManager().getPermission("ecocrates.reward.$id") == null) {
            Bukkit.getPluginManager().addPermission(this)
        }
    }

    private val maxWins = config.getInt("max-wins")

    private val winsKey: PersistentDataKey<Int> = PersistentDataKey(
        plugin.namespacedKeyFactory.create("${id}_wins"),
        PersistentDataKeyType.INT,
        0
    )

    private val canPermissionMultiply = config.getBool("chance.permission-multipliers")

    private val baseDisplay = ItemStackBuilder(
        Items.lookup(config.getString("display.item"))
            .let { if (it is EmptyTestableItem) MaterialTestableItem(Material.STONE) else it }
    ).setDisplayName(config.getString("display.name"))
        .build()

    fun getDisplay(player: Player, crate: Crate): ItemStack {
        val item = baseDisplay.clone()
        val fis = FastItemStack.wrap(item)
        val lore = config.getStrings("display.lore").map {
            it.replace(
                "%chance%",
                displayChance.toString()
            ).replace(
                "%actual_chance%",
                (actualChance / 100.0).toString()
            ).formatEco(player)
        }

        if (config.getBool("display.dont-keep-lore")) {
            fis.lore = lore
        } else {
            fis.lore = fis.lore + lore
        }

        return item
    }

    fun getDisplay(): ItemStack {
        return baseDisplay.clone()
    }

    fun getChance(player: Player): Double {
        val chance = actualChance.toDouble() / 100
        if (maxWins > 0) {
            if (player.profile.read(winsKey) >= maxWins) {
                return 0.0
            }
        }
        if (!player.hasPermission(permission)) {
            return 0.0
        }
        return chance
    }

    fun getPercentageChance(player: Player, among: Collection<Reward>): Double {
        val others = among.toMutableList()
        others.remove(this)

        var chance = this.getChance(player)

        if (canPermissionMultiply) {
            chance *= PermissionMultipliers.getForPlayer(player).multiplier
        }

        var totalChance = 0.0
        for (reward in among) {
            totalChance += reward.getChance(player)
        }

        return (chance / totalChance) * 100
    }

    // Legacy
    val displayRow = config.getIntOrNull("display.row")
    val displayColumn = config.getIntOrNull("display.column")

    val displayName = config.getFormattedString("display.name")

    init {
        PlayerPlaceholder(
            plugin,
            "${id}_wins",
        ) { getWins(it).toString() }.register()
    }

    fun giveTo(player: Player) {
        for (command in commands) {
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                command.replace("%player%", player.name)
            )
        }

        DropQueue(player)
            .addItems(items.map { it.item })
            .forceTelekinesis()
            .push()

        messages.forEach { player.sendMessage(plugin.langYml.prefix + it) }

        if (maxWins > 0) {
            player.profile.write(winsKey, player.profile.read(winsKey) + 1)
        }
    }

    fun getWins(player: OfflinePlayer): Int {
        return if (maxWins > 0) player.profile.read(winsKey) else 0
    }

    fun resetWins(player: OfflinePlayer) {
        if (maxWins > 0) {
            player.profile.write(winsKey, 0)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Reward) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}
