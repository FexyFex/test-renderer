package me.fexus.vulkan.descriptors.image

import org.lwjgl.vulkan.VK12.*


enum class ImageType(val vkValue: Int) {
    TYPE_1D(VK_IMAGE_TYPE_1D),
    TYPE_2D(VK_IMAGE_TYPE_2D),
    TYPE_3D(VK_IMAGE_TYPE_3D),
}