package me.fexus.examples.coolvoxelrendering.world.chunk.hull

import me.fexus.examples.coolvoxelrendering.world.Chunk


interface ChunkHullBuilder {
    fun build(chunk: Chunk, lod: Int): ChunkHullData
}