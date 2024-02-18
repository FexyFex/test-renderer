package me.fexus.vulkan.descriptors.image.feature

interface FormatFeatureFlags {
    val vkBits: Long

    operator fun plus(other: FormatFeatureFlags) = CombinedFormatFeatureFlags(this.vkBits or other.vkBits)

    operator fun contains(others: FormatFeatureFlags): Boolean {
        return (this.vkBits and others.vkBits) == others.vkBits
    }
}