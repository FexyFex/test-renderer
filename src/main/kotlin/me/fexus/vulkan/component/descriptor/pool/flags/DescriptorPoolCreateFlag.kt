package me.fexus.vulkan.component.descriptor.pool.flags

import org.lwjgl.vulkan.VK12.*

enum class DescriptorPoolCreateFlag(override val vkBits: Int): DescriptorPoolCreateFlags {
    FREE_DESCRIPTOR_SET(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT),
    UPDATE_AFTER_BIND(VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT),
    ALLOW_OVERALLOCATION_SETS_NV(0x00000008),
    ALLOW_OVERALLOCATION_POOLS_NV(0x00000010)
}