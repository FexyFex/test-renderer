package me.fexus.vulkan.component.debug

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.component.Instance
import me.fexus.vulkan.component.debug.utils.FullValidationHandler
import me.fexus.vulkan.component.debug.utils.VulkanDebugCallback
import org.lwjgl.vulkan.EXTDebugUtils
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT


class DebugUtilsMessenger {
    var vkHandle: Long = 0

    fun create(instance: Instance) {
        this.vkHandle = runMemorySafe {
            val debugCreateInfo = getCreateInfo()

            val pDebugHandle = allocateLong(1)
            vkCreateDebugUtilsMessengerEXT(instance.vkHandle, debugCreateInfo, null, pDebugHandle)
            debugCreateInfo.free()
            return@runMemorySafe pDebugHandle[0]
        }
    }

    fun destroy(instance: Instance) {
        vkDestroyDebugUtilsMessengerEXT(instance.vkHandle, vkHandle, null)
    }


    companion object {
        fun getCreateInfo(): VkDebugUtilsMessengerCreateInfoEXT {
            return VkDebugUtilsMessengerCreateInfoEXT.calloc()
                .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(
                    EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT or
                            EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
                )
                .messageType(
                    EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or
                            EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT or
                            EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                )
                    .pfnUserCallback(VulkanDebugCallback(FullValidationHandler()))
                    .pUserData(0L)
        }
    }
}