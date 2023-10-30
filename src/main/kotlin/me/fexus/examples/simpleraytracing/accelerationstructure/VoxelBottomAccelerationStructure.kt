package me.fexus.examples.simpleraytracing.accelerationstructure

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.VulkanBufferFactory
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperty
import org.lwjgl.vulkan.KHRAccelerationStructure
import org.lwjgl.vulkan.KHRAccelerationStructure.VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_AABBS_DATA_KHR
import org.lwjgl.vulkan.VkAabbPositionsKHR
import org.lwjgl.vulkan.VkAccelerationStructureGeometryAabbsDataKHR
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoxelBottomAccelerationStructure {
    var vkHandle: Long = 0L; private set
    lateinit var buffer: VulkanBuffer; private set

    private lateinit var aabbsBuffer: VulkanBuffer


    fun createAndBuild(device: Device, bufferFactory: VulkanBufferFactory, aabbs: List<AABB>) = runMemorySafe {
        createAABBBuffer(bufferFactory, aabbs)

        val geometryAABBsData = calloc(VkAccelerationStructureGeometryAabbsDataKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_AABBS_DATA_KHR)
            pNext(0L)
            data()
            stride()
        }
    }


    private fun createAABBBuffer(bufferFactory: VulkanBufferFactory, aabbs: List<AABB>) {
        val aabbsBufferConfig = VulkanBufferConfiguration(
            aabbs.size * AABB.SIZE_BYTES.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_KHR +
                    BufferUsage.SHADER_DEVICE_ADDRESS + BufferUsage.TRANSFER_DST
        )
        this.aabbsBuffer = bufferFactory.createBuffer(aabbsBufferConfig)

        val aabbsByteBuffer = ByteBuffer.allocate(aabbs.size * AABB.SIZE_BYTES)
        aabbsByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    }
}