package com.willfp.ecocrates.crate.placed

import org.bukkit.Chunk

data class ChunkKey(
    val worldName: String,
    val x: Int,
    val z: Int
)

val Chunk.key: ChunkKey
    get() = ChunkKey(this.world.name, this.x, this.z)
