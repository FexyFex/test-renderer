package me.fexus.vulkan.descriptors.image.usage

import org.lwjgl.vulkan.VK12


enum class ImageUsage(override val vkBits: Int): IImageUsage {
    SAMPLED(VK12.VK_IMAGE_USAGE_SAMPLED_BIT),
    TRANSFER_SRC(VK12.VK_IMAGE_USAGE_TRANSFER_SRC_BIT),
    TRANSFER_DST(VK12.VK_IMAGE_USAGE_TRANSFER_DST_BIT),
    COLOR_ATTACHMENT(VK12.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT),
    DEPTH_STENCIL_ATTACHMENT(VK12.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT),
    NONE(0)
}