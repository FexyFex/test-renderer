package me.fexus.octree.compression.dag

import java.nio.ByteBuffer


// Reference: https://github.com/Kotlin/kotlinx-io/blob/master/bytestring/common/src/ByteStringBuilder.kt
class DAGBufferBuilder(initialCapacity: Int = 0) {
    private var buffer = ByteArray(initialCapacity)
    private var offset = 0

    val bytesWritten: Int; get() = offset
    val capacity: Int; get() = buffer.size

    private fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size >= requiredCapacity) return

        val minSize = if (buffer.isEmpty()) 16 else (buffer.size * 1.5).toInt()
        val desiredSize = max(minSize, requiredCapacity)
    }
}

inline fun buildDAGBuffer(initialCapacity: Int = 0, buildBlock: DAGBufferBuilder.() -> Unit): ByteBuffer {

}