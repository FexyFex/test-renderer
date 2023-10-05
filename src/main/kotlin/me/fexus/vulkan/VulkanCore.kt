package me.fexus.vulkan

import me.fexus.vulkan.extension.DepthStencilResolveExtension
import me.fexus.vulkan.extension.DynamicRenderingExtension
import me.fexus.vulkan.extension.SwapchainExtension
import me.fexus.vulkan.layer.ValidationLayer
import me.fexus.vulkan.layer.VulkanLayer
import me.fexus.window.Window


class VulkanCore(private val window: Window) {
    private val enabledLayers = listOf<VulkanLayer>(ValidationLayer)
    private val enabledExtensions = listOf(
        SwapchainExtension,
        DynamicRenderingExtension,
        DepthStencilResolveExtension,
    )

    val instance = Instance()
    val physicalDevice = PhysicalDevice()
    val device = Device()


    fun createInstance() {
        instance.create(enabledLayers)
    }

    fun init() {
        physicalDevice.create(instance)
        device.create(physicalDevice, enabledLayers, enabledExtensions)
    }


    fun destroy() {

    }
}