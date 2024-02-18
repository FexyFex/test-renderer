package me.fexus.vulkan.descriptors.image.feature

import org.lwjgl.vulkan.VK13


enum class FormatFeatureFlag(override val vkBits: Long): FormatFeatureFlags {
    SAMPLED_IMAGE(VK13.VK_FORMAT_FEATURE_2_SAMPLED_IMAGE_BIT),
    STORAGE_IMAGE(VK13.VK_FORMAT_FEATURE_2_STORAGE_IMAGE_BIT)
}