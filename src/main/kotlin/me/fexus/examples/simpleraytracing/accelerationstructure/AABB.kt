package me.fexus.examples.simpleraytracing.accelerationstructure

import me.fexus.math.vec.Vec3
import java.nio.ByteBuffer

data class AABB(
    val minX: Float,
    val minY: Float,
    val minZ: Float,
    val maxX: Float,
    val maxY: Float,
    val maxZ: Float
) {
    constructor(min: Vec3, max: Vec3): this(min.x, min.y, min.z, max.x, max.y, max.z)


    fun toByteBuffer(targetBuf: ByteBuffer, offset: Int) {
        targetBuf.putFloat(offset, minX)
        targetBuf.putFloat(offset + 4, minY)
        targetBuf.putFloat(offset + 8, minZ)
        targetBuf.putFloat(offset + 12, maxX)
        targetBuf.putFloat(offset + 16, maxY)
        targetBuf.putFloat(offset + 20, maxZ)
    }

    companion object {
        const val SIZE_BYTES = Float.SIZE_BYTES * 6
    }
}
