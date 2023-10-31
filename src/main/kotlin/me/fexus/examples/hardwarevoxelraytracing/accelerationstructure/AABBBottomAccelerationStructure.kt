package me.fexus.examples.hardwarevoxelraytracing.accelerationstructure

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperty
import me.fexus.vulkan.exception.catchVK
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRAccelerationStructure.*
import java.nio.ByteBuffer
import java.nio.ByteOrder


class AABBBottomAccelerationStructure: IAccelerationStructure {
    override var vkHandle: Long = 0L
    override var deviceAddress: Long = 0L
    override lateinit var buffer: VulkanBuffer

    private lateinit var aabbsBuffer: VulkanBuffer


    fun createAndBuild(deviceUtil: VulkanDeviceUtil, config: AABBBlasConfiguration) = runMemorySafe {
        createAABBBuffer(deviceUtil, config.aabbs)

        val geometryAABBsData = calloc(VkAccelerationStructureGeometryAabbsDataKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_AABBS_DATA_KHR)
            pNext(0L)
            data().deviceAddress(aabbsBuffer.getDeviceAddress())
            stride(AABB.SIZE_BYTES.toLong())
        }

        val geometryStructure = calloc(VkAccelerationStructureGeometryKHR::calloc, 1)
        geometryStructure[0]
            .sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_KHR)
            .pNext(0L)
            .flags(VK_GEOMETRY_OPAQUE_BIT_KHR or VK_GEOMETRY_NO_DUPLICATE_ANY_HIT_INVOCATION_BIT_KHR)
            .geometryType(VK_GEOMETRY_TYPE_AABBS_KHR)
            .geometry().aabbs(geometryAABBsData)

        val buildGeometryInfo = calloc(VkAccelerationStructureBuildGeometryInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_GEOMETRY_INFO_KHR)
            pNext(0L)
            flags(VK_BUILD_ACCELERATION_STRUCTURE_PREFER_FAST_TRACE_BIT_KHR)
            type(VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR)
            mode(VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR)
            pGeometries(geometryStructure)
        }

        val buildSizesInfo = calloc(VkAccelerationStructureBuildSizesInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_SIZES_INFO_KHR)
        }
        vkGetAccelerationStructureBuildSizesKHR(
            deviceUtil.vkDeviceHandle,
            VK_ACCELERATION_STRUCTURE_BUILD_TYPE_DEVICE_KHR,
            buildGeometryInfo,
            intArrayOf(1),
            buildSizesInfo
        )

        val blasBufferConfig = VulkanBufferConfiguration(
            buildSizesInfo.accelerationStructureSize(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.ACCELERATION_STRUCTURE_STORAGE_KHR + BufferUsage.SHADER_DEVICE_ADDRESS
        )
        this@AABBBottomAccelerationStructure.buffer = deviceUtil.createBuffer(blasBufferConfig)

        val blasCreateInfo = calloc(VkAccelerationStructureCreateInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_CREATE_INFO_KHR)
            pNext(0L)
            buffer(this@AABBBottomAccelerationStructure.buffer.vkBufferHandle)
            offset(0L)
            size(buildSizesInfo.accelerationStructureSize())
            type(VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR)
        }

        val pBLASHandle = allocateLong(1)
        vkCreateAccelerationStructureKHR(deviceUtil.vkDeviceHandle, blasCreateInfo, null, pBLASHandle).catchVK()
        this@AABBBottomAccelerationStructure.vkHandle = pBLASHandle[0]

        val buildRangeInfo = calloc(VkAccelerationStructureBuildRangeInfoKHR::calloc) {
            primitiveCount(1)
            primitiveOffset(0)
            firstVertex(0)
            transformOffset(0)
        }

        val ppBuildRangeInfos = allocatePointer(1)
        ppBuildRangeInfos.put(0, buildRangeInfo)

        val pBuildGeometryInfos = calloc(VkAccelerationStructureBuildGeometryInfoKHR::calloc, 1)
        pBuildGeometryInfos.put(0, buildGeometryInfo)

        val cmdBuf = deviceUtil.beginSingleTimeCommandBuffer()
        vkCmdBuildAccelerationStructuresKHR(
            cmdBuf.vkHandle,
            pBuildGeometryInfos,
            ppBuildRangeInfos
        )
        deviceUtil.endSingleTimeCommandBuffer(cmdBuf)

        val deviceAddressInfo = calloc(VkAccelerationStructureDeviceAddressInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_DEVICE_ADDRESS_INFO_KHR)
            accelerationStructure(this@AABBBottomAccelerationStructure.vkHandle)
        }
        deviceAddress = vkGetAccelerationStructureDeviceAddressKHR(deviceUtil.vkDeviceHandle, deviceAddressInfo)
    }


    private fun createAABBBuffer(deviceUtil: VulkanDeviceUtil, aabbs: List<AABB>) {
        val bufferSize = aabbs.size * AABB.SIZE_BYTES
        val aabbsBufferConfig = VulkanBufferConfiguration(
            bufferSize.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_KHR +
                    BufferUsage.SHADER_DEVICE_ADDRESS + BufferUsage.TRANSFER_DST
        )
        this.aabbsBuffer = deviceUtil.createBuffer(aabbsBufferConfig)

        val aabbsByteBuffer = ByteBuffer.allocate(bufferSize)
        aabbsByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        aabbs.forEachIndexed { index, aabb ->
            aabb.toByteBuffer(aabbsByteBuffer, index * AABB.SIZE_BYTES)
        }

        deviceUtil.stagingCopy(aabbsByteBuffer, aabbsBuffer, 0L, 0L, bufferSize.toLong())
    }
}