package me.fexus.examples.customvoxelraytracing.buffer

import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import java.nio.ByteBuffer


class ChunkBuffer(val index: Int, val vulkanBuffer: VulkanBuffer, size: Int) {
    private val freeRanges = mutableListOf<BufferRange>(BufferRange(0, size))


    fun allocate(data: ByteBuffer): Int? {
        val offset = reserveAndGetFreeOffset(data.capacity()) ?: return null
        vulkanBuffer.put(offset, data)
        return offset
    }

    private fun reserveAndGetFreeOffset(size: Int): Int? {
        val targetRange = freeRanges.firstOrNull { it.size >= size } ?: return null
        freeRanges.remove(targetRange)
        if (targetRange.size > size) {
            val newRange = BufferRange(targetRange.start + size, targetRange.end)
            freeRanges.add(newRange)
        }
        return targetRange.start
    }


    fun destroy() {
        vulkanBuffer.destroy()
    }


    // End is non-inclusive
    private data class BufferRange(val start: Int, val end: Int) {
        val size = end - start
    }
}