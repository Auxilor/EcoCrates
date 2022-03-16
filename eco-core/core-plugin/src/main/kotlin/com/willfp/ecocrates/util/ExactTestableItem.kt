package com.willfp.ecocrates.util

import com.willfp.eco.core.items.TestableItem
import org.bukkit.inventory.ItemStack

class ExactTestableItem(
    private var itemStack: ItemStack
) : TestableItem {
    override fun matches(itemStack: ItemStack?): Boolean {
        return itemStack == this.itemStack
    }

    override fun getItem(): ItemStack = itemStack.clone()
}
