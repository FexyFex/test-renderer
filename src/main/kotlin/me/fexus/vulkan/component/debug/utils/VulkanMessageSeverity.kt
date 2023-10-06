package me.fexus.vulkan.component.debug.utils

import org.lwjgl.vulkan.EXTDebugUtils.*

enum class VulkanMessageSeverity(val value: Int) {
    VERBOSE(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT),
    INFO(VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT),
    WARNING(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT),
    ERROR(VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);

    companion object {
        fun find(value: Int): VulkanMessageSeverity {
            return values().first { it.value and value != 0 }
        }
    }
}
