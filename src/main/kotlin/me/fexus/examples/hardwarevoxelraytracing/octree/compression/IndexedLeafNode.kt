package me.fexus.examples.hardwarevoxelraytracing.octree.compression

import me.fexus.examples.hardwarevoxelraytracing.octree.OctreeNodeData


data class IndexedLeafNode(override val index: Int, override val nodeData: OctreeNodeData): IIndexedOctreeNode