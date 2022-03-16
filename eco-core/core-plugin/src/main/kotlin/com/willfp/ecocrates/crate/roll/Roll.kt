package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.reward.Reward

interface Roll {
    val reward: Reward
    fun roll()
}
