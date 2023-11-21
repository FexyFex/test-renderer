package me.fexus.voxel.octree.buffer.dag


data class DAGNode(val index: Int, val childCount: Int, val childPointers: List<DAGChildNodePointer>)