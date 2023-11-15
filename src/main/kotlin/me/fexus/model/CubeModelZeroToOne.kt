package me.fexus.model

import me.fexus.math.vec.Vec3
import java.nio.ByteBuffer


object CubeModelZeroToOne {
    private val pos1 = Vec3(1f, 1f, 0f)
    private val pos2 = Vec3(1f, 0f, 0f)
    private val pos3 = Vec3(1f, 1f, 1f)
    private val pos4 = Vec3(1f, 0f, 1f)
    private val pos5 = Vec3(0f, 1f, 0f)
    private val pos6 = Vec3(0f, 0f, 0f)
    private val pos7 = Vec3(0f, 1f, 1f)
    private val pos8 = Vec3(0f, 0f, 1f)

    val vertices = arrayOf(
        Vertex(pos1), Vertex(pos2), Vertex(pos3),
        Vertex(pos4), Vertex(pos5), Vertex(pos6),
        Vertex(pos7), Vertex(pos8)
    )

    val indices = intArrayOf(
        4, 2, 0,
        2, 7, 3,
        6, 5, 7,
        1, 7, 5,
        0, 3, 1,
        4, 1, 5,
        4, 6, 2,
        2, 6, 7,
        6, 4, 5,
        1, 3, 7,
        0, 2, 3,
        4, 0, 1
    )

    val wireframeIndices = intArrayOf(
        0, 1,
        1, 5,
        5, 4,
        4, 0,

        2, 3,
        3, 7,
        7, 6,
        6, 2,

        0, 2,
        1, 3,
        4, 6,
        5, 7
    )

    data class Vertex(val pos: Vec3) {
        fun toFloatArray() = floatArrayOf(pos.x, pos.y, pos.z, 1.0f)
        fun writeToByteBuffer(buffer: ByteBuffer, offset: Int) {
            buffer.putFloat(offset, pos.x)
            buffer.putFloat(offset + 4, pos.y)
            buffer.putFloat(offset + 8, pos.z)
            buffer.putFloat(offset + 12, 1f)
        }

        companion object {
            const val SIZE_BYTES = 16
        }
    }
}