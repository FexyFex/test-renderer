package me.fexus.vulkan.descriptors.buffer

import me.fexus.vulkan.component.Device
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperties
import me.fexus.vulkan.descriptors.buffer.usage.IBufferUsage
import org.lwjgl.vulkan.VK10.vkDestroyBuffer
import org.lwjgl.vulkan.VK10.vkFreeMemory


class VulkanBuffer(private val device: Device, val bufferHandle: Long, val memoryHandle: Long, val layout: VulkanBufferLayout) {
    fun hasProperty(memProp: MemoryProperties) = memProp in layout.memoryProperties
    fun hasUsage(usage: IBufferUsage) = usage in layout.usage

    fun destroy() {
        vkFreeMemory(device.vkHandle, memoryHandle, null)
        vkDestroyBuffer(device.vkHandle, bufferHandle, null)
    }
}