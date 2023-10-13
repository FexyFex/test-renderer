package me.fexus.vulkan.descriptors.image

import org.lwjgl.vulkan.VK12.*


enum class ImageColorFormat(val vkValue: Int) {
    R8G8B8A8_SRGB(VK_FORMAT_R8G8B8A8_SRGB),
    B8G8R8A8_SRGB(VK_FORMAT_B8G8R8A8_SRGB),
    D32_SFLOAT(VK_FORMAT_D32_SFLOAT),
    D32_SFLOAT_S8_UINT(VK_FORMAT_D32_SFLOAT_S8_UINT),
    D24_UNORM_S8_UINT(VK_FORMAT_D24_UNORM_S8_UINT),
}