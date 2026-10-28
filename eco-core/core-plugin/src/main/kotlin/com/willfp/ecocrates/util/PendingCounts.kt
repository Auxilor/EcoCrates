package com.willfp.ecocrates.util

import com.willfp.eco.core.data.Profile
import com.willfp.eco.core.data.keys.PersistentDataKey

/**
 * Add [amount] to a pending count in one atomic step, so a join handing the count out at the
 * same time cannot lose it.
 */
internal fun Profile.addPending(key: PersistentDataKey<Int>, amount: Int) {
    this.compute(key) { it + amount }
}

/**
 * Read a pending count and reset it to 0 in one atomic step.
 *
 * @return The count before it was reset.
 */
internal fun Profile.takePending(key: PersistentDataKey<Int>): Int {
    var pending = 0
    this.compute(key) { current ->
        pending = current
        0
    }
    return pending
}
