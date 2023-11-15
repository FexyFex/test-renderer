package me.fexus.octree.compression

import me.fexus.octree.OctreeNodeDataVoxelType


data class IndexedLeafNode(
    override val index: Int,
    override val nodeData: OctreeNodeDataVoxelType,
    override var hashValue: Int = 0
): IIndexedOctreeNode