package me.fexus.model

import me.fexus.math.vec.Vec2
import me.fexus.math.vec.Vec3


object CubeModel {
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
        Vertex(pos5, uv1, n1), Vertex(pos3, uv2, n1), Vertex(pos1, uv3, n1),
        Vertex(pos3, uv4, n2), Vertex(pos8, uv4, n2), Vertex(pos4, uv2, n2),
        Vertex(pos7, uv3, n3), Vertex(pos6, uv4, n3), Vertex(pos8, uv2, n3),
        Vertex(pos2, uv3, n4), Vertex(pos8, uv4, n4), Vertex(pos6, uv2, n4),
        Vertex(pos1, uv3, n5), Vertex(pos4, uv4, n5), Vertex(pos2, uv2, n5),
        Vertex(pos5, uv3, n6), Vertex(pos2, uv4, n6), Vertex(pos6, uv2, n6),
        Vertex(pos5, uv1, n1), Vertex(pos7, uv4, n1), Vertex(pos3, uv2, n1),
        Vertex(pos3, uv3, n2), Vertex(pos7, uv1, n2), Vertex(pos8, uv4, n2),
        Vertex(pos7, uv3, n3), Vertex(pos5, uv1, n3), Vertex(pos6, uv4, n3),
        Vertex(pos2, uv3, n4), Vertex(pos4, uv1, n4), Vertex(pos8, uv4, n4),
        Vertex(pos1, uv3, n5), Vertex(pos3, uv1, n5), Vertex(pos4, uv4, n5),
        Vertex(pos5, uv3, n6), Vertex(pos1, uv1, n6), Vertex(pos2, uv4, n6)
    )

    val verticesWireframe = arrayOf(
        Vertex(pos1, uv1, n1), Vertex(pos2, uv1, n1),
        Vertex(pos2, uv1, n1), Vertex(pos6, uv1, n1),
        Vertex(pos6, uv1, n1), Vertex(pos5, uv1, n1),
        Vertex(pos5, uv1, n1), Vertex(pos1, uv1, n1),

        Vertex(pos3, uv1, n1), Vertex(pos4, uv1, n1),
        Vertex(pos4, uv1, n1), Vertex(pos8, uv1, n1),
        Vertex(pos8, uv1, n1), Vertex(pos7, uv1, n1),
        Vertex(pos7, uv1, n1), Vertex(pos3, uv1, n1),

        Vertex(pos1, uv1, n1), Vertex(pos3, uv1, n1),
        Vertex(pos2, uv1, n1), Vertex(pos4, uv1, n1),
        Vertex(pos5, uv1, n1), Vertex(pos7, uv1, n1),
        Vertex(pos6, uv1, n1), Vertex(pos8, uv1, n1),
    )

    data class Vertex(val pos: Vec3, val uv: Vec2, val normal: Vec3) {
        fun toFloatArray() =
             floatArrayOf(pos.x, pos.y, pos.z, 1.0f, uv.x, uv.y, 0.0f, 0.0f, normal.x, normal.y, normal.z, 0.0f)

        companion object {
            const val SIZE_BYTES = 48
        }
    }
}