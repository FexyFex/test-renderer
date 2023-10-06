package me.fexus.examples.parallaxmapping

import me.fexus.math.vec.Vec2
import me.fexus.math.vec.Vec3


object ParallaxMappingQuadModel {
    private val pos1 = Vec3(-1.0f, +1.0f, 0.0f)
    private val pos2 = Vec3(-1.0f, -1.0f, 0.0f)
    private val pos3 = Vec3(+1.0f, -1.0f, 0.0f)
    private val pos4 = Vec3(+1.0f, +1.0f, 0.0f)

    private val uv1 = Vec2(0.0f, 1.0f)
    private val uv2 = Vec2(0.0f, 0.0f)
    private val uv3 = Vec2(1.0f, 0.0f)
    private val uv4 = Vec2(1.0f, 1.0f)

    private val norm = Vec3(0.0f, 0.0f, 1.0f)

    // Triangle 1
    private val edge1 = pos2 - pos1
    private val edge2 = pos3 - pos1
    private val deltaUV1 = uv2 - uv1
    private val deltaUV2 = uv3 - uv1

    private val f1 = 1.0f / (deltaUV1.x * deltaUV2.y - deltaUV2.x * deltaUV1.y)

    private val tangent1 = Vec3(
        f1 * (deltaUV2.y * edge1.x - deltaUV1.y * edge2.x),
        f1 * (deltaUV2.y * edge1.y - deltaUV1.y * edge2.y),
        f1 * (deltaUV2.y * edge1.z - deltaUV1.y * edge2.z)
    ).normalize()

    private val bitangent1 = Vec3(
        f1 * (-deltaUV2.x * edge1.x + deltaUV1.x * edge2.x),
        f1 * (-deltaUV2.x * edge1.y + deltaUV1.x * edge2.y),
        f1 * (-deltaUV2.x * edge1.z + deltaUV1.x * edge2.z)
    ).normalize()

    // Triangle 2
    private val edge3 = pos3 - pos1
    private val edge4 = pos4 - pos1
    private val deltaUV3 = uv3 - uv1
    private val deltaUV4 = uv4 - uv1

    private val f2 = 1.0f / (deltaUV3.x * deltaUV4.y - deltaUV2.x * deltaUV1.y)

    private val tangent2 = Vec3(
        f2 * (deltaUV4.y * edge3.x - deltaUV3.y * edge4.x),
        f2 * (deltaUV4.y * edge3.y - deltaUV3.y * edge4.y),
        f2 * (deltaUV4.y * edge3.z - deltaUV3.y * edge4.z)
    ).normalize()

    private val bitangent2 = Vec3(
        f2 * (-deltaUV4.x * edge3.x + deltaUV3.x * edge4.x),
        f2 * (-deltaUV4.x * edge3.y + deltaUV3.x * edge4.y),
        f2 * (-deltaUV4.x * edge3.z + deltaUV3.x * edge4.z)
    ).normalize()

    val vertices = arrayOf(
        // Position             Normal                  Texcoord      Tangent                             Bitangent
        pos1.x, pos1.y, pos1.z, norm.x, norm.y, norm.z, uv1.x, uv1.y, tangent1.x, tangent1.y, tangent1.z, bitangent1.x, bitangent1.y, bitangent1.z,
        pos2.x, pos2.y, pos2.z, norm.x, norm.y, norm.z, uv2.x, uv2.y, tangent1.x, tangent1.y, tangent1.z, bitangent1.x, bitangent1.y, bitangent1.z,
        pos3.x, pos3.y, pos3.z, norm.x, norm.y, norm.z, uv3.x, uv3.y, tangent1.x, tangent1.y, tangent1.z, bitangent1.x, bitangent1.y, bitangent1.z,

        pos1.x, pos1.y, pos1.z, norm.x, norm.y, norm.z, uv1.x, uv1.y, tangent2.x, tangent2.y, tangent2.z, bitangent2.x, bitangent2.y, bitangent2.z,
        pos3.x, pos3.y, pos3.z, norm.x, norm.y, norm.z, uv3.x, uv3.y, tangent2.x, tangent2.y, tangent2.z, bitangent2.x, bitangent2.y, bitangent2.z,
        pos4.x, pos4.y, pos4.z, norm.x, norm.y, norm.z, uv4.x, uv4.y, tangent2.x, tangent2.y, tangent2.z, bitangent2.x, bitangent2.y, bitangent2.z
    )
}