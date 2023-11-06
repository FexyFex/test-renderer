package me.fexus.examples.hardwarevoxelraytracing.octree

import me.fexus.math.vec.IVec3

class OctreeNode(override val position: IVec3, val children: MutableList<IOctreeNode>): IOctreeNode