package me.fexus.vulkan.raytracing.accelerationstructure

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.model.CubeModel
import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferFactory
import me.fexus.vulkan.descriptors.buffer.VulkanBufferLayout
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperty
import me.fexus.vulkan.exception.catchVK
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRAccelerationStructure.*
import org.lwjgl.vulkan.VK12.*


class BottomLevelAccelerationStructure {
    var vkHandle: Long = 0L; private set
    var deviceAddress: Long = 0L; private set
    lateinit var buffer: VulkanBuffer; private set


    fun createAndBuild(
            device: Device,
            bufferFactory: VulkanBufferFactory,
            commandBuffer: CommandBuffer,
            config: BottomLevelAccelerationStructureConfiguration
    ) = runMemorySafe {
        val pTriangleCount = allocateIntValues(config.primitivesCount)

        val geometryTrianglesData = calloc(VkAccelerationStructureGeometryTrianglesDataKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_TRIANGLES_DATA_KHR)
            pNext(0)
            vertexFormat(VK_FORMAT_R32G32B32A32_SFLOAT)
            vertexData().deviceAddress(config.vertexBuffer.getDeviceAddress()).hostAddress(0L)
            vertexStride(CubeModel.Vertex.SIZE_BYTES.toLong())
            maxVertex(CubeModel.vertices.size - 1)
            indexType(VK_INDEX_TYPE_UINT32)
            indexData().deviceAddress(config.indexBuffer?.getDeviceAddress() ?: 0L).hostAddress(0L)
            transformData().deviceAddress(0L).hostAddress(0L)
        }

        val accStructureGeometry = calloc(VkAccelerationStructureGeometryKHR::calloc, 1)
        accStructureGeometry[0]
            .sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_KHR)
            .pNext(0L)
            .flags(VK_GEOMETRY_OPAQUE_BIT_KHR)
            .geometryType(VK_GEOMETRY_TYPE_TRIANGLES_KHR)
            .geometry().triangles(geometryTrianglesData)

        val buildGeomInfo = calloc(VkAccelerationStructureBuildGeometryInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_GEOMETRY_INFO_KHR)
            pNext(0L)
            flags(VK_BUILD_ACCELERATION_STRUCTURE_PREFER_FAST_TRACE_BIT_KHR)
            type(VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR)
            mode(VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR)
            srcAccelerationStructure(0L)
            dstAccelerationStructure(0L)
            geometryCount(accStructureGeometry.capacity())
            pGeometries(accStructureGeometry)
            ppGeometries(null)
            scratchData().deviceAddress(0L).hostAddress(0L)
        }

        val buildSizesInfo = calloc(VkAccelerationStructureBuildSizesInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_SIZES_INFO_KHR)
        }
        vkGetAccelerationStructureBuildSizesKHR(
            device.vkHandle,
            VK_ACCELERATION_STRUCTURE_BUILD_TYPE_DEVICE_KHR,
            buildGeomInfo,
            pTriangleCount,
            buildSizesInfo
        )

        val asBufferLayout = VulkanBufferLayout(
            buildSizesInfo.accelerationStructureSize(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.ACCELERATION_STRUCTURE_STORAGE_KHR + BufferUsage.SHADER_DEVICE_ADDRESS
        )
        this@BottomLevelAccelerationStructure.buffer = bufferFactory.createBuffer(asBufferLayout)

        val accCreateInfo = calloc(VkAccelerationStructureCreateInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_CREATE_INFO_KHR)
            pNext(0)
            createFlags(0)
            buffer(this@BottomLevelAccelerationStructure.buffer.vkBufferHandle)
            offset(0)
            size(buildSizesInfo.accelerationStructureSize())
            type(VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR)
            deviceAddress(0L)
        }

        val pAccStructureHandle = allocateLong(1)
        vkCreateAccelerationStructureKHR(device.vkHandle, accCreateInfo, null, pAccStructureHandle).catchVK()
        this@BottomLevelAccelerationStructure.vkHandle = pAccStructureHandle[0]

        val scratchBufferLayout = VulkanBufferLayout(
            buildSizesInfo.buildScratchSize(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.STORAGE_BUFFER + BufferUsage.SHADER_DEVICE_ADDRESS
        )
        val scratchBuffer = bufferFactory.createBuffer(scratchBufferLayout)

        val accBuildGeometryInfo = calloc(VkAccelerationStructureBuildGeometryInfoKHR::calloc, 1)
        accBuildGeometryInfo[0]
                .sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_GEOMETRY_INFO_KHR)
                .pNext(0L)
                .flags(VK_BUILD_ACCELERATION_STRUCTURE_PREFER_FAST_TRACE_BIT_KHR)
                .type(VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR)
                .mode(VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR)
                .dstAccelerationStructure(this@BottomLevelAccelerationStructure.vkHandle)
                .pGeometries(accStructureGeometry)
                .scratchData().deviceAddress(scratchBuffer.getDeviceAddress())


        val buildRangeInfo = calloc(VkAccelerationStructureBuildRangeInfoKHR::calloc) {
            primitiveCount(config.primitivesCount)
            primitiveOffset(0)
            firstVertex(0)
            transformOffset(0)
        }

        val ppBuildRangeInfos = allocatePointerValues(buildRangeInfo.address())
        vkCmdBuildAccelerationStructuresKHR(
                commandBuffer.vkHandle,
                accBuildGeometryInfo,
                ppBuildRangeInfos
        )

        val deviceAddressInfo = calloc(VkAccelerationStructureDeviceAddressInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_DEVICE_ADDRESS_INFO_KHR)
            accelerationStructure(this@BottomLevelAccelerationStructure.vkHandle)
        }
        this@BottomLevelAccelerationStructure.deviceAddress =
                vkGetAccelerationStructureDeviceAddressKHR(device.vkHandle, deviceAddressInfo)

        scratchBuffer.destroy()
    }
}