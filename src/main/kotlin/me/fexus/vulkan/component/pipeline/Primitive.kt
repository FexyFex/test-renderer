package me.fexus.vulkan.component.pipeline

import org.lwjgl.vulkan.VK12


enum class Primitive(val vkValue: Int) {
    POINTS(VK12.VK_PRIMITIVE_TOPOLOGY_POINT_LIST),
    LINES(VK12.VK_PRIMITIVE_TOPOLOGY_LINE_LIST),
    TRIANGLES(VK12.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
}