package me.fexus.vulkan.queue

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.Device
import me.fexus.vulkan.queue.family.QueueFamily
import org.lwjgl.vulkan.VK10.vkGetDeviceQueue
import org.lwjgl.vulkan.VkQueue


class Queue {
    lateinit var vkHandle: VkQueue


    fun create(device: Device, queueFamily: QueueFamily, queueIndex: Int): Queue {
        this.vkHandle = runMemorySafe {
            val ppQueue = allocatePointer(1)
            vkGetDeviceQueue(device.vkHandle, queueFamily.index, queueIndex, ppQueue)
            return@runMemorySafe VkQueue(ppQueue[0], device.vkHandle)
        }

        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Queue) return false
        return this.vkHandle.address() == other.vkHandle.address()
    }

    override fun hashCode(): Int {
        return vkHandle.hashCode()
    }
}