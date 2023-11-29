package me.fexus.vulkan.descriptors.image.sampler

data class VulkanSamplerConfiguration(val addressMode: AddressMode, val mipLevels: Int, val filtering: Filtering)
