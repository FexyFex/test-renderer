package me.fexus.vulkan.swapchain

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.Device
import org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR
import org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR


class Swapchain {
    var vkHandle: Long = 0


    fun create(device: Device): Swapchain {
        this.vkHandle = runMemorySafe {
            val swapchainCreateInfo = calloc<VkSwapchainCreateInfoKHR>() {
                sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                pNext(0)
                flags(0)
                minImageCount()
                imageFormat()
                imageColorSpace()
                imageExtent()
                imageArrayLayers()
                imageUsage()
                imageSharingMode()
                queueFamilyIndexCount()
                pQueueFamilyIndices()
                preTransform()
                compositeAlpha()
                presentMode()
                clipped()
                oldSwapchain()
            }

            val pSwapchainHandle = allocateLong(1)
            vkCreateSwapchainKHR(device.vkHandle, swapchainCreateInfo, null, pSwapchainHandle)
            return@runMemorySafe pSwapchainHandle[0]
        }

        return this
    }
}