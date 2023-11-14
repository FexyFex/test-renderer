package me.fexus.octree.compression


data class SVOChildNodePointer(val octantIndex: Int, val offset: Int, val childIndex: Int) {
    companion object {
        const val SIZE_BYTES = Int.SIZE_BYTES * 3
    }
}