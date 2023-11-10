package me.fexus.examples.hardwarevoxelraytracing.voxel

import me.fexus.math.vec.Vec4


enum class VoxelColor(val intValue: Int, val colorVec4: Vec4) {
    INVISIBLE(0, Vec4(0f)),
    WHITE(-1, Vec4(1f)),
    GRAY(0x08080808, Vec4(0.5f)),
    BLACK(255, Vec4(0f,0f,0f,1f))
}