package me.fexus.vulkan.descriptors.buffer

import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlags
import me.fexus.vulkan.descriptors.buffer.usage.IBufferUsage
import org.lwjgl.vulkan.VK12.VK_SHARING_MODE_EXCLUSIVE


data class VulkanBufferConfiguration(
    val size: Long,
    val memoryProperties: MemoryPropertyFlags,
    val usage: IBufferUsage,
    val sharingMode: Int = VK_SHARING_MODE_EXCLUSIVE
)