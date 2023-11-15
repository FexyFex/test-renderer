package me.fexus.octree.compression

import me.fexus.octree.OctreeNodeDataVoxelType


data class IndexedLeafNode(override val index: Int, override val nodeData: OctreeNodeDataVoxelType): IIndexedOctreeNode