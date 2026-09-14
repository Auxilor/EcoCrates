package com.willfp.ecocrates.crate.roll

/** A resolved roll ID, plus a warning to log if the configured one had to be replaced. */
data class RollResolution(
    val id: String,
    val warning: String?
)

/** Picks the roll a source will actually use, falling back to [FALLBACK_ROLL] on bad config. */
object RollValidation {
    const val FALLBACK_ROLL = "csgo"

    /**
     * For sources limited to GUI rolls, such as pouches.
     *
     * @param isGuiRoll Whether the roll with that ID is a GUI roll, or null if no such roll exists.
     */
    fun resolveGuiRoll(requested: String, isGuiRoll: (String) -> Boolean?): RollResolution =
        when (isGuiRoll(requested)) {
            true -> RollResolution(requested, null)
            false -> RollResolution(FALLBACK_ROLL, "uses non-GUI roll '$requested', falling back to $FALLBACK_ROLL")
            null -> RollResolution(FALLBACK_ROLL, "uses unknown roll '$requested', falling back to $FALLBACK_ROLL")
        }
}
