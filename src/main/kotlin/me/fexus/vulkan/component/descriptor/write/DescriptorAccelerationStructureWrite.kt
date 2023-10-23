package me.fexus.vulkan.component.descriptor.write

import me.fexus.vulkan.component.descriptor.set.DescriptorSet
import me.fexus.vulkan.descriptors.DescriptorType


data class DescriptorAccelerationStructureWrite(
    override val dstBinding: Int,
    override val descriptorType: DescriptorType,
    override val descriptorCount: Int,
    override val dstSet: DescriptorSet,
    override val dstArrayElement: Int,
    val accStructInfos: DescriptorBufferInfo
): DescriptorWrite