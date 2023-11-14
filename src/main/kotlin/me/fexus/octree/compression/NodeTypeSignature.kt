package me.fexus.octree.compression

enum class NodeTypeSignature(val value: Byte) {
    PARENT(0),
    LEAF(1)
}