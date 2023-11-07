package me.fexus.examples.hardwarevoxelraytracing.octree

import me.fexus.math.vec.IVec3


interface IOctreeNode {
    val position: IVec3
    var voxelData: OctreeNodeData
}