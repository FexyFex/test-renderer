package me.fexus.vulkan.component

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.layer.VulkanLayer
import me.fexus.vulkan.component.queuefamily.QueueFamily
import me.fexus.vulkan.extension.*
import org.lwjgl.system.Struct
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

            val deviceFeatures = calloc(VkPhysicalDeviceFeatures::calloc) {
                samplerAnisotropy(true)
                sampleRateShading(true)
                multiDrawIndirect(true)
                robustBufferAccess(true)
            }

            val featureChain = mutableListOf<Struct>()

            // Always use dynamic rendering
            val dynamicRenderingFeatures = calloc(VkPhysicalDeviceDynamicRenderingFeatures::calloc) {
                sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES)
                pNext(0)
                dynamicRendering(true)
            }

            if (DescriptorBufferEXTExtension in extensions) {
                val descriptorBufferFeatures = calloc(VkPhysicalDeviceDescriptorBufferFeaturesEXT::calloc) {
                    sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_BUFFER_FEATURES_EXT)
                    pNext(dynamicRenderingFeatures.address())
                    descriptorBuffer(true)
                }

                featureChain.add(descriptorBufferFeatures)
            }

            if (DescriptorIndexingExtension in extensions) {
                val descriptorIndexingFeatures = calloc(VkPhysicalDeviceDescriptorIndexingFeatures::calloc) {
                    sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES)
                    pNext(0)
                    descriptorBindingPartiallyBound(true)
                    descriptorBindingVariableDescriptorCount(true)
                }

                featureChain.add(descriptorIndexingFeatures)
            }

            if (RayTracingPipelineKHRExtension in extensions) {
                val rayTracingProps = calloc(VkPhysicalDeviceRayTracingPipelinePropertiesKHR::calloc) {
                    sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_PIPELINE_PROPERTIES_KHR)
                    pNext(0)
                }

                val props2 = calloc(VkPhysicalDeviceProperties2::calloc) {
                    sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2)
                    pNext(rayTracingProps.address())
                }

                vkGetPhysicalDeviceProperties2(physicalDevice.vkHandle, props2)

                val accelerationStructureFeatures = calloc(VkPhysicalDeviceAccelerationStructureFeaturesKHR::calloc) {
                    sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ACCELERATION_STRUCTURE_FEATURES_KHR)
                    pNext(0L)
                    accelerationStructure(true)
                }

                val rayQueryFeatures = calloc(VkPhysicalDeviceRayQueryFeaturesKHR::calloc) {
                    sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_QUERY_FEATURES_KHR)
                    pNext(0L)
                    rayQuery(true)
                }

                val rayTracingFeatures = calloc(VkPhysicalDeviceRayTracingPipelineFeaturesKHR::calloc) {
                    sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_PIPELINE_FEATURES_KHR)
                    pNext(0L)
                    rayTracingPipeline(true)
                }

                featureChain.add(accelerationStructureFeatures)
                featureChain.add(rayQueryFeatures)
                featureChain.add(rayTracingFeatures)
            }

            if (featureChain.isNotEmpty()) {
                dynamicRenderingFeatures.pNext(featureChain.first().address())
                featureChain.removeAt(0)
                repeat(featureChain.size) {
                    val f = featureChain[it]
                    if (it + 1 < featureChain.size) {
                        val nextFeature = featureChain[it + 1]
                        val pNextFun = f::class.members.first { m -> m.name == "pNext" && m.parameters.size == 2 }
                        pNextFun.call(f, nextFeature.address())
                    }
                }
            }

            val features2 = calloc(VkPhysicalDeviceVulkan12Features::calloc) {
                sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
                pNext(dynamicRenderingFeatures.address())
                bufferDeviceAddress(BufferDeviceAddressKHRExtension in extensions)
                if (DescriptorIndexingExtension in extensions) {
                    descriptorIndexing(true)
                    descriptorBindingPartiallyBound(true)
                    descriptorBindingVariableDescriptorCount(true)
                }
            }

            val deviceCreateInfo = calloc(VkDeviceCreateInfo::calloc) {
                sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                pNext(features2)
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