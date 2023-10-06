package me.fexus.vulkan.pipeline

import org.lwjgl.vulkan.VK12.*


enum class CullMode(internal val vkValue: Int) {
    BACKFACE(VK_CULL_MODE_BACK_BIT),
    FRONTFACE(VK_CULL_MODE_FRONT_BIT),
    NONE(VK_CULL_MODE_NONE),
    ALL(VK_CULL_MODE_FRONT_AND_BACK)
}