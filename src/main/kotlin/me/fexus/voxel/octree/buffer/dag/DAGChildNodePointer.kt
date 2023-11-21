package me.fexus.voxel.octree.buffer.dag


data class DAGChildNodePointer(val octantIndex: Int, val offset: Int, val childIndex: Int)