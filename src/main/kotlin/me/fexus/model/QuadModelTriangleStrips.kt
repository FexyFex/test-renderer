package me.fexus.model

import me.fexus.math.vec.Vec2
import me.fexus.math.vec.Vec3
import java.nio.ByteBuffer


object QuadModelTriangleStrips {
    private val pos1 = Vec3(0f, 0f, 0f)
    private val pos2 = Vec3(0f, 1f, 0f)
    private val pos3 = Vec3(1f, 0f, 0f)
    private val pos4 = Vec3(1f, 1f, 0f)

    private val uv1 = Vec2(0f ,1f)
    private val uv2 = Vec2(1f, 0f)
    private val uv3 = Vec2(0f, 0f)
    private val uv4 = Vec2(1f, 1f)

    val vertices = arrayOf(Vertex(pos1, uv1), Vertex(pos3, uv4), Vertex(pos2, uv3), Vertex(pos4, uv2))

    data class Vertex(val pos: Vec3, val uv: Vec2) {
        fun writeToByteBuffer(buffer: ByteBuffer, offset: Int) {
            pos.intoByteBuffer(buffer, offset)
            uv.toByteBuffer(buffer, offset + 12)
        }

        companion object {
            const val SIZE_BYTES = 20
        }
    }
}