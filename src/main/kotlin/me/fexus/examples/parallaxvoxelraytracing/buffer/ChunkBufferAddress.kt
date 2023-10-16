package me.fexus.examples.parallaxvoxelraytracing.buffer

data class ChunkBufferAddress(val bufferIndex: Int, val offset: Int) {
    init {
        if (bufferIndex > 15 || bufferIndex < 0) throw IndexOutOfBoundsException("$bufferIndex")
    }

    fun compress(): Int = bufferIndex or (offset shl 4)
}
