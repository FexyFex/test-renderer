package me.fexus.vulkan

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.extension.DeviceExtension
import me.fexus.vulkan.layer.VulkanLayer
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkDeviceCreateInfo
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo


class Device {
    lateinit var handle: VkDevice


    fun create(physicalDevice: PhysicalDevice, layers: List<VulkanLayer>, extensions: List<DeviceExtension>): Device {
        this.handle = runMemorySafe {
            val queueCreateInfos = calloc<VkDeviceQueueCreateInfo>(1)
            queueCreateInfos[0]
                .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .pNext(0)
                .flags(0)
                .pQueuePriorities()
                .queueFamilyIndex()

            val deviceCreateInfo = calloc<VkDeviceCreateInfo>() {
                sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                pNext(0)
                flags(0)
                pQueueCreateInfos(queueCreateInfos)
                ppEnabledLayerNames()
                ppEnabledExtensionNames()
            }

            val pDeviceHandle = allocatePointer(1)
            vkCreateDevice(physicalDevice.vkPhysicalDevice, deviceCreateInfo, null, pDeviceHandle)
            return@runMemorySafe VkDevice(pDeviceHandle[0], physicalDevice.vkPhysicalDevice, deviceCreateInfo)
        }
        return this
    }


    fun destroy() {
        vkDestroyDevice(handle, null)
    }
}