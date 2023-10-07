package me.fexus.vulkan.component.pipeline

import org.lwjgl.vulkan.VK12.*

enum class PolygonMode(internal val vkValue: Int) {
    FILL(VK_POLYGON_MODE_FILL),
    WIREFRAME(VK_POLYGON_MODE_LINE),
    POINT(VK_POLYGON_MODE_POINT)
}