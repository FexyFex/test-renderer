package me.fexus.octree.compression

import me.fexus.octree.OctreeNodeDataVoxelType


interface IIndexedOctreeNode {
    val index: Int
    val nodeData: OctreeNodeDataVoxelType
}