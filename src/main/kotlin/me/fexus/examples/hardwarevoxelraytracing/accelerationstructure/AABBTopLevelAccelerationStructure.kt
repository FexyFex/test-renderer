package me.fexus.examples.hardwarevoxelraytracing.accelerationstructure

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperty
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRAccelerationStructure.*


class AABBTopLevelAccelerationStructure: IAccelerationStructure {
    override var vkHandle: Long = 0L
    override var deviceAddress: Long = 0L
    override lateinit var buffer: VulkanBuffer


    fun createAndBuild(deviceUtil: VulkanDeviceUtil, config: AABBTLASConfiguration) = runMemorySafe {
        val tlasGeometries = calloc(VkAccelerationStructureGeometryKHR::calloc, config.instancesBuffers.size)
        repeat(config.instancesBuffers.size) {
            val instancesDataBufferAddress = calloc(VkDeviceOrHostAddressConstKHR::calloc) {
                deviceAddress(config.instancesBuffers[it].getDeviceAddress())
            }

            val instanceGeometryData = calloc(VkAccelerationStructureGeometryInstancesDataKHR::calloc) {
                sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_INSTANCES_DATA_KHR)
                pNext(0L)
                arrayOfPointers(false)
                data(instancesDataBufferAddress)
            }

            val tlasGeometry = calloc(VkAccelerationStructureGeometryKHR::calloc) {
                sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_KHR)
                pNext(0L)
                geometryType(VK_GEOMETRY_TYPE_INSTANCES_KHR)
                flags(VK_GEOMETRY_OPAQUE_BIT_KHR)
                geometry().instances(instanceGeometryData)
            }

            tlasGeometries.put(it, tlasGeometry)
        }

        val buildGeometryInfo = calloc(VkAccelerationStructureBuildGeometryInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_GEOMETRY_INFO_KHR)
            pNext(0L)
            flags(VK_BUILD_ACCELERATION_STRUCTURE_PREFER_FAST_TRACE_BIT_KHR)
            type(VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR)
            mode(VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR)
            geometryCount(config.instancesBuffers.size)
            pGeometries(tlasGeometries)
            srcAccelerationStructure(0L)
            dstAccelerationStructure(0L)
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

        val tlasBufferConfig = VulkanBufferConfiguration(
            buildSizesInfo.accelerationStructureSize(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.ACCELERATION_STRUCTURE_STORAGE_KHR + BufferUsage.SHADER_DEVICE_ADDRESS
        )
        buffer = deviceUtil.createBuffer(tlasBufferConfig)

        val tlasCreateInfo = calloc(VkAccelerationStructureCreateInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_CREATE_INFO_KHR)
            pNext(0L)
            buffer(buffer.vkBufferHandle)
            size(buildSizesInfo.accelerationStructureSize())
            type(VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR)
        }

        val pTLASHandle = allocateLong(1)
        vkCreateAccelerationStructureKHR(deviceUtil.vkDeviceHandle, tlasCreateInfo, null, pTLASHandle)
        vkHandle = pTLASHandle[0]

        deviceUtil.runSingleTimeCommands { cmdBuf ->
            val buildRangeInfo = calloc(VkAccelerationStructureBuildRangeInfoKHR::calloc) {
                primitiveCount(1)
                primitiveOffset(0)
                firstVertex(0)
                transformOffset(0)
            }

            val ppBuildRanges = allocatePointerValues(buildRangeInfo.address())
            val pBuildGeometryInfos = calloc(VkAccelerationStructureBuildGeometryInfoKHR::calloc, 1)
            pBuildGeometryInfos.put(0, buildGeometryInfo)
            vkCmdBuildAccelerationStructuresKHR(cmdBuf.vkHandle, pBuildGeometryInfos, ppBuildRanges)
        }

        val tlasDeviceAddressInfo = calloc(VkAccelerationStructureDeviceAddressInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_DEVICE_ADDRESS_INFO_KHR)
            pNext(0L)
            accelerationStructure(vkHandle)
        }
        deviceAddress = vkGetAccelerationStructureDeviceAddressKHR(deviceUtil.vkDeviceHandle, tlasDeviceAddressInfo)
    }
}