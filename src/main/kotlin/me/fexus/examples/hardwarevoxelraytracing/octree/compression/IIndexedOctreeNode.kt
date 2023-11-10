package me.fexus.examples.hardwarevoxelraytracing.octree.compression

import me.fexus.examples.hardwarevoxelraytracing.octree.OctreeNodeDataVoxelType


interface IIndexedOctreeNode {
    val index: Int
    val nodeData: OctreeNodeDataVoxelType
}