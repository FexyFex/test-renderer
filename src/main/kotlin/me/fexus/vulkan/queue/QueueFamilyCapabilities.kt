package me.fexus.vulkan.queue

interface QueueFamilyCapabilities {
    val vkBits: Int

    operator fun plus(other: QueueFamilyCapabilities) = MixedQueueFamilyCapabilities(this.vkBits or other.vkBits)
}