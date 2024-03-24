package me.fexus.vulkan.descriptors.image.sampler

import me.fexus.vulkan.component.Device
import org.lwjgl.vulkan.VK12.*


class VulkanSampler(private val device: Device, val vkHandle: Long, val layout: VulkanSamplerConfiguration) {
    var index: Int = -1 // An index the user can set (useful for bindless approaches)

    fun destroy(device: Device = this.device) {
        vkDestroySampler(device.vkHandle, vkHandle, null)
    }
}