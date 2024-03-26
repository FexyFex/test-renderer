package me.fexus.examples.coolvoxelrendering.world.chunk

import me.fexus.examples.coolvoxelrendering.world.Chunk


data class ChunkHullingPacket(val chunk: Chunk, val surroundingChunks: List<Chunk>, val maxDepth: Int)