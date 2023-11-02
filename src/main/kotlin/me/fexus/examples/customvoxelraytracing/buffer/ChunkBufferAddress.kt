package me.fexus.examples.customvoxelraytracing.buffer

data class ChunkBufferAddress(val bufferIndex: Int, val index: Int) {
    init {
        if (bufferIndex > 15 || bufferIndex < 0) throw IndexOutOfBoundsException("$bufferIndex")
    }

    fun compress(): Int = bufferIndex or (index shl 4)
}
