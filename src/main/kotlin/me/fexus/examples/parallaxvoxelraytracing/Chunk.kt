package me.fexus.examples.parallaxvoxelraytracing

import me.fexus.examples.parallaxvoxelraytracing.buffer.ChunkBufferAddress
import me.fexus.examples.parallaxvoxelraytracing.buffer.ChunkDataBufferArray
import me.fexus.math.repeatCubed
import me.fexus.math.vec.IVec3
import java.nio.ByteBuffer
import java.nio.ByteOrder


class Chunk(val chunkPos: IVec3, extent: Int, chunkBufferArray: ChunkDataBufferArray) {
    val address: ChunkBufferAddress
    val blockArray: SparseBlockArray

    init {
        val blockArray = SparseBlockArray(extent)

        val chunkData = ByteBuffer.allocate(extent * extent * extent * Int.SIZE_BYTES)
        chunkData.order(ByteOrder.LITTLE_ENDIAN)
        var index = 0

        repeatCubed(extent) { lX, lY, lZ ->
            val x = lX + chunkPos.x * extent
            val y = lY + chunkPos.y * extent
            val z = lZ + chunkPos.z * extent
            var resultBlock = 0
            if (y < 4 || y > 24) {
                resultBlock = 1
                val offset = index * Int.SIZE_BYTES
                chunkData.putInt(offset, resultBlock)
                blockArray.set(lX, lY, lZ, resultBlock)
            }

            index++
        }
        this.address = chunkBufferArray.putChunkData(chunkPos, chunkData)
        this.blockArray = blockArray
    }
}