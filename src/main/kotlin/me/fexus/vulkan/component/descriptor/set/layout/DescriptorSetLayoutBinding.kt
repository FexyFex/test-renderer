package me.fexus.vulkan.component.descriptor.set.layout

import me.fexus.vulkan.component.descriptor.set.layout.bindingflags.DescriptorSetLayoutBindingFlags
import me.fexus.vulkan.descriptors.DescriptorType
import me.fexus.vulkan.component.pipeline.ShaderStage


data class DescriptorSetLayoutBinding(
    val dstBinding: Int,
    val descriptorCount: Int,
    val descriptorType: DescriptorType,
    val shaderStage: ShaderStage,
    val flags: DescriptorSetLayoutBindingFlags
)
