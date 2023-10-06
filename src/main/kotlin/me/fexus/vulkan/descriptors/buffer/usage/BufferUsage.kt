package me.fexus.vulkan.descriptors.buffer.usage

import org.lwjgl.vulkan.VK10


enum class BufferUsage(override val vkBits: Int): IBufferUsage {
    STORAGE_BUFFER(VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT),
    UNIFORM_BUFFER(VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT),
    VERTEX_BUFFER(VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT),
    INDEX_BUFFER(VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT),
    TRANSFER_DST(VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT),
    TRANSFER_SRC(VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT),
    INDIRECT(VK10.VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT)
}