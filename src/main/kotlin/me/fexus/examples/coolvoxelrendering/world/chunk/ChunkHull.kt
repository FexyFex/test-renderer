package me.fexus.examples.coolvoxelrendering.world.chunk

import me.fexus.examples.coolvoxelrendering.world.chunk.hull.ChunkHullData
import me.fexus.math.vec.IVec3


data class ChunkHull(val chunkPosition: IVec3, val data: ChunkHullData)
