package me.fexus.voxel.octree

import me.fexus.math.vec.IVec3


interface IOctreeNode<T: IOctreeNodeData> {
    val position: IVec3
    var nodeData: T
}