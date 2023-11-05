package me.fexus.examples.hardwarevoxelraytracing.octree

enum class ContentType(val value: Int) {
    EMPTY(0),
    BLOCK(1),
    NODES(2),
    VOXEL(3)
}