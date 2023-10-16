package me.fexus.examples.parallaxvoxelraytracing

import me.fexus.examples.parallaxvoxelraytracing.buffer.ChunkBufferAddress
import me.fexus.examples.parallaxvoxelraytracing.buffer.ChunkDataBufferArray
import me.fexus.math.repeatCubed
import me.fexus.math.vec.IVec3
import java.nio.ByteBuffer


class Chunk(val chunkPos: IVec3, extent: Int, chunkBufferArray: ChunkDataBufferArray) {
    val address: ChunkBufferAddress

    init {
        val chunkData = ByteBuffer.allocate(extent * extent * extent)
        var index = 0
        repeatCubed(extent) { x, y, z ->
            if (y < 4 || y > 24) {
                val offset = index++ * Int.SIZE_BYTES
                chunkData.putInt(offset, 1)
            }
        }
        this.address = chunkBufferArray.putChunkData(chunkPos, chunkData)
    }
}