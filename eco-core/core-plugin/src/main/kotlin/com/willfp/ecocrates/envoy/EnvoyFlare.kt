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

/**
 * A category's start flare: a physical item that starts that envoy on
 * right-click, subject to its own per-player cooldown and optional price.
 */
class EnvoyFlare(
    val categoryId: String,
    private val config: Config
) {
    val enabled = config.getBool("enabled")

    val cooldownTicks = config.getInt("cooldown-ticks")

    /** Whether using the flare should also reset this category's scheduled timer. */
    val resetsSchedule = config.getBool("reset-schedule")

    val price: ConfiguredPrice = config.getSubsectionOrNull("price")
        ?.let { ConfiguredPrice.create(it) }
        ?: ConfiguredPrice.FREE

    // Registration is gated on `enabled` so a disabled start-flare never
    // occupies an item ID or a live crafting recipe - not just inert when
    // used. EnvoyCategory still constructs an EnvoyFlare to read `enabled`
    // before its `takeIf { it.enabled }`, so the gate has to live here,
    // inside the eager initializers themselves.
    private val testableItem: TestableItem? = if (enabled) {
        CustomItem(
            plugin.namespacedKeyFactory.create("${categoryId}_flare"),
            { it.isFlareFor(categoryId) },
            ItemStackBuilder(Items.lookup(config.getString("item")))
                .addLoreLines(config.getFormattedStrings("lore"))
                .build()
                .apply { flareCategoryId = categoryId }
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
            "${categoryId}_flare",
            item,
            recipeStrings,
            crafting.getStringOrNull("permission"),
            crafting.getBool("shapeless")
        )
    }

    fun matches(itemStack: ItemStack?): Boolean =
        itemStack != null && testableItem?.matches(itemStack) == true

    override fun toString() = "EnvoyFlare{category=$categoryId}"
}

private val flareKey = plugin.namespacedKeyFactory.create("envoy_flare")

internal var ItemStack.flareCategoryId: String?
    get() = this.itemMeta?.persistentDataContainer
        ?.get(flareKey, org.bukkit.persistence.PersistentDataType.STRING)
    set(value) {
        val meta = this.itemMeta ?: return

        if (value == null) {
            meta.persistentDataContainer.remove(flareKey)
        } else {
            meta.persistentDataContainer.set(
                flareKey,
                org.bukkit.persistence.PersistentDataType.STRING,
                value
            )
        }

        this.itemMeta = meta
    }

internal fun ItemStack.isFlareFor(categoryId: String): Boolean =
    this.flareCategoryId.equals(categoryId, ignoreCase = true)
