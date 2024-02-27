package me.fexus.vulkan.descriptors.image

import me.fexus.vulkan.descriptors.DescriptorBucket

class ImageBucket: DescriptorBucket<VulkanImage> {
    private val images = mutableListOf<VulkanImage>()
    override val size: Int; get() = images.size

    override fun isEmpty(): Boolean = images.isEmpty()
    override fun iterator(): Iterator<VulkanImage> = images.iterator()
    override fun contains(element: VulkanImage): Boolean = images.contains(element)
    override fun containsAll(elements: Collection<VulkanImage>): Boolean = images.containsAll(elements)

    override fun add(descriptor: VulkanImage) { images.add(descriptor) }
    override fun remove(descriptor: VulkanImage) { images.remove(descriptor) }
    override fun destroyDescriptors(): DescriptorBucket<VulkanImage> {
        images.forEach(VulkanImage::destroy)
        return this
    }
    override fun clear() { images.clear() }
}


infix fun VulkanImage.inside(bucket: ImageBucket): VulkanImage {
    bucket.add(this)
    return this
}