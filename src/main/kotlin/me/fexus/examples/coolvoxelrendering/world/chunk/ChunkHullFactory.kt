package me.fexus.examples.coolvoxelrendering.world.chunk

import me.fexus.examples.coolvoxelrendering.world.chunk.hull.ChunkHullBuilderSimple
import me.fexus.examples.coolvoxelrendering.world.chunk.hull.ChunkHullData
import me.fexus.examples.coolvoxelrendering.world.chunk.hull.ChunkMeshBuilderGreedy
import me.fexus.examples.coolvoxelrendering.world.Chunk


class ChunkHullFactory {
    private val simpleHullBuilder = ChunkHullBuilderSimple()
    private val greedyHullBuilder = ChunkMeshBuilderGreedy()


    fun buildSimple(chunkHullingPacket: ChunkHullingPacket): ChunkHullData {
        return simpleHullBuilder.build(chunkHullingPacket)
    }


    fun buildGreedy(chunkHullingPacket: ChunkHullingPacket): ChunkHullData {
        return greedyHullBuilder.build(chunkHullingPacket)
    }
}