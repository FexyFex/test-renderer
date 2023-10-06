package me.fexus.vulkan.descriptors.image

import org.lwjgl.vulkan.VK12.*

enum class ImageTiling(val vkValue: Int) {
    OPTIMAL(VK_IMAGE_TILING_OPTIMAL),
    LINEAR(VK_IMAGE_TILING_LINEAR)
}