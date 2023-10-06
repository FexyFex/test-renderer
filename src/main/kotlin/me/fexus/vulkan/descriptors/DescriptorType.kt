package me.fexus.vulkan.descriptors

import org.lwjgl.vulkan.VK12.*


enum class DescriptorType(val vkValue: Int) {
    SAMPLED_IMAGE(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE),
    UNIFORM_BUFFER(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER),
    STORAGE_BUFFER(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER),
    SAMPLER(VK_DESCRIPTOR_TYPE_SAMPLER),
    COMBINED_IMAGE_SAMPLER(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER),
    NONE(0);

    companion object {
        fun Int.toDescriptorTypeString(): String {
            return enumValues<DescriptorType>().find { it.vkValue == this }?.name ?: "No matching descriptor type for enum $this"
        }
    }
}