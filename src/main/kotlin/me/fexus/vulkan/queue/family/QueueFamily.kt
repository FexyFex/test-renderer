package me.fexus.vulkan.queue.family

import me.fexus.vulkan.queue.family.capabilities.QueueFamilyCapabilities


class QueueFamily(
    val index: Int,
    val capabilities: QueueFamilyCapabilities,
    val supportsPresent: Boolean
)