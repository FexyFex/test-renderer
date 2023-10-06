package me.fexus.vulkan.component.queuefamily

import me.fexus.vulkan.component.queuefamily.capabilities.QueueFamilyCapabilities


class QueueFamily(
    val index: Int,
    val capabilities: QueueFamilyCapabilities,
    val supportsPresent: Boolean
)