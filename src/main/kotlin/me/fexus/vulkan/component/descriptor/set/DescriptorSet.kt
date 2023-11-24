package me.fexus.vulkan.component.descriptor.set

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.component.Device
import me.fexus.vulkan.component.descriptor.pool.DescriptorPool
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayout
import me.fexus.vulkan.component.descriptor.write.DescriptorAccelerationStructureWrite
import me.fexus.vulkan.component.descriptor.write.DescriptorBufferWrite
import me.fexus.vulkan.component.descriptor.write.DescriptorImageWrite
import me.fexus.vulkan.component.descriptor.write.DescriptorWrite
import me.fexus.vulkan.exception.catchVK
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.StructBuffer
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRAccelerationStructure.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET_ACCELERATION_STRUCTURE_KHR
import org.lwjgl.vulkan.VK12.*


class DescriptorSet {
    var vkHandle: Long = 0L; private set

    fun create(device: Device, pool: DescriptorPool, layout: DescriptorSetLayout) = runMemorySafe {
        val pSetLayouts = allocateLong(1)
        pSetLayouts.put(0, layout.vkHandle)

        val descAllocInfo = calloc(VkDescriptorSetAllocateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
            pNext(0)
            descriptorPool(pool.vkHandle)
            pSetLayouts(pSetLayouts)
        }

        val pDescriptorSetHandle = allocateLong(1)
        vkAllocateDescriptorSets(device.vkHandle, descAllocInfo, pDescriptorSetHandle).catchVK()
        this@DescriptorSet.vkHandle = pDescriptorSetHandle[0]

        return@runMemorySafe this@DescriptorSet
    }

    fun update(device: Device, vararg descriptorWrites: DescriptorWrite) = update(device, descriptorWrites.toList())
    fun update(device: Device, descriptorWrites: List<DescriptorWrite>) {
        val structs = mutableListOf<StructBuffer<*,*>>()
        val vkWrites = VkWriteDescriptorSet.calloc(descriptorWrites.size)
        descriptorWrites.forEachIndexed { index, write ->
            val peeNext = if (write is DescriptorAccelerationStructureWrite) {
                val pAccelStructs = memAllocLong(1)
                pAccelStructs.put(0, write.accStructInfos.bufferHandle)
                val descAccStruct = VkWriteDescriptorSetAccelerationStructureKHR.calloc(1)
                descAccStruct[0]
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET_ACCELERATION_STRUCTURE_KHR)
                        .pNext(0L)
                        .accelerationStructureCount(1)
                        .pAccelerationStructures(pAccelStructs)

                structs.add(descAccStruct)

                descAccStruct.address()
            } else 0L

            vkWrites[index]
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .pNext(peeNext)
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
                is DescriptorAccelerationStructureWrite -> {}
                else -> throw Exception("What the fudge?")
            }
        }

        vkUpdateDescriptorSets(device.vkHandle, vkWrites, null)

        structs.forEach { it.free() }
        vkWrites.free()
    }
}