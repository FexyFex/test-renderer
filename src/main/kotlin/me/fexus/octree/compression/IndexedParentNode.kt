package me.fexus.octree.compression

import me.fexus.octree.OctreeNodeDataVoxelType

data class IndexedParentNode(override val index: Int, override val nodeData: OctreeNodeDataVoxelType):
    IIndexedOctreeNode {
    val children = Array<IIndexedOctreeNode?>(8) { null }

    val childCount: Int; get() = children.count { it != null }
}