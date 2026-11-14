package com.willfp.ecocrates.pouch

import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.util.InventoryTake
import com.willfp.ecocrates.util.addPending
import com.willfp.ecocrates.util.takePending
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import wiki.ecohub.config.Config
import wiki.ecohub.data.PersistentDataKey
import wiki.ecohub.data.PersistentDataKeyType
import wiki.ecohub.game.drop.DropQueue
import wiki.ecohub.game.entity.player.profile
import wiki.ecohub.game.item.CustomItem
import wiki.ecohub.game.item.Items
import wiki.ecohub.game.item.builder.ItemStackBuilder
import wiki.ecohub.game.item.lookup.TestableItem
import wiki.ecohub.modules.recipe.Recipes
import wiki.ecohub.modules.recipe.crafting.CraftingRecipe

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
        get() = testableItem.getItem().clone()

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
        player.profile.addPending(toGetKey, amount)
    }

    fun grantPending(player: Player) {
        val pending = player.profile.takePending(toGetKey)

        if (pending <= 0) {
            return
        }

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
