package me.fexus.examples.coolvoxelrendering.world.chunk

import me.fexus.examples.coolvoxelrendering.world.chunk.hull.ChunkHullBuilderSimple
import me.fexus.examples.coolvoxelrendering.world.chunk.hull.ChunkHullData
import me.fexus.examples.coolvoxelrendering.world.chunk.hull.ChunkMeshBuilderGreedy
import me.fexus.examples.coolvoxelrendering.world.Chunk


class ChunkHullFactory {
    private val simpleHullBuilder = ChunkHullBuilderSimple()
    private val greedyHullBuilder = ChunkMeshBuilderGreedy()


    fun buildSimple(chunk: Chunk, maxDepth: Int): ChunkHullData {
        return simpleHullBuilder.build(chunk, maxDepth)
    }


    fun buildGreedy(chunk: Chunk, maxDepth: Int): ChunkHullData {
        return greedyHullBuilder.build(chunk, maxDepth)
    }
}