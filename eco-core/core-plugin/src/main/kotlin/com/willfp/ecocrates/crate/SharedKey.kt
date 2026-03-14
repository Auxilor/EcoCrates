package com.willfp.ecocrates.crate

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.gui.menu.MenuBuilder
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.TestableItem
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.eco.util.formatEco
import com.willfp.ecocrates.plugin
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.Objects

class SharedKey(
    override val id: String,
    private val config: Config
) : KRegistrable {

    val isCustomItem = config.getBool("use-custom-item")

    private val testableItem: TestableItem = if (isCustomItem) {
        Items.lookup(config.getString("item"))
    } else {
        CustomItem(
            plugin.namespacedKeyFactory.create("${id}_key"),
            { it.key == this },
            Items.lookup(config.getString("item")).item.clone().apply { key = this@SharedKey }
        ).apply { register() }
    }

    val lore = config.getFormattedStrings("lore")

    private val keysKey = makeIntKey("${id}_keys")
    private val toGetKey = makeIntKey("${id}_to_get")

    val item: ItemStack
        get() = testableItem.item.clone()

    fun createItem(owner: OfflinePlayer? = null): ItemStack {
        val itemStack = item

        if (owner != null && plugin.configYml.getBool("track-player-keys")) {
            val meta = itemStack.itemMeta ?: return itemStack
            meta.persistentDataContainer.set(
                plugin.namespacedKeyFactory.create("player"),
                PersistentDataType.STRING,
                owner.uniqueId.toString()
            )
            itemStack.itemMeta = meta
        }

        return itemStack
    }

    fun matches(itemStack: ItemStack?): Boolean {
        return itemStack != null && testableItem.matches(itemStack)
    }

    fun adjustVirtualKeys(player: OfflinePlayer, amount: Int) {
        player.profile.write(keysKey, player.profile.read(keysKey) + amount)
    }

    fun getVirtualKeys(player: OfflinePlayer): Int {
        return player.profile.read(keysKey)
    }

    fun getKeysToGet(player: OfflinePlayer): Int {
        return player.profile.read(toGetKey)
    }

    fun setKeysToGet(player: OfflinePlayer, amount: Int) {
        player.profile.write(toGetKey, amount)
    }

    /**
     * Adds this key's slot to the Key GUI.
     * The key config drives everything: item, lore, position, click actions.
     * The optional 'keygui.crate' field controls which crate is opened on left-click.
     */
    internal fun addToKeyGUI(builder: MenuBuilder) {
        if (!config.getBool("keygui.enabled")) {
            return
        }

        // Resolve the crate to open (defaults to the crate with the same ID as the key)
        val crateId = config.getString("keygui.crate").ifEmpty { id }

        builder.setSlot(
            config.getInt("keygui.row"),
            config.getInt("keygui.column"),
            slot(
                ItemStackBuilder(Items.lookup(config.getString("keygui.item"))).build()
            ) {
                onLeftClick { event, _, _ ->
                    if (config.getBool("keygui.left-click-opens")) {
                        val player = event.whoClicked as Player
                        val crate = Crates[crateId] ?: return@onLeftClick
                        player.closeInventory()
                        crate.openWithMethod(player, OpenMethod.VIRTUAL_KEY)
                    }
                }

                onRightClick { event, _, _ ->
                    if (config.getBool("keygui.right-click-previews")) {
                        val player = event.whoClicked as Player
                        val crate = Crates[crateId] ?: return@onRightClick
                        player.closeInventory()
                        crate.previewForPlayer(player)
                    }
                }

                onShiftLeftClick { event, _, _ ->
                    event.whoClicked.closeInventory()
                    config.getFormattedStrings("keygui.shift-left-click-message")
                        .forEach { event.whoClicked.sendMessage(it) }
                }

                setUpdater { player, _, previous ->
                    previous.apply {
                        itemMeta = itemMeta?.apply {
                            lore = config.getStrings("keygui.lore")
                                .map { it.replace("%keys%", getVirtualKeys(player).toString()) }
                                .map { it.formatEco(player) }
                        }
                    }
                    previous
                }
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other !is SharedKey) {
            return false
        }

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }

    override fun toString(): String {
        return "SharedKey{id=$id}"
    }

    override fun getID(): String {
        return id
    }

    companion object {
        private fun makeIntKey(key: String) = PersistentDataKey(
            plugin.namespacedKeyFactory.create(key),
            PersistentDataKeyType.INT,
            0
        )
    }
}
