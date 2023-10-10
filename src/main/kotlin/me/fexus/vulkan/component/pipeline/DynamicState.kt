package me.fexus.vulkan.component.pipeline

import org.lwjgl.vulkan.EXTExtendedDynamicState.VK_DYNAMIC_STATE_PRIMITIVE_TOPOLOGY_EXT
import org.lwjgl.vulkan.EXTExtendedDynamicState3.VK_DYNAMIC_STATE_POLYGON_MODE_EXT
import org.lwjgl.vulkan.VK12.*


enum class DynamicState(val vkValue: Int) {
    VIEWPORT(VK_DYNAMIC_STATE_VIEWPORT),
    SCISSOR(VK_DYNAMIC_STATE_SCISSOR),
    DEPTH_BOUND(VK_DYNAMIC_STATE_DEPTH_BOUNDS),
    PRIMITVE_TOPOLOGY_EXT(VK_DYNAMIC_STATE_PRIMITIVE_TOPOLOGY_EXT),
    POLYGON_MODE_EXT(VK_DYNAMIC_STATE_POLYGON_MODE_EXT)
}