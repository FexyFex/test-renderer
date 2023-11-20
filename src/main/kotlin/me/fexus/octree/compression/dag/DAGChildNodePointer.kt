package me.fexus.octree.compression.dag


data class DAGChildNodePointer(val octantIndex: Int, val offset: Int, val childIndex: Int)