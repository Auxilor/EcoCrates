package com.willfp.ecocrates.event

import com.willfp.ecocrates.crate.Crate

interface CrateEvent {
    val crate: Crate
}