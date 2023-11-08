package me.fexus.examples.hardwarevoxelraytracing.octree.compression

import me.fexus.examples.hardwarevoxelraytracing.octree.OctreeNodeData


interface IIndexedOctreeNode {
    val index: Int
    val nodeData: OctreeNodeData
}