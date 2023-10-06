package me.fexus.vulkan.component.descriptor.write

import me.fexus.vulkan.component.descriptor.set.DescriptorSet
import me.fexus.vulkan.descriptors.DescriptorType


data class DescriptorBufferWrite(
    override val dstBinding: Int,
    override val descriptorType: DescriptorType,
    override val descriptorCount: Int,
    override val dstSet: DescriptorSet,
    override val dstArrayElement: Int,
    val bufferInfos: List<DescriptorBufferInfo>,
): DescriptorWrite