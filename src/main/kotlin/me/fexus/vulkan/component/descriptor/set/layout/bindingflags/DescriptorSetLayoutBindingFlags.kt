package me.fexus.vulkan.component.descriptor.set.layout.bindingflags

interface DescriptorSetLayoutBindingFlags {
    val vkBits: Int

    operator fun plus(other: DescriptorSetLayoutBindingFlags) =
        CombinedDescriptorSetLayoutBindingFlags(this.vkBits or other.vkBits)
}