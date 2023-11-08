package me.fexus.examples.hardwarevoxelraytracing.octree.compression

data class SVONode(val index: Int, val childCount: Int, val childPointers: Array<SVOChildNodePointer>)