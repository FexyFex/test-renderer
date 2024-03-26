package me.fexus.examples.coolvoxelrendering.world.chunk.hull

import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHullingPacket


interface ChunkHullBuilder {
    fun build(chunkHullingPacket: ChunkHullingPacket): ChunkHullData
}