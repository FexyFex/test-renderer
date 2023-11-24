package me.fexus.vulkan.descriptors.buffer

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperties
import me.fexus.vulkan.descriptors.buffer.usage.IBufferUsage
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperty
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkBufferDeviceAddressInfo
import java.nio.ByteBuffer


class VulkanBuffer(private val device: Device, val vkBufferHandle: Long, val vkMemoryHandle: Long, val config: VulkanBufferConfiguration) {
    private var mappingHandle: Long = -1L

    fun hasProperty(memProp: MemoryProperties) = memProp in config.memoryProperties
    fun hasUsage(usage: IBufferUsage) = usage in config.usage

    // This function works with HeapByteBuffers as well
    fun put(dstOffset: Int, data: ByteBuffer, srcOffset: Int = 0, srcSize: Int = data.capacity()) {
        if (hasProperty(MemoryProperty.HOST_COHERENT + MemoryProperty.HOST_VISIBLE)) {
            // Put can be done without staging
            val address = getMemoryMappingHandle() + dstOffset
            repeat(srcSize) {
                val lOffset = address + it
                MemoryUtil.memPutByte(lOffset, data[it + srcOffset])
            }
        } else {
            throw Exception("Device local buffers require staging")
        }
    }

    // This function only works with off-heap data
    fun copy(srcAddress: Long, dstAddress: Long, size: Long) {
        if (hasProperty(MemoryProperty.HOST_COHERENT + MemoryProperty.HOST_VISIBLE)) {
            // Copy can be done without staging
            MemoryUtil.memCopy(srcAddress, dstAddress, size)
        } else {
            throw Exception("Device local buffers require staging")
        }
    }

    fun putInt(offset: Int, value: Int) {
        if (hasProperty(MemoryProperty.HOST_COHERENT + MemoryProperty.HOST_VISIBLE)) {
            // Copy can be done without staging
            val address = getMemoryMappingHandle() + offset
            MemoryUtil.memPutInt(address, value)
        } else {
            throw Exception("Device local buffers require staging")
        }
    }

    fun set(offset: Int, value: Int, size: Long) {
        if (hasProperty(MemoryProperty.HOST_COHERENT + MemoryProperty.HOST_VISIBLE)) {
            // Copy can be done without staging
            val address = getMemoryMappingHandle()
            MemoryUtil.memSet(address + offset, value, size)
        } else {
            throw Exception("Device local buffers require staging")
        }
    }

    fun getFloat(offset: Int): Float {
        if (hasProperty(MemoryProperty.HOST_COHERENT + MemoryProperty.HOST_VISIBLE)) {
            // Copy can be done without staging
            val address = getMemoryMappingHandle() + offset
            return MemoryUtil.memGetFloat(address)
        } else {
            throw Exception("Device local buffers require staging")
        }
    }

    fun getInt(offset: Int): Int {
        if (hasProperty(MemoryProperty.HOST_COHERENT + MemoryProperty.HOST_VISIBLE)) {
            // Copy can be done without staging
            val address = getMemoryMappingHandle() + offset
            return MemoryUtil.memGetInt(address)
        } else {
            throw Exception("Device local buffers require staging")
        }
    }

    fun getDeviceAddress() = runMemorySafe {
        if (!hasUsage(BufferUsage.SHADER_DEVICE_ADDRESS))
            throw Exception("Only device local buffers and buffers flagged with usage DEVICE_ADDRESS_KHR may be assigned a device address")

        val addressInfo = calloc(VkBufferDeviceAddressInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_BUFFER_DEVICE_ADDRESS_INFO)
            pNext(0L)
            buffer(this@VulkanBuffer.vkBufferHandle)
        }

        return@runMemorySafe vkGetBufferDeviceAddress(device.vkHandle, addressInfo)
    }

    private fun getMemoryMappingHandle(): Long = runMemorySafe {
        if (mappingHandle == -1L) {
            val ppMappingHandle = allocatePointer(1)
            vkMapMemory(device.vkHandle, vkMemoryHandle, 0, config.size, 0, ppMappingHandle)
            this@VulkanBuffer.mappingHandle = ppMappingHandle[0]
            return@runMemorySafe this@VulkanBuffer.mappingHandle
        } else return@runMemorySafe mappingHandle
    }

    fun destroy() {
        vkFreeMemory(device.vkHandle, vkMemoryHandle, null)
        vkDestroyBuffer(device.vkHandle, vkBufferHandle, null)
        mappingHandle = -1L
    }
}