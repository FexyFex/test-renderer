package me.fexus.vulkan.component.descriptor.pool.flags

interface DescriptorPoolCreateFlags {
    val vkBits: Int

    operator fun plus(other: DescriptorPoolCreateFlag) =
        CombinedDescriptorPoolCreateFlags(this.vkBits or other.vkBits)
}