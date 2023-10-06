package me.fexus.vulkan.descriptors.image.aspect

import org.lwjgl.vulkan.VK12.*

enum class ImageAspect(override val vkBits: Int): IImageAspect {
    COLOR(VK_IMAGE_ASPECT_COLOR_BIT),
    DEPTH(VK_IMAGE_ASPECT_DEPTH_BIT),
    STENCIL(VK_IMAGE_ASPECT_STENCIL_BIT)
}