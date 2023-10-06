package me.fexus.vulkan.queue.family.capabilities

import org.lwjgl.vulkan.VK10.*

enum class QueueFamilyCapability(override val vkBits: Int): QueueFamilyCapabilities {
    NONE(0),
    GRAPHICS(VK_QUEUE_GRAPHICS_BIT),
    COMPUTE(VK_QUEUE_COMPUTE_BIT),
    TRANSFER(VK_QUEUE_TRANSFER_BIT)
}