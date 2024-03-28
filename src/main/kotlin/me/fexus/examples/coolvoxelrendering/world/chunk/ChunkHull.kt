package me.fexus.examples.coolvoxelrendering.world.chunk

import me.fexus.examples.coolvoxelrendering.world.chunk.hull.ChunkHullData
import me.fexus.examples.coolvoxelrendering.world.position.ChunkPosition
import me.fexus.math.vec.IVec3


data class ChunkHull(val chunkPosition: ChunkPosition, val data: ChunkHullData)
