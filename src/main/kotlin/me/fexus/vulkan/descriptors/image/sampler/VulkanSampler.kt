package me.fexus.vulkan.descriptors.image.sampler

import me.fexus.vulkan.component.Device
import org.lwjgl.vulkan.VK12.*


class VulkanSampler(private val device: Device, val vkHandle: Long, val layout: VulkanSamplerConfiguration) {
    fun destroy(device: Device = this.device) {
        vkDestroySampler(device.vkHandle, vkHandle, null)
    }
}