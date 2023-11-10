package me.fexus.examples.instancedrendering

import me.fexus.math.vec.Vec3
import me.fexus.math.vec.Vec4
import java.nio.ByteBuffer


data class VoxelData(val position: Vec3, val color: Vec4) {

    fun toByteBuffer(buffer: ByteBuffer, offset: Int) {
        position.toByteBuffer(buffer, offset)
        buffer.putFloat(offset + 12, 0.0f)
        color.toByteBuffer(buffer, offset + 16)
    }

    companion object {
        const val SIZE_BYTES = 32
    }
}
