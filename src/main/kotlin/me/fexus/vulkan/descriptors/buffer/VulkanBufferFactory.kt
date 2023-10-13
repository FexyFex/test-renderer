package me.fexus.vulkan.descriptors.buffer

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.component.PhysicalDevice
import me.fexus.vulkan.descriptors.DescriptorFactory
import me.fexus.vulkan.exception.catchVK
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*


class VulkanBufferFactory: DescriptorFactory {
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
    fun createBuffer(preferredBufferLayout: VulkanBufferLayout): VulkanBuffer {
        // TODO: Check if layout preferences are valid and can be fulfilled

        val buffer = runMemorySafe {
            val bufferInfo = calloc(VkBufferCreateInfo::calloc) {
                sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                pNext(0)
                size(preferredBufferLayout.size)
                usage(preferredBufferLayout.usage.vkBits)
                sharingMode(preferredBufferLayout.sharingMode)
            }

            val pBufferHandle = allocateLong(1)
            vkCreateBuffer(device.vkHandle, bufferInfo, null, pBufferHandle).catchVK()
            val bufferHandle = pBufferHandle[0]

            val memRequirements = calloc(VkMemoryRequirements::calloc)
            vkGetBufferMemoryRequirements(device.vkHandle, bufferHandle, memRequirements)

            val memoryTypeIndex = findMemoryTypeIndex(
                memRequirements.memoryTypeBits(),
                preferredBufferLayout.memoryProperties
            )

            val allocInfo = calloc(VkMemoryAllocateInfo::calloc) {
                sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                pNext(0)
                allocationSize(memRequirements.size())
                memoryTypeIndex(memoryTypeIndex)
            }

            val pBufferMemoryHandle = allocateLong(1)
            vkAllocateMemory(device.vkHandle, allocInfo, null, pBufferMemoryHandle).catchVK()
            val bufferMemoryHandle = pBufferMemoryHandle[0]
            vkBindBufferMemory(device.vkHandle, bufferHandle, bufferMemoryHandle, 0).catchVK()

            val actualSize: Long = memRequirements.size()
            val actualProperties = preferredBufferLayout.memoryProperties
            val actualUsage = preferredBufferLayout.usage
            val actualBufferLayout = VulkanBufferLayout(actualSize, actualProperties, actualUsage)

            return@runMemorySafe VulkanBuffer(device, bufferHandle, bufferMemoryHandle, actualBufferLayout)
        }

        return buffer
    }
}