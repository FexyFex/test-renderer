package me.fexus.examples.hardwarevoxelraytracing.voxel

import me.fexus.math.vec.Vec4


enum class VoxelColor(val colorVec4: Vec4) {
    INVISIBLE(Vec4(0f)),
    BLACK(Vec4(0f,0f,0f,1f)),
    WHITE(Vec4(1f)),
    LIGHT_GRAY(Vec4(0.8f, 0.8f, 0.8f, 1f)),
    GRAY(Vec4(0.5f, 0.5f, 0.5f, 1.0f)),
    DARK_GRAY(Vec4(0.25f, 0.25f, 0.25f, 1f)),
    BROWN(Vec4(0.6f, 0.3f, 0.1f, 1.0f)),
    DARK_BROWN(Vec4(0.4f, 0.1f, 0.03f, 1.0f));

    val intValue: Int = (colorVec4.x.toInt() * 255) or
                ((colorVec4.y.toInt() * 255) shl 8) or
                ((colorVec4.z.toInt() * 255) shl 16) or
                ((colorVec4.w.toInt() * 255) shl 24)
}