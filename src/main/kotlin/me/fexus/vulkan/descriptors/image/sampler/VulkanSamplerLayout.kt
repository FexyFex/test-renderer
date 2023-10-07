package me.fexus.vulkan.descriptors.image.sampler

data class VulkanSamplerLayout(val addressMode: AddressMode, val mipLevels: Int, val filtering: Filtering)
