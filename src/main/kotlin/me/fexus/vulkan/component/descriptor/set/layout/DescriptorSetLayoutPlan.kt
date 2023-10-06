package me.fexus.vulkan.component.descriptor.set.layout

import me.fexus.vulkan.component.descriptor.set.layout.bindingflags.DescriptorSetLayoutBindingFlags
import me.fexus.vulkan.component.descriptor.set.layout.createflags.DescriptorSetLayoutCreateFlags

data class DescriptorSetLayoutPlan(
    val bindings: List<DescriptorSetLayoutBinding>,
    val bindingFlags: DescriptorSetLayoutBindingFlags,
    val layoutFlags: DescriptorSetLayoutCreateFlags
)