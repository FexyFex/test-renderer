package me.fexus.vulkan.sync

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.Device
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkFenceCreateInfo


class Fence {
    var vkHandle: Long = 0; private set

    fun create(device: Device) {
        this.vkHandle = runMemorySafe {
            val fenceInfo = calloc<VkFenceCreateInfo>() {
                sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                pNext(0)
                flags(VK_FENCE_CREATE_SIGNALED_BIT)
            }

            val pFenceHandle = allocateLong(1)
            vkCreateFence(device.vkHandle, fenceInfo, null, pFenceHandle)
            return@runMemorySafe pFenceHandle[0]
        }
    }

    fun destroy(device: Device) {
        vkDestroyFence(device.vkHandle, vkHandle, null)
    }
}