package me.fexus.examples.coolvoxelrendering

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.buffer.usage.IBufferUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
import org.lwjgl.vulkan.VK10.vkCmdCopyBuffer
import org.lwjgl.vulkan.VkBufferCopy
import java.nio.ByteBuffer


class MeshUploader(private val deviceUtil: VulkanDeviceUtil) {


    fun uploadBuffer(vertexData: ByteBuffer, usage: IBufferUsage): VulkanBuffer {
        val size = vertexData.capacity().toLong()

        val vertexBufferConfig = VulkanBufferConfiguration(
            size, MemoryPropertyFlag.DEVICE_LOCAL, usage + BufferUsage.TRANSFER_DST
        )
        val buffer = deviceUtil.createBuffer(vertexBufferConfig)

        val stagingBuffer = createStagingBuffer(size)
        stagingBuffer.put(0, vertexData)

        runMemorySafe {
            val cmdBuf = deviceUtil.beginSingleTimeCommandBuffer()

            val copyRegion = calloc(VkBufferCopy::calloc, 1)
            with(copyRegion[0]) {
                size(size)
                dstOffset(0)
                srcOffset(0)
            }

            vkCmdCopyBuffer(cmdBuf.vkHandle, stagingBuffer.vkBufferHandle, buffer.vkBufferHandle, copyRegion)

            deviceUtil.endSingleTimeCommandBuffer(cmdBuf)
        }

        stagingBuffer.destroy()

        return buffer
    }

    private fun createStagingBuffer(size: Long): VulkanBuffer {
        val stagingBufferConfig = VulkanBufferConfiguration(
            size,
            MemoryPropertyFlag.HOST_COHERENT + MemoryPropertyFlag.HOST_VISIBLE,
            BufferUsage.TRANSFER_SRC
        )

        return deviceUtil.createBuffer(stagingBufferConfig)
    }
}