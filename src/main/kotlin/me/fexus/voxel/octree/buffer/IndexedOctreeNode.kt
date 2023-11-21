package me.fexus.voxel.octree.buffer

import me.fexus.voxel.octree.OctreeNodeDataVoxelType


data class IndexedOctreeNode(val index: Int, val nodeData: OctreeNodeDataVoxelType, val children: List<IndexedOctreeChildLink>)