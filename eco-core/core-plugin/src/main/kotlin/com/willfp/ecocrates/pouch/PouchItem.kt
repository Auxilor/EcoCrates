package com.willfp.ecocrates.pouch

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.TestableItem
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.recipe.Recipes
import com.willfp.eco.core.recipe.recipes.CraftingRecipe
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.util.InventoryTake
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * The physical item for a pouch. Registered as the eco custom item
 * `ecocrates:pouch_<id>` so other plugins can look it up, tagged with the
 * pouch ID, with an optional crafting recipe and an offline to-get queue.
 */
class PouchItem(
    private val pouchId: String,
    config: Config
) {
    private val toGetKey = PersistentDataKey(
        plugin.namespacedKeyFactory.create("pouch_${pouchId}_to_get"),
        PersistentDataKeyType.INT,
        0
    )

    private val testableItem: TestableItem = CustomItem(
        plugin.namespacedKeyFactory.create("pouch_$pouchId"),
        { readPouchId(it) == pouchId },
        ItemStackBuilder(Items.lookup(config.getString("item")))
            .addLoreLines(config.getFormattedStrings("lore"))
            .build()
            .also { writePouchId(it, pouchId) }
    ).apply { register() }

    val item: ItemStack
        get() = testableItem.item.clone()

    val displayName: String
        get() = item.itemMeta?.displayName ?: pouchId

    @Suppress("unused")
    val recipe: CraftingRecipe? = run {
        val crafting = config.getSubsectionOrNull("crafting") ?: return@run null

        if (!crafting.getBool("enabled")) {
            return@run null
        }

        val recipeStrings = crafting.getStrings("recipe")

        if (recipeStrings.isEmpty()) {
            return@run null
        }

        Recipes.createAndRegisterRecipe(
            plugin,
            "pouch_$pouchId",
            item,
            recipeStrings,
            crafting.getStringOrNull("permission"),
            crafting.getBool("shapeless")
        )
    }

    fun matches(itemStack: ItemStack?): Boolean =
        itemStack != null && testableItem.matches(itemStack)

    fun give(player: Player, amount: Int) {
        DropQueue(player)
            .addItems(List(amount.coerceAtLeast(1)) { item })
            .forceTelekinesis()
            .push()
    }

    /** All-or-nothing. */
    fun take(player: Player, amount: Int): Boolean =
        InventoryTake.takeAllOrNothing(player, amount) { matches(it) }

    /** Queues pouches for an offline player, handed over by [grantPending] on join. */
    fun adjustToGet(player: OfflinePlayer, amount: Int) {
        player.profile.write(toGetKey, player.profile.read(toGetKey) + amount)
    }

    fun grantPending(player: Player) {
        val pending = player.profile.read(toGetKey)

        if (pending <= 0) {
            return
        }

        player.profile.write(toGetKey, 0)
        give(player, pending)

        player.sendMessage(
            plugin.langYml.getMessage("offline-items-received")
                .replace("%amount%", pending.toString())
                .replace("%item%", displayName)
        )
    }

    companion object {
        private val pouchIdKey = plugin.namespacedKeyFactory.create("pouch_id")

        /** The pouch ID an item is tagged with, or null if it isn't a pouch. */
        fun readPouchId(itemStack: ItemStack?): String? =
            itemStack?.itemMeta?.persistentDataContainer
                ?.get(pouchIdKey, PersistentDataType.STRING)

        private fun writePouchId(itemStack: ItemStack, pouchId: String) {
            val meta = itemStack.itemMeta ?: return
            meta.persistentDataContainer.set(pouchIdKey, PersistentDataType.STRING, pouchId)
            itemStack.itemMeta = meta
        }
    }
}
