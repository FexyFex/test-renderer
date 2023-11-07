package me.fexus.examples.hardwarevoxelraytracing.octree

import me.fexus.math.vec.IVec3

class OctreeNode(
    override val position: IVec3,
    override var voxelData: OctreeNodeData
): IOctreeParentNode {
    override val children = Array<IOctreeNode?>(8) { null }
}