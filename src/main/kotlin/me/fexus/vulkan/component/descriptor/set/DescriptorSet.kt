package me.fexus.vulkan.component.descriptor.set

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.component.descriptor.pool.DescriptorPool
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayout
import me.fexus.vulkan.component.descriptor.write.DescriptorBufferWrite
import me.fexus.vulkan.component.descriptor.write.DescriptorImageWrite
import me.fexus.vulkan.component.descriptor.write.DescriptorWrite
import me.fexus.vulkan.exception.catchVK
import org.lwjgl.system.StructBuffer
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkDescriptorBufferInfo
import org.lwjgl.vulkan.VkDescriptorImageInfo
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo
import org.lwjgl.vulkan.VkWriteDescriptorSet


class DescriptorSet {
    var vkHandle: Long = 0L; private set

    fun create(device: Device, pool: DescriptorPool, layout: DescriptorSetLayout) = runMemorySafe {
        val pSetLayouts = allocateLong(1)
        pSetLayouts.put(0, layout.vkHandle)

        val descAllocInfo = calloc<VkDescriptorSetAllocateInfo>() {
            sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
            pNext(0)
            descriptorPool(pool.vkHandle)
            pSetLayouts(pSetLayouts)
        }

        val pDescriptorSetHandle = allocateLong(1)
        vkAllocateDescriptorSets(device.vkHandle, descAllocInfo, pDescriptorSetHandle).catchVK()
        this@DescriptorSet.vkHandle = pDescriptorSetHandle[0]
    }

    fun update(device: Device, vararg descriptorWrites: DescriptorWrite) = update(device, descriptorWrites.toList())
    fun update(device: Device, descriptorWrites: List<DescriptorWrite>) {
        val structs = mutableListOf<StructBuffer<*,*>>()
        val vkWrites = VkWriteDescriptorSet.calloc(descriptorWrites.size)
        descriptorWrites.forEachIndexed { index, write ->
            vkWrites[index]
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .pNext(0)
                .dstSet(write.dstSet.vkHandle)
                .dstBinding(write.dstBinding)
                .dstArrayElement(write.dstArrayElement)
                .descriptorCount(write.descriptorCount)
                .descriptorType(write.descriptorType.vkValue)
                .pTexelBufferView(null)
                .pBufferInfo(null)
                .pImageInfo(null)

            when (write) {
                is DescriptorBufferWrite -> {
                    val bufferInfos = VkDescriptorBufferInfo.calloc(write.bufferInfos.size)
                    structs.add(bufferInfos)
                    write.bufferInfos.forEachIndexed { bufIndex, bufInfo ->
                        bufferInfos[bufIndex]
                            .buffer(bufInfo.bufferHandle)
                            .offset(bufInfo.offset)
                            .range(bufInfo.range)
                    }
                    vkWrites[index].pBufferInfo(bufferInfos)
                }
                is DescriptorImageWrite -> {
                    val imageInfos = VkDescriptorImageInfo.calloc(write.imageInfos.size)
                    structs.add(imageInfos)
                    write.imageInfos.forEachIndexed { imgIndex, imgInfo ->
                        imageInfos[imgIndex]
                            .sampler(imgInfo.sampler)
                            .imageView(imgInfo.imageViewHandle)
                            .imageLayout(imgInfo.imageLayout.vkValue)
                    }
                    vkWrites[index].pImageInfo(imageInfos)
                }
                else -> throw Exception("What the fudge?")
            }
        }

        vkUpdateDescriptorSets(device.vkHandle, vkWrites, null)

        structs.forEach { it.free() }
        vkWrites.free()
    }
}