package me.fexus.examples.compute.bulletlimbo

import me.fexus.memory.runMemorySafe
import me.fexus.model.QuadModel
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkBufferCopy
import java.nio.ByteBuffer
import java.nio.ByteOrder


class SpriteMesh {
    private lateinit var deviceUtil: VulkanDeviceUtil

    lateinit var vertexBuffer: VulkanBuffer; private set
    lateinit var indexBuffer: VulkanBuffer; private set


    fun init(deviceUtil: VulkanDeviceUtil) {
        this.deviceUtil = deviceUtil

        createMeshBuffers()
    }


    private fun createMeshBuffers() {
        // -- VERTEX BUFFER --
        val vertexBufferSize = QuadModel.vertices.size * QuadModel.Vertex.SIZE_BYTES
        val vertexBufferData = ByteBuffer.allocate(vertexBufferSize)
        vertexBufferData.order(ByteOrder.LITTLE_ENDIAN)
        QuadModel.vertices.forEachIndexed { index, vertex ->
            val offset = index * QuadModel.Vertex.SIZE_BYTES
            vertex.writeToByteBuffer(vertexBufferData, offset)
        }
        val vertexBufferLayout = VulkanBufferConfiguration(
            vertexBufferSize.toLong(),
            MemoryPropertyFlag.DEVICE_LOCAL,
            BufferUsage.VERTEX_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.vertexBuffer = deviceUtil.createBuffer(vertexBufferLayout)
        // Staging
        val stagingVertexBufferLayout = VulkanBufferConfiguration(
            vertexBufferSize.toLong(),
            MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.TRANSFER_SRC
        )
        val stagingVertexBuffer = deviceUtil.createBuffer(stagingVertexBufferLayout)
        stagingVertexBuffer.put(0, vertexBufferData)
        // Copy from Staging to Vertex Buffer
        runMemorySafe {
            val cmdBuf = deviceUtil.beginSingleTimeCommandBuffer()

            val copyRegion = calloc(VkBufferCopy::calloc, 1)
            copyRegion[0]
                .srcOffset(0)
                .dstOffset(0)
                .size(vertexBufferSize.toLong())
            vkCmdCopyBuffer(
                cmdBuf.vkHandle,
                stagingVertexBuffer.vkBufferHandle,
                vertexBuffer.vkBufferHandle,
                copyRegion
            )

            deviceUtil.endSingleTimeCommandBuffer(cmdBuf)
        }
        stagingVertexBuffer.destroy()
        // -- VERTEX BUFFER --

        // -- INDEX BUFFER --
        val indexBufferSize = QuadModel.indices.size * Int.SIZE_BYTES
        val indexBufferData = ByteBuffer.allocate(indexBufferSize)
        indexBufferData.order(ByteOrder.LITTLE_ENDIAN)
        QuadModel.indices.forEachIndexed { index, vertexIndex ->
            val offset = index * Int.SIZE_BYTES
            indexBufferData.putInt(offset, vertexIndex)
        }
        val indexBufferConfig = VulkanBufferConfiguration(
            indexBufferSize.toLong(),
            MemoryPropertyFlag.DEVICE_LOCAL,
            BufferUsage.INDEX_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.indexBuffer = deviceUtil.createBuffer(indexBufferConfig)
        val stagingIndexBufferConfig = VulkanBufferConfiguration(
            indexBufferSize.toLong(),
            MemoryPropertyFlag.HOST_COHERENT + MemoryPropertyFlag.HOST_VISIBLE,
            BufferUsage.TRANSFER_SRC
        )
        val stagingIndexBuffer = deviceUtil.createBuffer(stagingIndexBufferConfig)
        stagingIndexBuffer.put(0, indexBufferData)

        runMemorySafe {
            val cmdBuf = deviceUtil.beginSingleTimeCommandBuffer()

            val copyRegion = calloc(VkBufferCopy::calloc, 1)
            copyRegion[0]
                .srcOffset(0)
                .dstOffset(0)
                .size(indexBufferSize.toLong())
            vkCmdCopyBuffer(
                cmdBuf.vkHandle,
                stagingIndexBuffer.vkBufferHandle,
                indexBuffer.vkBufferHandle,
                copyRegion
            )

            deviceUtil.endSingleTimeCommandBuffer(cmdBuf)
        }
        stagingIndexBuffer.destroy()
        // -- INDEX BUFFER --
    }


    fun destroy() {

    }
}