package me.fexus.examples.coolvoxelrendering.world.chunk

import me.fexus.examples.coolvoxelrendering.world.chunk.hull.ChunkHullBuilderSimple
import me.fexus.examples.coolvoxelrendering.world.chunk.hull.ChunkHullData
import me.fexus.examples.coolvoxelrendering.world.chunk.hull.ChunkMeshBuilderGreedy
import me.fexus.examples.coolvoxelrendering.world.Chunk


class ChunkHullFactory {
    private val simpleHullBuilder = ChunkHullBuilderSimple()
    private val greedyHullBuilder = ChunkMeshBuilderGreedy()


    fun buildSimple(chunk: Chunk, lod: Int): ChunkHullData {
        return simpleHullBuilder.build(chunk, lod)
    }


    fun buildGreedy(chunk: Chunk, lod: Int): ChunkHullData {
        return greedyHullBuilder.build(chunk, lod)
    }
}