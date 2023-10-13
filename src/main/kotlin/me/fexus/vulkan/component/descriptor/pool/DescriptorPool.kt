package me.fexus.vulkan.component.descriptor.pool

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.component.Device
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo
import org.lwjgl.vulkan.VkDescriptorPoolSize


class DescriptorPool {
    var vkHandle: Long = 0L; private set

    fun create(device: Device, plan: DescriptorPoolPlan) = runMemorySafe {
        val poolSizes = calloc(VkDescriptorPoolSize::calloc, plan.sizes.size)
        plan.sizes.forEachIndexed { index, poolSize ->
            poolSizes[index]
                .type(poolSize.descriptorType.vkValue)
                .descriptorCount(poolSize.count)
        }

        val poolInfo = calloc(VkDescriptorPoolCreateInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
            pNext(0)
            flags(plan.flags.vkBits)
            maxSets(plan.maxSets)
            pPoolSizes(poolSizes)
        }

        val pPoolHandle = allocateLong(1)
        vkCreateDescriptorPool(device.vkHandle, poolInfo, null, pPoolHandle)
        this@DescriptorPool.vkHandle = pPoolHandle[0]
    }

    fun destroy(device: Device) {
        vkDestroyDescriptorPool(device.vkHandle, vkHandle, null)
    }
}