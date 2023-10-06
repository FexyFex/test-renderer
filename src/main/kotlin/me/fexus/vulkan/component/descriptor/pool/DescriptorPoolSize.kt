package me.fexus.vulkan.component.descriptor.pool

import me.fexus.vulkan.descriptors.DescriptorType

data class DescriptorPoolSize(val descriptorType: DescriptorType, val count: Int)