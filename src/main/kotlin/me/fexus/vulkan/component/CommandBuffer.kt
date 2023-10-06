package me.fexus.vulkan.component

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo


class CommandBuffer {
    lateinit var vkHandle: VkCommandBuffer; private set

    fun create(device: Device, commandPool: CommandPool, level: Int = VK_COMMAND_BUFFER_LEVEL_PRIMARY) {
        this.vkHandle = runMemorySafe {
            val cmdBufAllocInfo = calloc<VkCommandBufferAllocateInfo>() {
                sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                pNext(0)
                commandBufferCount(1)
                level(level)
                commandPool(commandPool.vkHandle)
            }

            val pCommandBufferHandle = allocatePointer(1)
            vkAllocateCommandBuffers(device.vkHandle, cmdBufAllocInfo, pCommandBufferHandle)
            return@runMemorySafe VkCommandBuffer(pCommandBufferHandle[0], device.vkHandle)
        }
    }
}