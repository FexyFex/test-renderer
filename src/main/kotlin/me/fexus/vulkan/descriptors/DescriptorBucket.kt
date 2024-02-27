package me.fexus.vulkan.descriptors


// Descriptor Buckets are meant to be a simple descriptor sorting utility.
// Descriptors can be put into a bucket and can then be modified or destroyed as a group.
interface DescriptorBucket<T>: Collection<T> {
    fun add(descriptor: T)
    fun remove(descriptor: T)
    fun destroyDescriptors(): DescriptorBucket<T>
    fun clear()
}