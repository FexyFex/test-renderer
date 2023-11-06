package me.fexus.examples.hardwarevoxelraytracing.voxel

enum class VoxelColor(val value: Int) {
    INVISIBLE(0),
    WHITE(-1),
    GRAY(0x08080808)
}