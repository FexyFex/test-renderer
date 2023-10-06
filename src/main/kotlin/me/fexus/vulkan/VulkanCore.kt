package me.fexus.vulkan

import me.fexus.vulkan.debug.DebugUtilsMessenger
import me.fexus.vulkan.extension.*
import me.fexus.vulkan.layer.ValidationLayer
import me.fexus.vulkan.layer.VulkanLayer
import me.fexus.vulkan.queue.family.QueueFamily


class VulkanCore {
    private val enabledLayers = listOf<VulkanLayer>(ValidationLayer)
    private val enabledExtensions = listOf(
        SwapchainExtension,
        DynamicRenderingExtension,
        DepthStencilResolveExtension,
        Synchronization2Extension,
        //DescriptorBufferExtension,
        DescriptorIndexingExtension
    )

    val instance = Instance()
    private val debugMessenger = DebugUtilsMessenger()
    val physicalDevice = PhysicalDevice()
    val device = Device()


    fun createInstance() {
        instance.create(enabledLayers)
        debugMessenger.create(instance)
    }

    fun createPhysicalDevice() {
        physicalDevice.create(instance)
    }

    fun createDevice(uniqueQueueFamilies: List<QueueFamily>) {
        device.create(physicalDevice, enabledLayers, enabledExtensions, uniqueQueueFamilies)
    }


    fun destroy() {
        device.destroy()
        debugMessenger.destroy(instance)
        instance.destroy()
    }
}