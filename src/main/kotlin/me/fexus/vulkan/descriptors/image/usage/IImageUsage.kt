package me.fexus.vulkan.descriptors.image.usage

interface IImageUsage {
    val vkBits: Int

    operator fun plus(other: IImageUsage): CombinedImageUsage = CombinedImageUsage(other.vkBits or this.vkBits)

    operator fun contains(usage: IImageUsage): Boolean {
        return (this.vkBits and usage.vkBits) == usage.vkBits
    }
}