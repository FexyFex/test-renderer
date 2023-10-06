package me.fexus.vulkan.component.descriptor.set.layout.createflags

import org.lwjgl.vulkan.VK12.*

enum class DescriptorSetLayoutCreateFlag(override val vkBits: Int): DescriptorSetLayoutCreateFlags {
    UPDATE_AFTER_BIND(VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT)
}