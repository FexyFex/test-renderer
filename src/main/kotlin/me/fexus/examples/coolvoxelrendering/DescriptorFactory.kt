package me.fexus.examples.coolvoxelrendering

import me.fexus.examples.Globals
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.component.descriptor.pool.DescriptorPool
import me.fexus.vulkan.component.descriptor.set.DescriptorSet
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayout
import me.fexus.vulkan.component.descriptor.write.*
import me.fexus.vulkan.descriptors.DescriptorType
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.image.ImageLayout
import me.fexus.vulkan.descriptors.image.VulkanImage
import me.fexus.vulkan.descriptors.image.VulkanImageConfiguration
import me.fexus.vulkan.descriptors.image.sampler.VulkanSampler
import me.fexus.vulkan.descriptors.image.sampler.VulkanSamplerConfiguration
import org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
import org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE


class DescriptorFactory(
    private val deviceUtil: VulkanDeviceUtil,
    private val descriptorPool: DescriptorPool,
    val descriptorSetLayout: DescriptorSetLayout
) {
    private val descriptorTypeIndices = mutableMapOf<DescriptorType, Int>()
    private val descriptors = mutableMapOf<DescriptorType, MutableList<DescriptorInfo>>()

    lateinit var descriptorSets: Array<DescriptorSet>; private set


    init {
        descriptorSetLayout.plan.bindings.forEach { binding ->
            descriptorTypeIndices[binding.descriptorType] = binding.dstBinding
        }
    }

    fun init() {
        descriptorSets = Array(Globals.FRAMES_TOTAL) {
            DescriptorSet().create(deviceUtil.device, descriptorPool, descriptorSetLayout)
        }
    }


    fun createBuffer(bufferConfiguration: VulkanBufferConfiguration): VulkanBuffer {
        val buffer = deviceUtil.createBuffer(bufferConfiguration)

        val descriptorType = when {
            (BufferUsage.UNIFORM_BUFFER in bufferConfiguration.usage) -> DescriptorType.UNIFORM_BUFFER
            (BufferUsage.STORAGE_BUFFER in bufferConfiguration.usage) -> DescriptorType.STORAGE_BUFFER
            else -> throw Exception("Unrecognized Buffer Usage: ${bufferConfiguration.usage}")
        }

        val list = descriptors.getOrPut(descriptorType) { mutableListOf() }
        list.add(DescriptorInfo(longArrayOf(buffer.vkBufferHandle), arrayOf(buffer::index.setter)))

        return buffer
    }

    fun createNBuffers(bufferConfiguration: VulkanBufferConfiguration): Array<VulkanBuffer> {
        val buffers = Array(Globals.FRAMES_TOTAL) { deviceUtil.createBuffer(bufferConfiguration) }

        val descriptorType = when {
            (BufferUsage.UNIFORM_BUFFER in bufferConfiguration.usage) -> DescriptorType.UNIFORM_BUFFER
            (BufferUsage.STORAGE_BUFFER in bufferConfiguration.usage) -> DescriptorType.STORAGE_BUFFER
            else -> throw Exception("Unrecognized Buffer Usage: ${bufferConfiguration.usage}")
        }

        val list = descriptors.getOrPut(descriptorType) { mutableListOf() }
        list.add(DescriptorInfo(buffers.map { it.vkBufferHandle }.toLongArray(), buffers.map { it::index.setter }.toTypedArray()))

        return buffers
    }

    fun createImage(imageConfiguration: VulkanImageConfiguration): VulkanImage {
        val image = deviceUtil.createImage(imageConfiguration)

        val list = descriptors.getOrPut(DescriptorType.SAMPLED_IMAGE) { mutableListOf() }
        list.add(DescriptorInfo(longArrayOf(image.vkImageViewHandle), arrayOf(image::index.setter)))

        return image
    }

    fun createSampler(samplerConfiguration: VulkanSamplerConfiguration): VulkanSampler {
        val sampler: VulkanSampler = deviceUtil.createSampler(samplerConfiguration)

        val list = descriptors.getOrPut(DescriptorType.SAMPLER) { mutableListOf() }
        list.add(DescriptorInfo(longArrayOf(sampler.vkHandle), arrayOf(sampler::index.setter)))

        return sampler
    }


    fun updateDescriptorSet() {
       descriptorSets.forEachIndexed { descriptorSetIndex, descriptorSet ->
           val descriptorWrites = mutableListOf<DescriptorWrite>()

            for (descriptorBindingIndex in descriptorTypeIndices) {
                val (descriptorType, dstBinding) = descriptorBindingIndex
                val descriptorsToBind: List<DescriptorInfo> = descriptors[descriptorType]!!

                for ((descriptorIndex, descriptor) in descriptorsToBind.withIndex()) {
                    val descWrite: DescriptorWrite = when (descriptorType) {
                        DescriptorType.STORAGE_BUFFER, DescriptorType.UNIFORM_BUFFER -> {
                            val index = if (descriptor.vkHandles.size != 1) descriptorSetIndex else 0
                            val buf = descriptor.vkHandles[index]
                            descriptor.indexSetters[index](descriptorIndex)

                            DescriptorBufferWrite(
                                dstBinding, descriptorType,1, descriptorSet, descriptorIndex,
                                listOf(DescriptorBufferInfo(buf, 0L, VK_WHOLE_SIZE))
                            )
                        }

                        DescriptorType.SAMPLED_IMAGE -> {
                            descriptor.indexSetters[0](descriptorIndex)
                            DescriptorImageWrite(
                                dstBinding, descriptorType, 1, descriptorSet, descriptorIndex,
                                listOf(DescriptorImageInfo(0L, descriptor.vkHandles[0], ImageLayout.SHADER_READ_ONLY_OPTIMAL))
                            )
                        }

                        DescriptorType.SAMPLER -> {
                            descriptor.indexSetters[0](descriptorIndex)
                            DescriptorImageWrite(
                                dstBinding, descriptorType, 1, descriptorSet, descriptorIndex,
                                listOf(DescriptorImageInfo(descriptor.vkHandles[0], 0L, ImageLayout.SHADER_READ_ONLY_OPTIMAL))
                            )
                        }

                        else -> throw Exception("Unexpected DescriptorType: $descriptorType")
                    }

                    descriptorWrites.add(descWrite)
                }
            }
           descriptorSet.update(deviceUtil.device, descriptorWrites)
        }
    }


    fun destroy() {
        descriptorTypeIndices.clear()
        descriptors.clear()
    }


    private class DescriptorInfo(val vkHandles: LongArray, val indexSetters: Array<(Int) -> Unit>)
}