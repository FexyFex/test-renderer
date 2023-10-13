package me.fexus.vulkan.component

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.component.Device
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkSemaphoreCreateInfo


class Semaphore {
    var vkHandle: Long = 0; private set

    fun create(device: Device) {
        this.vkHandle = runMemorySafe {
            val createInfo = calloc(VkSemaphoreCreateInfo::calloc) {
                sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                pNext(0)
                flags(0)
            }

            val pSemaphoreHandle = allocateLong(1)
            vkCreateSemaphore(device.vkHandle, createInfo, null, pSemaphoreHandle)
            return@runMemorySafe pSemaphoreHandle[0]
        }
    }

    fun destroy(device: Device) {
        vkDestroySemaphore(device.vkHandle, vkHandle, null)
    }
}