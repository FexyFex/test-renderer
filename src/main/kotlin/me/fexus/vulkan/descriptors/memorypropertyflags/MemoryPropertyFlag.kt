package me.fexus.vulkan.descriptors.memorypropertyflags

import org.lwjgl.vulkan.VK12.*

enum class MemoryPropertyFlag(override val vkBits: Int): MemoryPropertyFlags {
    DEVICE_LOCAL(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
    HOST_VISIBLE(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT),
    HOST_COHERENT(VK_MEMORY_PROPERTY_HOST_COHERENT_BIT),
    HOST_CACHED(VK_MEMORY_PROPERTY_HOST_CACHED_BIT),
    LAZILY_ALLOCATED(VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT),
    NONE(0)
}