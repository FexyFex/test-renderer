package me.fexus.examples.hardwarevoxelraytracing.octree

import me.fexus.math.vec.IVec3

data class OctreeNode<T: IOctreeNodeData>(
    override val position: IVec3,
    override var nodeData: T
): IOctreeParentNode<T> {
    override val children = Array<IOctreeNode<T>?>(8) { null }
}