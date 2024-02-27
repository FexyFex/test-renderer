package me.fexus.vulkan.descriptors.image

import me.fexus.vulkan.component.Device
import org.lwjgl.vulkan.VK12.*

class VulkanImage(
    private val device: Device,
    val vkImageHandle: Long,
    val vkImageMemoryHandle: Long,
    val vkImageViewHandle: Long,
    val config: VulkanImageConfiguration,
) {
    var index: Int = -1 // An index the user can set (useful for bindless approaches)

    fun destroy() {
        vkDestroyImageView(device.vkHandle, vkImageViewHandle, null)
        vkFreeMemory(device.vkHandle, vkImageMemoryHandle, null)
        vkDestroyImage(device.vkHandle, vkImageHandle, null)
    }
}