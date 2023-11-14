package me.fexus.octree

import me.fexus.math.vec.IVec3

data class OctreeForkNode<T: IOctreeNodeData>(
    override val position: IVec3,
    override var nodeData: T
): IOctreeParentNode<T> {
    override val children = Array<IOctreeNode<T>?>(8) { null }
}