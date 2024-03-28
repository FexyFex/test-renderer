package me.fexus.examples.coolvoxelrendering.world.generation

import me.fexus.examples.coolvoxelrendering.world.Chunk
import me.fexus.examples.coolvoxelrendering.world.position.ChunkPosition
import me.fexus.math.vec.IVec3


// The GPU generation already attempts to place grass and dirt in the correct way but
// there are cases where a chunk border might interfere with that, so the GPU flags some voxels
// as "potentially grass/dirt replacable". Those voxels might be replaced here if sensible.
class BlockTypePlacementVerifier {

    // Get all chunks that have the chunk above them generated
    fun verifySoil(allChunks: Map<ChunkPosition, Chunk>, relevantChunks: Map<ChunkPosition, Chunk>) {
        val up = IVec3(0, 1, 0)
        val chunksForReplacing = mutableListOf<ChunkSoilReplacementPair>()
        relevantChunks.forEach { (pos: ChunkPosition, chunk: Chunk) ->
            val upperPos = ChunkPosition(pos + up)
            val upperChunk = allChunks[upperPos]
            if (upperChunk != null) {
                chunksForReplacing.add(ChunkSoilReplacementPair(chunk, upperChunk))
            }
        }
        chunksForReplacing.forEach(this::placeSoil)
    }

    private fun placeSoil(chunkPair: ChunkSoilReplacementPair) {
        val (lowerChunk, upperChunk) = chunkPair

        for (x in 0 until Chunk.EXTENT) {
            for (z in 0 until Chunk.EXTENT) {
                val upperValue = upperChunk.getVoxelAt(x, 0, z)
                val upperVoxel = upperValue and 65535
                val upperFlags = upperValue ushr 16
                val isFree = upperVoxel == 0
                val isCave = (upperFlags and 2) == 2
                if (!isFree || isCave) continue

                val value = lowerChunk.getVoxelAt(x, 15, z)
                val flags = value ushr 16
                val isSoilFlagged = (flags and 1) == 1
                if (isSoilFlagged) {
                    lowerChunk.setVoxelAt(x, 15, z, 2)

                    // Do it once more if successful the first time
                    val value2 = lowerChunk.getVoxelAt(x, 14, z)
                    val voxel2 = value2 and 65535
                    if (voxel2 != 0) lowerChunk.setVoxelAt(x, 14, z, 2)
                }
            }
        }

        lowerChunk.isSoilFlagged = false
    }


    data class ChunkSoilReplacementPair(val lowerChunk: Chunk, val upperChunk: Chunk)
}