package me.fexus.vulkan.component

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.layer.VulkanLayer
import me.fexus.vulkan.component.queuefamily.QueueFamily
import me.fexus.vulkan.extension.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDescriptorBuffer.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_BUFFER_FEATURES_EXT
import org.lwjgl.vulkan.KHRAccelerationStructure.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ACCELERATION_STRUCTURE_FEATURES_KHR
import org.lwjgl.vulkan.KHRRayQuery.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_QUERY_FEATURES_KHR
import org.lwjgl.vulkan.KHRRayTracingPipeline.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_PIPELINE_FEATURES_KHR
import org.lwjgl.vulkan.KHRRayTracingPipeline.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_PIPELINE_PROPERTIES_KHR
import org.lwjgl.vulkan.VK13.*


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
                ppEnabledLayerNames.put(index, allocateStringValue(vulkanLayer.name))
            }

            val ppEnabledExtensions = allocatePointer(extensions.size)
            extensions.forEachIndexed { index, deviceExtension ->
                ppEnabledExtensions.put(index, allocateStringValue(deviceExtension.name))
            }

            val baseDeviceFeatures = calloc(VkPhysicalDeviceFeatures::calloc) {
                samplerAnisotropy(true)
                sampleRateShading(true)
                multiDrawIndirect(true)
                robustBufferAccess(true)
            }

            // Always use dynamic rendering and descriptor indexing (bye bye, mobile)
            val dynamicRenderingFeatures = calloc(VkPhysicalDeviceDynamicRenderingFeatures::calloc) {
                sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES)
                pNext(0L)
                dynamicRendering(true)
            }

            if (RayTracingPipelineKHRExtension in extensions) {
                val accelerationStructureFeatures = calloc(VkPhysicalDeviceAccelerationStructureFeaturesKHR::calloc) {
                    sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ACCELERATION_STRUCTURE_FEATURES_KHR)
                    pNext(0L)
                    accelerationStructure(true)
                }

                val rayTracingFeatures = calloc(VkPhysicalDeviceRayTracingPipelineFeaturesKHR::calloc) {
                    sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_PIPELINE_FEATURES_KHR)
                    pNext(accelerationStructureFeatures.address())
                    rayTracingPipeline(true)
                }

                dynamicRenderingFeatures.pNext(rayTracingFeatures.address())
            }

            val features2 = calloc(VkPhysicalDeviceVulkan12Features::calloc) {
                sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
                pNext(dynamicRenderingFeatures.address())
                bufferDeviceAddress(BufferDeviceAddressKHRExtension in extensions)
                if (DescriptorIndexingExtension in extensions) {
                    descriptorIndexing(true)
                    descriptorBindingPartiallyBound(true)
                    descriptorBindingVariableDescriptorCount(true)
                    shaderStorageBufferArrayNonUniformIndexing(true)
                    shaderSampledImageArrayNonUniformIndexing(true)
                }
            }

            val deviceCreateInfo = calloc(VkDeviceCreateInfo::calloc) {
                sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                pNext(features2.address())
                flags(0)
                pQueueCreateInfos(queueCreateInfos)
                ppEnabledLayerNames(ppEnabledLayerNames)
                ppEnabledExtensionNames(ppEnabledExtensions)
                pEnabledFeatures(baseDeviceFeatures)
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