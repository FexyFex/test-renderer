package me.fexus.vulkan.component.descriptor.set.layout

import me.fexus.vulkan.component.descriptor.set.layout.createflags.DescriptorSetLayoutCreateFlags

data class DescriptorSetLayoutPlan(
    val layoutFlags: DescriptorSetLayoutCreateFlags,
    val bindings: List<DescriptorSetLayoutBinding>,
)