package me.fexus.examples.coolvoxelrendering.world.chunk.hull

import me.fexus.examples.coolvoxelrendering.world.Chunk
import me.fexus.examples.coolvoxelrendering.VoxelSide
import me.fexus.examples.coolvoxelrendering.VoxelSideDirection
import me.fexus.math.vec.IVec2
import me.fexus.voxel.VoxelOctree
import me.fexus.voxel.type.VoidVoxel
import me.fexus.voxel.type.VoxelType
import java.nio.ByteBuffer
import java.nio.ByteOrder


class ChunkHullBuilderSimple: ChunkHullBuilder {
    private val directions = VoxelSideDirection.values()

    override fun build(chunk: Chunk, lod: Int): ChunkHullData {
        val buf = ByteBuffer.allocate(VoxelOctree.VOXEL_COUNT * 6)
        buf.order(ByteOrder.LITTLE_ENDIAN)

        val scaling = VoxelOctree.EXTENT shr (lod + 1)

        var instanceCount = 0
        var offset = 0

        chunk.forEachVoxel(lod) { position, voxel ->
            directions.forEach { dir ->
                val nextPos = position + (dir.normal * scaling)
                val nextVoxel: VoxelType = try {
                    chunk.getVoxelAt(nextPos, lod)
                } catch (e: Exception) {
                    VoidVoxel
                }
                if (nextVoxel == VoidVoxel) {
                    val sidePos = position + (dir.sidePositionOffset * scaling)
                    val side = VoxelSide(sidePos, IVec2(scaling), dir, voxel.id - 1)
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