package me.fexus.examples.hardwarevoxelraytracing.octree.compression

import me.fexus.examples.hardwarevoxelraytracing.octree.OctreeNodeDataVoxelType


data class IndexedLeafNode(override val index: Int, override val nodeData: OctreeNodeDataVoxelType): IIndexedOctreeNode