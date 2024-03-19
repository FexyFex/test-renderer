package me.fexus.vulkan.component.pipeline

import org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_INSTANCE
import org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX


enum class VertexInputRate(val vkEnum: Int) {
    VERTEX(VK_VERTEX_INPUT_RATE_VERTEX),
    INSTANCE(VK_VERTEX_INPUT_RATE_INSTANCE)
}