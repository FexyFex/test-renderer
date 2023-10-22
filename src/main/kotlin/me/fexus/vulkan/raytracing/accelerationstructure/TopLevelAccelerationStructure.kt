package me.fexus.vulkan.raytracing.accelerationstructure

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferFactory
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperty
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRAccelerationStructure.*


class TopLevelAccelerationStructure {
    var vkHandle: Long = 0L; private set
    var deviceAddress: Long = 0L; private set
    lateinit var buffer: VulkanBuffer; private set


    fun createAndBuild(
            device: Device,
            bufferFactory: VulkanBufferFactory,
            commandBuffer: CommandBuffer,
            config: TopLevelAccelerationStructureConfiguration
    ) = runMemorySafe {
        val accelerationStructureGeometry = calloc(VkAccelerationStructureGeometryKHR::calloc, 1)
        accelerationStructureGeometry[0]
                .sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_KHR)
                .pNext(0L)
                .geometryType(VK_GEOMETRY_TYPE_INSTANCES_KHR)
                .flags(VK_GEOMETRY_OPAQUE_BIT_KHR)
        accelerationStructureGeometry[0].geometry().instances().sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_INSTANCES_DATA_KHR)
        accelerationStructureGeometry[0].geometry().instances().arrayOfPointers(false)
        accelerationStructureGeometry[0].geometry().instances().data(
                calloc(VkDeviceOrHostAddressConstKHR::calloc) { deviceAddress(config.instanceDataBuffer.getDeviceAddress()) }
        )

        val buildGeometryInfo = calloc(VkAccelerationStructureBuildGeometryInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_GEOMETRY_INFO_KHR)
            pNext(0L)
            flags(VK_BUILD_ACCELERATION_STRUCTURE_PREFER_FAST_TRACE_BIT_KHR)
            type(VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR)
            geometryCount(1)
            pGeometries(accelerationStructureGeometry)
        }

        val pPrimitivesCount = allocateIntValues(1)
        val buildSizesInfo = calloc(VkAccelerationStructureBuildSizesInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_SIZES_INFO_KHR)
        }
        vkGetAccelerationStructureBuildSizesKHR(
                device.vkHandle,
                VK_ACCELERATION_STRUCTURE_BUILD_TYPE_DEVICE_KHR,
                buildGeometryInfo,
                pPrimitivesCount,
                buildSizesInfo
        )

        val asBufferLayout = VulkanBufferConfiguration(
                buildSizesInfo.accelerationStructureSize(),
                MemoryProperty.DEVICE_LOCAL,
                BufferUsage.ACCELERATION_STRUCTURE_STORAGE_KHR + BufferUsage.SHADER_DEVICE_ADDRESS
        )
        this@TopLevelAccelerationStructure.buffer = bufferFactory.createBuffer(asBufferLayout)

        val createInfo = calloc(VkAccelerationStructureCreateInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_CREATE_INFO_KHR)
            pNext(0L)
            buffer(this@TopLevelAccelerationStructure.buffer.vkBufferHandle)
            size(buildSizesInfo.accelerationStructureSize())
            type(VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR)
        }
        val pAccelerationStructureHandle = allocateLong(1)
        vkCreateAccelerationStructureKHR(device.vkHandle, createInfo, null, pAccelerationStructureHandle)
        this@TopLevelAccelerationStructure.vkHandle = pAccelerationStructureHandle[0]

        val scratchBufferLayout = VulkanBufferConfiguration(
                buildSizesInfo.buildScratchSize(),
                MemoryProperty.DEVICE_LOCAL,
                BufferUsage.STORAGE_BUFFER + BufferUsage.SHADER_DEVICE_ADDRESS
        )
        val scratchBuffer = bufferFactory.createBuffer(scratchBufferLayout)

        val buildGeometryInfo2 = calloc(VkAccelerationStructureBuildGeometryInfoKHR::calloc, 1)
        buildGeometryInfo2[0]
                .sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_GEOMETRY_INFO_KHR)
                .flags(VK_BUILD_ACCELERATION_STRUCTURE_PREFER_FAST_TRACE_BIT_KHR)
                .type(VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR)
                .mode(VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR)
                .dstAccelerationStructure(vkHandle)
                .geometryCount(1)
                .pGeometries(accelerationStructureGeometry)
                .scratchData().deviceAddress(scratchBuffer.getDeviceAddress())

        val buildRangeInfo = calloc(VkAccelerationStructureBuildRangeInfoKHR::calloc) {
            primitiveCount(1)
            primitiveOffset(0)
            firstVertex(0)
            transformOffset(0)
        }

        val ppBuildRanges = allocatePointerValues(buildRangeInfo.address())
        vkCmdBuildAccelerationStructuresKHR(commandBuffer.vkHandle, buildGeometryInfo2, ppBuildRanges)

        val accDeviceAddressInfo = calloc(VkAccelerationStructureDeviceAddressInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_DEVICE_ADDRESS_INFO_KHR)
            pNext(0L)
            accelerationStructure(vkHandle)
        }

        this@TopLevelAccelerationStructure.deviceAddress =
                vkGetAccelerationStructureDeviceAddressKHR(device.vkHandle, accDeviceAddressInfo)

        scratchBuffer.destroy()
    }


    fun destroy(device: Device) {
        vkDestroyAccelerationStructureKHR(device.vkHandle, vkHandle, null)
    }
}