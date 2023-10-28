package me.fexus.model

import me.fexus.math.vec.Vec3


object CubeModelPositionsOnly {
    private val pos1 = Vec3(1f, 1f, -1f)
    private val pos2 = Vec3(1f, -1f, -1f)
    private val pos3 = Vec3(1f, 1f, 1f)
    private val pos4 = Vec3(1f, -1f, 1f)
    private val pos5 = Vec3(-1f, 1f, -1f)
    private val pos6 = Vec3(-1f, -1f, -1f)
    private val pos7 = Vec3(-1f, 1f, 1f)
    private val pos8 = Vec3(-1f, -1f, 1f)

    val vertices = arrayOf(
        Vertex(pos1), Vertex(pos2), Vertex(pos3),
        Vertex(pos4), Vertex(pos5), Vertex(pos6),
        Vertex(pos7), Vertex(pos8)
    )

    val indices = intArrayOf(
        5, 3, 1,
        3, 8, 4,
        7, 6, 8,
        2, 8, 6,
        1, 4, 2,
        5, 2, 6,
        5, 7, 3,
        3, 7, 8,
        7, 5, 6,
        2, 4, 8,
        1, 3, 4,
        5, 1, 2
    )

    val verticesWireframe = arrayOf(
        Vertex(pos1), Vertex(pos2),
        Vertex(pos2), Vertex(pos6),
        Vertex(pos6), Vertex(pos5),
        Vertex(pos5), Vertex(pos1),

        Vertex(pos3), Vertex(pos4),
        Vertex(pos4), Vertex(pos8),
        Vertex(pos8), Vertex(pos7),
        Vertex(pos7), Vertex(pos3),

        Vertex(pos1), Vertex(pos3),
        Vertex(pos2), Vertex(pos4),
        Vertex(pos5), Vertex(pos7),
        Vertex(pos6), Vertex(pos8),
    )

    data class Vertex(val pos: Vec3) {
        fun toFloatArray() = floatArrayOf(pos.x, pos.y, pos.z, 1.0f)

        companion object {
            const val SIZE_BYTES = 16
        }
    }
}