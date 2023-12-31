package me.fexus.vulkan.descriptors.buffer

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.component.PhysicalDevice
import me.fexus.vulkan.descriptors.DescriptorFactory
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.exception.catchVK
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRBufferDeviceAddress.VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT_KHR
import org.lwjgl.vulkan.VK12.*


class VulkanBufferFactory : DescriptorFactory {
    override lateinit var physicalDevice: PhysicalDevice
    override lateinit var device: Device

    fun init(physicalDevice: PhysicalDevice, device: Device) {
        this.physicalDevice = physicalDevice
        this.device = device
    }


    /**
     * Attempts to create a VulkanBuffer according to the given preferred VulkanBufferLayout.
     * If it is impossible to create the VulkanBuffer with the given VulkanBufferLayout, the function
     * returns a VulkanBuffer with an altered VulkanBufferLayout to indicate the changes
     * that were made during creation.
     */
    fun createBuffer(preferredBufferConfig: VulkanBufferConfiguration): VulkanBuffer = runMemorySafe {
        // TODO: Check if preferences are valid and can be fulfilled

        val bufferInfo = calloc(VkBufferCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            pNext(0)
            size(preferredBufferConfig.size)
            usage(preferredBufferConfig.usage.vkBits)
            sharingMode(preferredBufferConfig.sharingMode)
        }

        val pBufferHandle = allocateLong(1)
        vkCreateBuffer(device.vkHandle, bufferInfo, null, pBufferHandle).catchVK()
        val bufferHandle = pBufferHandle[0]

        val memRequirements = calloc(VkMemoryRequirements::calloc)
        vkGetBufferMemoryRequirements(device.vkHandle, bufferHandle, memRequirements)

        val memoryTypeIndex = findMemoryTypeIndex(
            memRequirements.memoryTypeBits(),
            preferredBufferConfig.memoryProperties
        )

        val allocInfo = calloc(VkMemoryAllocateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
            pNext(0)
            allocationSize(memRequirements.size())
            memoryTypeIndex(memoryTypeIndex)
        }

        if (BufferUsage.SHADER_DEVICE_ADDRESS in preferredBufferConfig.usage) {
            val memoryAllocateFlagsInfo = calloc(VkMemoryAllocateFlagsInfo::calloc) {
                sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_FLAGS_INFO)
                pNext(0L)
                flags(VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT_KHR)
            }

            allocInfo.pNext(memoryAllocateFlagsInfo.address())
        }

        val pBufferMemoryHandle = allocateLong(1)
        vkAllocateMemory(device.vkHandle, allocInfo, null, pBufferMemoryHandle).catchVK()
        val bufferMemoryHandle = pBufferMemoryHandle[0]
        vkBindBufferMemory(device.vkHandle, bufferHandle, bufferMemoryHandle, 0).catchVK()

        val actualSize: Long = memRequirements.size()
        val actualProperties = preferredBufferConfig.memoryProperties
        val actualUsage = preferredBufferConfig.usage
        val actualBufferLayout = VulkanBufferConfiguration(actualSize, actualProperties, actualUsage)

        return@runMemorySafe VulkanBuffer(device, bufferHandle, bufferMemoryHandle, actualBufferLayout)
    }
}