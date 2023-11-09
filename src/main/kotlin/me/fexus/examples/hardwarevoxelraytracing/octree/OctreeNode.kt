package me.fexus.examples.hardwarevoxelraytracing.octree

import me.fexus.math.vec.IVec3

data class OctreeNode(
    override val position: IVec3,
    override var nodeData: OctreeNodeData
): IOctreeParentNode {
    override val children = Array<IOctreeNode?>(8) { null }
}