package me.fexus.model

import me.fexus.math.vec.Vec2
import me.fexus.math.vec.Vec3


object CubemapModel {
    private val pos1 = Vec3(1f, 1f, -1f)
    private val pos2 = Vec3(1f, -1f, -1f)
    private val pos3 = Vec3(1f, 1f, 1f)
    private val pos4 = Vec3(1f, -1f, 1f)
    private val pos5 = Vec3(-1f, 1f, -1f)
    private val pos6 = Vec3(-1f, -1f, -1f)
    private val pos7 = Vec3(-1f, 1f, 1f)
    private val pos8 = Vec3(-1f, -1f, 1f)

    private val uv1 = Vec2(1f, 1f)
    private val uv2 = Vec2(0f, 0f)
    private val uv3 = Vec2(1f, 0f)
    private val uv4 = Vec2(0f, 1f)

    private val n1 = Vec3(0f, 1f, 0f)
    private val n2 = Vec3(0f, 0f, 1f)
    private val n3 = Vec3(-1f, 0f, 0f)
    private val n4 = Vec3(0f, -1f, 0f)
    private val n5 = Vec3(1f, 0f, 0f)
    private val n6 = Vec3(0f, 0f, -1f)

    val vertices = arrayOf(
        // TOP
        Vertex(pos5, uv1, 0, n1), Vertex(pos7, uv3, 0, n1), Vertex(pos3, uv2, 0, n1),
        Vertex(pos5, uv1, 0, n1), Vertex(pos3, uv2, 0, n1), Vertex(pos1, uv4, 0, n1),

        // RIGHT
        Vertex(pos1, uv3, 1, n5), Vertex(pos4, uv4, 1, n5), Vertex(pos2, uv1, 1, n5),
        Vertex(pos1, uv3, 1, n5), Vertex(pos3, uv2, 1, n5), Vertex(pos4, uv4, 1, n5),

        // FRONT
        Vertex(pos5, uv3, 2, n6), Vertex(pos1, uv2, 2, n6), Vertex(pos2, uv4, 2, n6),
        Vertex(pos5, uv3, 2, n6), Vertex(pos2, uv4, 2, n6), Vertex(pos6, uv1, 2, n6),

        // LEFT
        Vertex(pos7, uv3, 3, n3), Vertex(pos6, uv4, 3, n3), Vertex(pos8, uv1, 3, n3),
        Vertex(pos7, uv3, 3, n3), Vertex(pos5, uv2, 3, n3), Vertex(pos6, uv4, 3, n3),

        // BACK
        Vertex(pos3, uv3, 4, n2), Vertex(pos8, uv4, 4, n2), Vertex(pos4, uv1, 4, n2),
        Vertex(pos3, uv3, 4, n2), Vertex(pos7, uv2, 4, n2), Vertex(pos8, uv4, 4, n2),

        // BOTTOM
        Vertex(pos2, uv2, 5, n4), Vertex(pos8, uv1, 5, n4), Vertex(pos6, uv3, 5, n4),
        Vertex(pos2, uv2, 5, n4), Vertex(pos4, uv4, 5, n4), Vertex(pos8, uv1, 5, n4),
    )

    data class Vertex(val pos: Vec3, val uv: Vec2, val textureIndex: Int, val normal: Vec3) {
        fun toFloatArray() =
             floatArrayOf(pos.x, pos.y, pos.z, 1.0f, uv.x, uv.y, textureIndex.toFloat(), 0.0f, normal.x, normal.y, normal.z, 0.0f)

        companion object {
            const val SIZE_BYTES = 48
        }
    }
}