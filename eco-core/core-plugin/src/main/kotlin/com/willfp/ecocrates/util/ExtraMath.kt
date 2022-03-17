package com.willfp.ecocrates.util

fun lerp(start: Double, end: Double, fraction: Double): Double =
    (start * (1 - fraction)) + (end * fraction)
