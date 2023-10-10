package me.fexus.examples.parallaxvoxelraytracing

import me.fexus.math.vec.Vec2
import me.fexus.math.vec.Vec3


object QuadWireFrame {
    private val pos1 = Vec3(-1.0f, +1.0f, 0.0f)
    private val pos2 = Vec3(-1.0f, -1.0f, 0.0f)
    private val pos3 = Vec3(+1.0f, -1.0f, 0.0f)
    private val pos4 = Vec3(+1.0f, +1.0f, 0.0f)

    private val uv1 = Vec2(0.0f, 1.0f)
    private val uv2 = Vec2(0.0f, 0.0f)
    private val uv3 = Vec2(1.0f, 0.0f)
    private val uv4 = Vec2(1.0f, 1.0f)

    private val norm = Vec3(0.0f, 0.0f, 1.0f)

    val vertices = arrayOf(
        // Position                   Normal                        Texcoord
        pos1.x, pos1.y, pos1.z, 1.0f, norm.x, norm.y, norm.z, 0.0f, uv1.x, uv1.y, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
        pos2.x, pos2.y, pos2.z, 1.0f, norm.x, norm.y, norm.z, 0.0f, uv2.x, uv2.y, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,

        pos2.x, pos2.y, pos2.z, 1.0f, norm.x, norm.y, norm.z, 0.0f, uv2.x, uv2.y, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
        pos3.x, pos3.y, pos3.z, 1.0f, norm.x, norm.y, norm.z, 0.0f, uv3.x, uv3.y, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,

        pos1.x, pos1.y, pos1.z, 1.0f, norm.x, norm.y, norm.z, 0.0f, uv1.x, uv1.y, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
        pos4.x, pos4.y, pos4.z, 1.0f, norm.x, norm.y, norm.z, 0.0f, uv4.x, uv4.y, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,

        pos3.x, pos3.y, pos3.z, 1.0f, norm.x, norm.y, norm.z, 0.0f, uv3.x, uv3.y, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
        pos4.x, pos4.y, pos4.z, 1.0f, norm.x, norm.y, norm.z, 0.0f, uv4.x, uv4.y, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
    )

    val SIZE_BYTES = vertices.size * Float.SIZE_BYTES
}