package me.fexus.octree.compression.dag


data class DAGNode(val index: Int, val childCount: Int, val childPointers: List<DAGChildNodePointer>)