package me.fexus.vulkan.descriptors.buffer.usage

interface IBufferUsage {
    val vkBits: Int

    operator fun plus(other: IBufferUsage): CombinedBufferUsage = CombinedBufferUsage(other.vkBits or this.vkBits)

    operator fun contains(usage: IBufferUsage): Boolean {
        return (this.vkBits and usage.vkBits) == usage.vkBits
    }
}