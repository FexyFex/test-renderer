package me.fexus.examples.coolvoxelrendering.world.chunk.hull

import me.fexus.examples.coolvoxelrendering.world.VoxelSide
import me.fexus.examples.coolvoxelrendering.world.Direction
import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHullingPacket
import me.fexus.math.vec.IVec2
import me.fexus.math.vec.IVec3
import me.fexus.voxel.VoxelOctree
import java.nio.ByteBuffer
import java.nio.ByteOrder


class ChunkHullBuilderSimple: ChunkHullBuilder {
    private val directions = Direction.values()

    override fun build(chunkHullingPacket: ChunkHullingPacket): ChunkHullData {
        val (chunk, surroundingChunks, maxDepth) = chunkHullingPacket

        val buf = ByteBuffer.allocate(VoxelOctree.VOXEL_COUNT * 6 * Int.SIZE_BYTES)
        buf.order(ByteOrder.LITTLE_ENDIAN)

        val scaling = VoxelOctree.EXTENT shr (maxDepth + 1)

        var instanceCount = 0
        var offset = 0

        chunk.forEachVoxel(maxDepth) { position, voxel ->
            if (chunk.isFull && position.all { it in 1..14 }) return@forEachVoxel
            directions.forEach { dir ->
                val nextPos = position + (dir.normal * scaling)
                val nextVoxel: Int
                if (nextPos.any { it < 0 || it > 15 }) {
                    val index = nextPos.indexOfFirst { it < 0 || it > 15 }
                    val nextChunk = if (nextPos[index] > 15) {
                        surroundingChunks[index]
                    } else {
                        surroundingChunks[index + 3]
                    }
                    val transformedPos = nextPos.floorMod(IVec3(16))
                    nextVoxel = nextChunk.getVoxelAt(transformedPos, maxDepth)
                } else {
                    nextVoxel = chunk.getVoxelAt(nextPos, maxDepth)
                }
                if (nextVoxel == 0) {
                    val sidePos = position + (dir.sidePositionOffset * scaling)
                    val side = VoxelSide(sidePos, IVec2(scaling), dir, voxel)
                    val packed = side.packToInt()

                    buf.putInt(offset, packed)

                    offset += Int.SIZE_BYTES
                    instanceCount++
                }
            }
        }

        return ChunkHullData(buf, instanceCount)
    }
}