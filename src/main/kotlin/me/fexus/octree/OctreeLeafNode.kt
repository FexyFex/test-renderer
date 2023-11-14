package me.fexus.octree

import me.fexus.math.vec.IVec3

data class OctreeLeafNode<T: IOctreeNodeData>(override val position: IVec3, override var nodeData: T): IOctreeNode<T>