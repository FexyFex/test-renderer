package me.fexus.vulkan.component.debug.utils

import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT


class VulkanDebugCallback(private val handler: FullValidationHandler) : VkDebugUtilsMessengerCallbackEXT() {
    override fun invoke(messageSeverity: Int, messageTypes: Int, pCallbackData: Long, pUserData: Long): Int {
        val callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)

        val report = ValidationReport.from(callbackData)

        handler.handle(VulkanMessageSeverity.find(messageSeverity), VulkanMessageType.find(messageTypes), report)

        return 0
    }
}