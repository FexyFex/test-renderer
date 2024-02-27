package me.fexus.vulkan.descriptors.buffer

import me.fexus.vulkan.descriptors.DescriptorBucket


class BufferBucket: DescriptorBucket<VulkanBuffer> {
    private val buffers: MutableList<VulkanBuffer> = mutableListOf()
    override val size: Int; get() = buffers.size

    override fun isEmpty(): Boolean = buffers.isEmpty()
    override fun contains(element: VulkanBuffer): Boolean = buffers.contains(element)
    override fun containsAll(elements: Collection<VulkanBuffer>): Boolean = buffers.containsAll(elements)
    override fun iterator(): Iterator<VulkanBuffer> = buffers.iterator()

    override fun add(descriptor: VulkanBuffer) { buffers.add(descriptor) }
    override fun remove(descriptor: VulkanBuffer) { buffers.remove(descriptor) }
    override fun destroyDescriptors(): DescriptorBucket<VulkanBuffer> {
        buffers.forEach(VulkanBuffer::destroy)
        return this
    }
    override fun clear() { buffers.clear() }
}


infix fun VulkanBuffer.inside(bucket: BufferBucket): VulkanBuffer {
    bucket.add(this)
    return this
}