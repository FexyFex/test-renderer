package me.fexus.examples.hardwarevoxelraytracing.octree.compression

import me.fexus.examples.hardwarevoxelraytracing.voxel.type.VoxelType
import java.nio.ByteBuffer
import kotlin.experimental.and


data class VoxelData(val isVoid: Boolean, val textureIndex: Int) {
    constructor(voxelType: VoxelType): this(voxelType.id == 0, voxelType.id)


    fun toByteBuffer(buffer: ByteBuffer, offset: Int) {
        val unified = textureIndex or ((isVoid.toByte() and 1).toInt() shl 31)
        buffer.putInt(offset, unified)
    }

    companion object {
        const val SIZE_BYTES = Int.SIZE_BYTES

        private fun Boolean.toByte(): Byte = if (this) 1 else 0
    }
}
