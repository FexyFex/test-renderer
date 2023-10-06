package me.fexus.vulkan.component.debug.utils

import org.lwjgl.vulkan.EXTDebugUtils.*

enum class VulkanMessageType(val value: Int) {
    GENERAL(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT),
    VALIDATION(VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT),
    PERFORMANCE(VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);

    companion object {
        fun find(value: Int): Array<VulkanMessageType> {
            return values().filter { it.value and value != 0 }.toTypedArray()
        }
    }
}
