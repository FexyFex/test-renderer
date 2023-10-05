package me.fexus.vulkan.queue


class QueueFamily(
    val queueFamilyIndex: Int,
    val capabilities: QueueFamilyCapabilities,
    val supportsPresent: Boolean
)