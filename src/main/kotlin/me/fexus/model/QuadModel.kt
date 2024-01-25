package me.fexus.model

import me.fexus.math.vec.Vec2
import me.fexus.math.vec.Vec3
import java.nio.ByteBuffer


object QuadModel {
    private val pos1 = Vec3(0f, 0f, 0f)
    private val pos2 = Vec3(0f, 1f, 0f)
    private val pos3 = Vec3(1f, 1f, 0f)
    private val pos4 = Vec3(1f, 0f, 0f)

    private val uv1 = Vec2(0f, 0f)
    private val uv2 = Vec2(0f, 1f)
    private val uv3 = Vec2(1f, 1f)
    private val uv4 = Vec2(1f, 0f)

    val vertices = arrayOf(Vertex(pos1, uv1), Vertex(pos2, uv2), Vertex(pos3, uv3), Vertex(pos4, uv4))
    val indices = arrayOf(0, 1, 2, 2, 3, 0)

    data class Vertex(val pos: Vec3, val uv: Vec2) {
        fun toFloatArray() = floatArrayOf(pos.x, pos.y, pos.z, 1.0f, uv.x, uv.y, 0.0f, 0.0f)

        fun writeToByteBuffer(buffer: ByteBuffer, offset: Int) {
            pos.toByteBuffer(buffer, offset)
            buffer.putFloat(offset + 12, 1.0f)
            uv.toByteBuffer(buffer, offset + 16)
        }

        companion object {
            const val SIZE_BYTES = 32
        }
    }
}