package me.fexus.vulkan.component

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.component.queuefamily.QueueFamily
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkCommandPoolCreateInfo

class CommandPool {
    var vkHandle: Long = 0L; private set

    fun create(device: Device, queueFamily: QueueFamily) {
        this.vkHandle = runMemorySafe {
            val poolCreateInfo = calloc<VkCommandPoolCreateInfo>() {
                sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                pNext(0)
                flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                queueFamilyIndex(queueFamily.index)
            }

            val pCommandPoolHandle = allocateLong(1)
            vkCreateCommandPool(device.vkHandle, poolCreateInfo, null, pCommandPoolHandle)
            return@runMemorySafe pCommandPoolHandle[0]
        }
    }

    fun destroy(device: Device) {
        vkDestroyCommandPool(device.vkHandle, vkHandle, null)
    }
}