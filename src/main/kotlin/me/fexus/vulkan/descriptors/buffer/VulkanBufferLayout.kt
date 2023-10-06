package me.fexus.vulkan.descriptors.buffer

import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperties
import me.fexus.vulkan.descriptors.buffer.usage.IBufferUsage
import org.lwjgl.vulkan.VK12.VK_SHARING_MODE_EXCLUSIVE


data class VulkanBufferLayout(
    val size: Long,
    val memoryProperties: MemoryProperties,
    val usage: IBufferUsage,
    val sharingMode: Int = VK_SHARING_MODE_EXCLUSIVE
)