package me.fexus.vulkan

import me.fexus.vulkan.component.Device
import me.fexus.vulkan.component.Instance
import me.fexus.vulkan.component.PhysicalDevice
import me.fexus.vulkan.component.debug.DebugUtilsMessenger
import me.fexus.vulkan.extension.*
import me.fexus.vulkan.layer.ValidationLayer
import me.fexus.vulkan.layer.VulkanLayer
import me.fexus.vulkan.component.queuefamily.QueueFamily


class VulkanCore {
    private var debug: Boolean = false
    private val enabledLayers: List<VulkanLayer> = listOf<VulkanLayer>(ValidationLayer)
    val enabledExtensions = mutableListOf<DeviceExtension>(
        SwapchainKHR,
        DynamicRenderingKHR,
        DepthStencilResolveKHR,
        Synchronization2KHR,
        DescriptorIndexingEXT,
        MemoryBudgetEXT
    )

    val instance = Instance()
    private val debugMessenger = DebugUtilsMessenger()
    val physicalDevice = PhysicalDevice()
    val device = Device()


    fun createInstance(withDebug: Boolean = false) {
        instance.create(enabledLayers, withDebug)
        this.debug = withDebug
        if (withDebug) debugMessenger.create(instance)
    }

    fun createPhysicalDevice() {
        physicalDevice.create(instance)
    }

    fun createDevice(uniqueQueueFamilies: List<QueueFamily>) {
        device.create(physicalDevice, enabledLayers, enabledExtensions, uniqueQueueFamilies)
    }


    fun destroy() {
        device.destroy()
        if (this.debug) debugMessenger.destroy(instance)
        instance.destroy()
    }
}