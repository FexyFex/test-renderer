package me.fexus.examples.parallaxvoxelraytracing

import me.fexus.examples.parallaxvoxelraytracing.buffer.ChunkBufferAddress
import me.fexus.examples.parallaxvoxelraytracing.buffer.ChunkDataBufferArray
import me.fexus.math.repeatCubed
import me.fexus.math.vec.IVec3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin


class Chunk(val chunkPos: IVec3, extent: Int, chunkBufferArray: ChunkDataBufferArray) {
    val address: ChunkBufferAddress

    init {
        val chunkData = ByteBuffer.allocate(extent * extent * extent * Int.SIZE_BYTES)
        chunkData.order(ByteOrder.LITTLE_ENDIAN)
        var index = 0
        repeatCubed(extent) { x, y, z ->
            val x2 = x + chunkPos.x * extent
            val y2 = y + chunkPos.y * extent
            val z2 = z + chunkPos.z * extent
            if (y2 < 5*sin((x2+z2)*0.1)) {
                println(y2)
                val offset = index * Int.SIZE_BYTES
                chunkData.putInt(offset, 1)
            }
            index++
        }
        this.address = chunkBufferArray.putChunkData(chunkPos, chunkData)
    }
}