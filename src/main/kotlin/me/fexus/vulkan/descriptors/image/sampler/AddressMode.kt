package me.fexus.vulkan.descriptors.image.sampler

import org.lwjgl.vulkan.VK12

enum class AddressMode(val vkValue: Int) {
    CLAMP_TO_EDGE(VK12.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE),
    REPEAT(VK12.VK_SAMPLER_ADDRESS_MODE_REPEAT)
}