package me.fexus.examples.hardwarevoxelraytracing.octree.buffer

enum class NodeTypeSignature(val value: Byte) {
    PARENT(0),
    LEAF(1)
}