package com.willfp.ecocrates.envoy

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.TestableItem
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.price.ConfiguredPrice
import com.willfp.eco.core.recipe.Recipes
import com.willfp.eco.core.recipe.recipes.CraftingRecipe
import com.willfp.ecocrates.plugin
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

private val compassKey = plugin.namespacedKeyFactory.create("envoy_compass")

internal var ItemStack.compassCategoryId: String?
    get() = this.itemMeta?.persistentDataContainer
        ?.get(compassKey, PersistentDataType.STRING)
    set(value) {
        val meta = this.itemMeta ?: return

        if (value == null) {
            meta.persistentDataContainer.remove(compassKey)
        } else {
            meta.persistentDataContainer.set(compassKey, PersistentDataType.STRING, value)
        }

        this.itemMeta = meta
    }

internal fun ItemStack.isCompassFor(categoryId: String): Boolean =
    this.compassCategoryId.equals(categoryId, ignoreCase = true)

/**
 * A category's envoy compass: a consumable item that marks nearby envoy
 * crates on the player's locator bar for a limited time.
 */
class EnvoyCompass(
    val categoryId: String,
    private val config: Config
) {
    val enabled = config.getBool("enabled")

    val cooldownTicks = config.getInt("cooldown-ticks")

    /** How long one use lasts, in ticks. */
    val durationTicks = config.getInt("duration").coerceAtLeast(1)

    /** How many crates one compass marks at once. */
    val maxTracked = config.getInt("max-tracked").coerceAtLeast(1)

    /** Blocks. 0 or less means unlimited. */
    val range = config.getInt("range")

    val price: ConfiguredPrice = config.getSubsectionOrNull("price")
        ?.let { ConfiguredPrice.create(it) }
        ?: ConfiguredPrice.FREE

    // Registration is gated on `enabled` so a disabled compass never
    // occupies an item ID or a live crafting recipe - not just inert when
    // used. EnvoyCategory still constructs an EnvoyCompass to read `enabled`
    // before its `takeIf { it.enabled }`, so the gate has to live here,
    // inside the eager initializers themselves.
    private val testableItem: TestableItem? = if (enabled) {
        CustomItem(
            plugin.namespacedKeyFactory.create("${categoryId}_compass"),
            { it.isCompassFor(categoryId) },
            ItemStackBuilder(Items.lookup(config.getString("item")))
                .addLoreLines(config.getFormattedStrings("lore"))
                .build()
                .apply { compassCategoryId = categoryId }
        ).apply { register() }
    } else {
        null
    }

    val item: ItemStack
        get() = testableItem?.item?.clone() ?: ItemStack(Material.AIR)

    @Suppress("unused")
    val recipe: CraftingRecipe? = run {
        if (!enabled) {
            return@run null
        }

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
            "${categoryId}_compass",
            item,
            recipeStrings,
            crafting.getStringOrNull("permission"),
            crafting.getBool("shapeless")
        )
    }

    fun matches(itemStack: ItemStack?): Boolean =
        itemStack != null && testableItem?.matches(itemStack) == true

    override fun toString() = "EnvoyCompass{category=$categoryId}"
}
