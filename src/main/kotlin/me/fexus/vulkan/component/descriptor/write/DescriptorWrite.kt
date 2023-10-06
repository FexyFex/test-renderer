package me.fexus.vulkan.component.descriptor.write

import me.fexus.vulkan.component.descriptor.set.DescriptorSet
import me.fexus.vulkan.descriptors.DescriptorType


interface DescriptorWrite {
    val dstSet: DescriptorSet
    val dstBinding: Int
    val descriptorType: DescriptorType
    val descriptorCount: Int
    val dstArrayElement: Int
}