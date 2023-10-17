package me.fexus.examples.parallaxvoxelraytracing.buffer

data class ChunkBufferAddress(val bufferIndex: Int, var startIndex: Int) {
    init {
        if (bufferIndex > 15 || bufferIndex < 0) throw IndexOutOfBoundsException("$bufferIndex")
    }

    fun compress(): Int = bufferIndex or (startIndex shl 4)
}
