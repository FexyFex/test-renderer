package me.fexus.vulkan.component.descriptor.set.layout

import me.fexus.vulkan.descriptors.DescriptorType
import me.fexus.vulkan.pipeline.ShaderStage


data class DescriptorSetLayoutBinding(
    val dstBinding: Int,
    val descriptorCount: Int,
    val descriptorType: DescriptorType,
    val shaderStage: ShaderStage
)
