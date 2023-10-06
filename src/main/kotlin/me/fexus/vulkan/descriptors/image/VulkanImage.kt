package me.fexus.vulkan.descriptors.image

import me.fexus.vulkan.Device
import org.lwjgl.vulkan.VK12.*

class VulkanImage(
    private val device: Device,
    val imageHandle: Long,
    val imageMemoryHandle: Long,
    val imageViewHandle: Long,
    val layout: VulkanImageLayout,
) {

    fun destroy() {
        vkDestroyImageView(device.vkHandle, imageViewHandle, null)
        vkFreeMemory(device.vkHandle, imageMemoryHandle, null)
        vkDestroyImage(device.vkHandle, imageHandle, null)
    }
}