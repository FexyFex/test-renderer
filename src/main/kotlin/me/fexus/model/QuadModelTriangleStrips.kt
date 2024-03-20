package me.fexus.model

import me.fexus.math.vec.Vec3
import java.nio.ByteBuffer


object QuadModelTriangleStrips {
    private val pos1 = Vec3(0f, 0f, 0f)
    private val pos2 = Vec3(0f, 1f, 0f)
    private val pos3 = Vec3(1f, 0f, 0f)
    private val pos4 = Vec3(1f, 1f, 0f)

    val vertices = arrayOf(Vertex(pos1), Vertex(pos3), Vertex(pos2), Vertex(pos4))

    data class Vertex(val pos: Vec3) {
        fun writeToByteBuffer(buffer: ByteBuffer, offset: Int) {
            pos.intoByteBuffer(buffer, offset)
            buffer.putFloat(offset + 12, 1.0f)
        }

        companion object {
            const val SIZE_BYTES = 16
        }
    }
}