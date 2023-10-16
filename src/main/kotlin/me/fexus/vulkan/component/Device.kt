package me.fexus.vulkan.component

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.extension.DeviceExtension
import me.fexus.vulkan.layer.VulkanLayer
import me.fexus.vulkan.component.queuefamily.QueueFamily
import org.lwjgl.vulkan.EXTDescriptorBuffer.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_BUFFER_FEATURES_EXT
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES
import org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkDeviceCreateInfo
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo
import org.lwjgl.vulkan.VkPhysicalDeviceDescriptorBufferFeaturesEXT
import org.lwjgl.vulkan.VkPhysicalDeviceDescriptorIndexingFeatures
import org.lwjgl.vulkan.VkPhysicalDeviceDynamicRenderingFeatures
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures


class Device {
    lateinit var vkHandle: VkDevice


    fun create(
        physicalDevice: PhysicalDevice,
        layers: List<VulkanLayer>,
        extensions: List<DeviceExtension>,
        queueFamilies: List<QueueFamily>
    ): Device {
        this.vkHandle = runMemorySafe {
            val queueCreateInfos = calloc(VkDeviceQueueCreateInfo::calloc, queueFamilies.size)
            queueFamilies.forEachIndexed { index, queueFamily ->
                val pQueuePriority = allocateFloat(1)
                pQueuePriority.put(0, 1f)
                queueCreateInfos[index]
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .pNext(0)
                    .flags(0)
                    .pQueuePriorities(pQueuePriority)
                    .queueFamilyIndex(queueFamily.index)
            }

            val ppEnabledLayerNames = allocatePointer(layers.size)
            layers.forEachIndexed { index, vulkanLayer ->
                ppEnabledLayerNames.put(index, allocateString(vulkanLayer.name))
            }

            val ppEnabledExtensions = allocatePointer(extensions.size)
            extensions.forEachIndexed { index, deviceExtension ->
                ppEnabledExtensions.put(index, allocateString(deviceExtension.name))
            }

            val deviceFeatures = calloc(VkPhysicalDeviceFeatures::calloc) {
                samplerAnisotropy(true)
                sampleRateShading(true)
                multiDrawIndirect(true)
            }

            val dynamicRenderingFeatures = calloc(VkPhysicalDeviceDynamicRenderingFeatures::calloc) {
                sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES)
                dynamicRendering(true)
            }

            val descriptorBufferFeatures = calloc(VkPhysicalDeviceDescriptorBufferFeaturesEXT::calloc) {
                sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_BUFFER_FEATURES_EXT)
                pNext(dynamicRenderingFeatures.address())
                descriptorBuffer(true)
            }

            val descriptorIndexingFeatures = calloc(VkPhysicalDeviceDescriptorIndexingFeatures::calloc) {
                sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES)
                pNext(dynamicRenderingFeatures.address())
                descriptorBindingPartiallyBound(true)
                descriptorBindingVariableDescriptorCount(true)
            }

            val deviceCreateInfo = calloc(VkDeviceCreateInfo::calloc) {
                sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                pNext(descriptorIndexingFeatures.address())
                flags(0)
                pQueueCreateInfos(queueCreateInfos)
                ppEnabledLayerNames(ppEnabledLayerNames)
                ppEnabledExtensionNames(ppEnabledExtensions)
                pEnabledFeatures(deviceFeatures)
            }

            val pDeviceHandle = allocatePointer(1)
            vkCreateDevice(physicalDevice.vkHandle, deviceCreateInfo, null, pDeviceHandle)
            return@runMemorySafe VkDevice(pDeviceHandle[0], physicalDevice.vkHandle, deviceCreateInfo)
        }
        return this
    }


    fun waitIdle() = vkDeviceWaitIdle(vkHandle)


    fun destroy() {
        vkDestroyDevice(vkHandle, null)
    }
}