package me.fexus.examples.hardwarevoxelraytracing.octree.compression

import me.fexus.examples.hardwarevoxelraytracing.octree.OctreeNodeData

data class IndexedParentNode(override val index: Int, override val nodeData: OctreeNodeData, override var hashValue: Int = 0): IIndexedOctreeNode {
    val children = Array<IIndexedOctreeNode?>(8) { null }

    val childCount: Int; get() = children.count { it != null }
}