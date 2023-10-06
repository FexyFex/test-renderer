package me.fexus.vulkan.component.descriptor.write

data class DescriptorBufferInfo(
    val bufferHandle: Long,
    val offset: Long,
    val range: Long
)
