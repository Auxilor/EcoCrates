package com.willfp.ecocrates.crate

import org.bukkit.Bukkit
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault

data class PermissionMultiplier(
    val permission: String,
    val multiplier: Double,
    val priority: Int
) {
    init {
        if (Bukkit.getPluginManager().getPermission(permission) == null) {
            Bukkit.getPluginManager().addPermission(
                Permission(
                    permission,
                    "Gives a ${multiplier}x chance multiplier in crate rewards",
                    PermissionDefault.FALSE
                )
            )
        }
    }
}
