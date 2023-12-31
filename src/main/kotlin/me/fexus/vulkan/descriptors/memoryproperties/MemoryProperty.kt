package me.fexus.vulkan.descriptors.memoryproperties

import org.lwjgl.vulkan.VK12.*

enum class MemoryProperty(override val vkBits: Int): MemoryProperties {
    HOST_VISIBLE(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT),
    HOST_COHERENT(VK_MEMORY_PROPERTY_HOST_COHERENT_BIT),
    DEVICE_LOCAL(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
    HOST_CACHED(VK_MEMORY_PROPERTY_HOST_CACHED_BIT),
    LAZILY_ALLOCATED(VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT),
    NONE(0)
}