package me.fexus.vulkan.descriptors.image.sampler

import me.fexus.vulkan.component.Device
import org.lwjgl.vulkan.VK12.*


class VulkanSampler(val vkHandle: Long, val layout: VulkanSamplerConfiguration) {
    fun destroy(device: Device) {
        vkDestroySampler(device.vkHandle, vkHandle, null)
    }
}