package me.fexus.vulkan.component.descriptor.set.layout.bindingflags

import org.lwjgl.vulkan.VK12.*

enum class DescriptorSetLayoutBindingFlag(override val vkBits: Int): DescriptorSetLayoutBindingFlags {
    NONE(0),
    UPDATE_AFTER_BIND(VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT),
    PARTIALLY_BOUND(VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT),
    VARIABLE_DESCRIPTOR_COUNT(VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT)
}