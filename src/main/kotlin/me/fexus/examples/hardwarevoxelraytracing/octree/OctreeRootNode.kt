package me.fexus.examples.hardwarevoxelraytracing.octree

import me.fexus.math.vec.IVec3


class OctreeRootNode(override val position: IVec3, override val voxelData: OctreeNodeVoxelData, val children: MutableList<IOctreeNode>): IOctreeNode {
    val isEmpty: Boolean; get() = children.isEmpty()
}