package me.fexus.examples.hardwarevoxelraytracing.octree

import me.fexus.math.vec.IVec3


class OctreeRootNode(
    override val position: IVec3,
    override var nodeData: OctreeNodeData
): IOctreeParentNode {
    override val children = Array<IOctreeNode?>(8) { null }
    val isEmpty: Boolean; get() = children.all { it == null }
}