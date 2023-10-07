package me.fexus.vulkan.descriptors.image.sampler

import org.lwjgl.vulkan.VK12.VK_FILTER_LINEAR
import org.lwjgl.vulkan.VK12.VK_FILTER_NEAREST


enum class Filtering(val vkValue: Int) {
    NEAREST(VK_FILTER_NEAREST),
    LINEAR(VK_FILTER_LINEAR)
}