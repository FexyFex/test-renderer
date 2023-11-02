package me.fexus.examples.customvoxelraytracing

import me.fexus.examples.customvoxelraytracing.buffer.ChunkBufferAddress
import me.fexus.examples.customvoxelraytracing.buffer.ChunkDataBufferArray
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
        var hasBlocks = false
        val middle = IVec3(extent) / 2
        repeatCubed(extent) { x, y, z ->

            val x2 = x + chunkPos.x * extent
            val y2 = y + chunkPos.y * extent
            val z2 = z + chunkPos.z * extent
            if (y2 < 5*sin((x2+z2)*0.1)) {
                val offset = index * Int.SIZE_BYTES
                chunkData.putInt(offset, 1)
                hasBlocks = true
            }


            if ((middle - IVec3(x,y,z)).length < 3f) {
                val offset = index * Int.SIZE_BYTES
                chunkData.putInt(offset, 1)
            }
            index++
        }
        if (hasBlocks) this.address = chunkBufferArray.putChunkData(chunkPos, chunkData)
        else this.address = ChunkBufferAddress(0,0)
    }
}