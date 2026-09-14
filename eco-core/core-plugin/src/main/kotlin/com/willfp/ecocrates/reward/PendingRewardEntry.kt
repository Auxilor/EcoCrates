package com.willfp.ecocrates.reward

/**
 * One queued offline reward, stored as `type:sourceId:rewardId`. Entries
 * written before typed sources existed are `crateId:rewardId` and are read
 * back as crates.
 */
data class PendingRewardEntry(
    val sourceType: String,
    val sourceId: String,
    val rewardId: String
) {
    fun serialize(): String = "$sourceType:$sourceId:$rewardId"

    companion object {
        fun parse(raw: String): PendingRewardEntry? {
            val parts = raw.split(':')

            if (parts.any { it.isEmpty() }) {
                return null
            }

            return when (parts.size) {
                2 -> PendingRewardEntry(SourceTypes.CRATE, parts[0], parts[1])
                3 -> PendingRewardEntry(parts[0], parts[1], parts[2])
                else -> null
            }
        }
    }
}
