package com.willfp.ecocrates.crate.open

import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.placed.PlacedCrate
import org.bukkit.Location

/**
 * Where and how an open happens.
 *
 * @property method How the open was paid for.
 * @property location Where the rolls play.
 * @property placedCrate The placed crate being opened, if any.
 */
internal data class OpenContext(
    val method: OpenMethod,
    val location: Location,
    val placedCrate: PlacedCrate?
)
