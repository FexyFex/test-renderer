package me.fexus.octree.compression.dag


data class DAGChildNodePointer(val octantIndex: Int, val offset: Int, val childIndex: Int) {
    companion object {
        const val SIZE_BYTES = Int.SIZE_BYTES * 3
    }
}