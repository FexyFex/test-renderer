package me.fexus.vulkan.descriptors.image

import org.lwjgl.vulkan.VK12.*

enum class ImageViewType(val vkValue: Int) {
    TYPE_1D(VK_IMAGE_VIEW_TYPE_1D),
    TYPE_1D_ARRAY(VK_IMAGE_VIEW_TYPE_1D_ARRAY),
    TYPE_2D(VK_IMAGE_VIEW_TYPE_2D),
    TYPE_2D_ARRAY(VK_IMAGE_VIEW_TYPE_2D_ARRAY),
    TYPE_3D(VK_IMAGE_VIEW_TYPE_3D)
}