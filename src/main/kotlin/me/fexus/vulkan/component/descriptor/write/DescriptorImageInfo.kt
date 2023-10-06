package me.fexus.vulkan.component.descriptor.write

import me.fexus.vulkan.descriptors.image.ImageLayout

data class DescriptorImageInfo(
    val sampler: Long,
    val imageViewHandle: Long,
    val imageLayout: ImageLayout
)
