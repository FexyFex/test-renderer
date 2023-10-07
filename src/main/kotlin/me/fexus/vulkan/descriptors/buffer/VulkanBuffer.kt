package me.fexus.vulkan.descriptors.buffer

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperties
import me.fexus.vulkan.descriptors.buffer.usage.IBufferUsage
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperty
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK12.*
import java.nio.ByteBuffer


class VulkanBuffer(private val device: Device, val bufferHandle: Long, val memoryHandle: Long, val layout: VulkanBufferLayout) {
    private var mappingHandle: Long = -1L

    fun hasProperty(memProp: MemoryProperties) = memProp in layout.memoryProperties
    fun hasUsage(usage: IBufferUsage) = usage in layout.usage

    fun put(device: Device, data: ByteBuffer) {
        if (hasProperty(MemoryProperty.HOST_COHERENT + MemoryProperty.HOST_VISIBLE)) {
            // Copy can be done without staging
            val address = getMemoryMappingHandle(device)
            repeat(data.capacity()) {
                val offset = address + it
                MemoryUtil.memPutByte(offset, data[it])
            }
        } else {
            throw Exception("Device Local Buffers require Staging")
        }
    }

    private fun getMemoryMappingHandle(device: Device): Long = runMemorySafe {
        if (mappingHandle == -1L) {
            val ppMappingHandle = allocatePointer(1)
            vkMapMemory(device.vkHandle, memoryHandle, 0, layout.size, 0, ppMappingHandle)
            this@VulkanBuffer.mappingHandle = ppMappingHandle[0]
            return@runMemorySafe this@VulkanBuffer.mappingHandle
        } else return@runMemorySafe mappingHandle
    }

    fun destroy() {
        vkFreeMemory(device.vkHandle, memoryHandle, null)
        vkDestroyBuffer(device.vkHandle, bufferHandle, null)
        mappingHandle = -1L
    }
}