package me.fexus.vulkan.component.queuefamily.capabilities

interface QueueFamilyCapabilities {
    val vkBits: Int

    operator fun plus(other: QueueFamilyCapabilities) = CombinedQueueFamilyCapabilities(this.vkBits or other.vkBits)

    operator fun contains(element: QueueFamilyCapabilities) = this.vkBits and element.vkBits == element.vkBits
}