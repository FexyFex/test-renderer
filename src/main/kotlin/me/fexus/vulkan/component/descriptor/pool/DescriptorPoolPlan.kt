package me.fexus.vulkan.component.descriptor.pool

import me.fexus.vulkan.component.descriptor.pool.flags.DescriptorPoolCreateFlags

data class DescriptorPoolPlan(
    val maxSets: Int,
    val flags: DescriptorPoolCreateFlags,
    val sizes: List<DescriptorPoolSize>
)