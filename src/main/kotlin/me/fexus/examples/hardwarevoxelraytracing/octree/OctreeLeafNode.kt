package me.fexus.examples.hardwarevoxelraytracing.octree

import me.fexus.math.vec.IVec3

class OctreeLeafNode(override val position: IVec3, override var nodeData: OctreeNodeData): IOctreeNode {
}