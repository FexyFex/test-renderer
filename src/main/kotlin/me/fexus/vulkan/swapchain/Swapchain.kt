package me.fexus.vulkan.swapchain

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.Device
import me.fexus.vulkan.PhysicalDevice
import me.fexus.vulkan.Surface
import me.fexus.vulkan.exception.catchVK
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.VkSurfaceFormatKHR
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR


class Swapchain {
    var vkHandle: Long = 0
    var imageFormat: Int = VK_FORMAT_UNDEFINED

    fun create(surface: Surface, physicalDevice: PhysicalDevice, device: Device, imagesTotal: Int): Swapchain {
        this.vkHandle = runMemorySafe {
            // Capabilities
            val capabilities = calloc<VkSurfaceCapabilitiesKHR>()
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.vkHandle, surface.vkHandle, capabilities).catchVK()

            // Formats
            val pFormatCount = allocateInt(1)
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.vkHandle, surface.vkHandle, pFormatCount, null).catchVK()
            val formatCount = pFormatCount[0]
            if (formatCount <= 0) throw Exception()
            val formats = calloc<VkSurfaceFormatKHR, VkSurfaceFormatKHR.Buffer>(formatCount)
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.vkHandle, surface.vkHandle, pFormatCount, formats).catchVK()
            val surfaceFormat = chooseSurfaceFormat(formats)

            // Present Modes
            val pPresentModeCount = allocateInt(1)
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.vkHandle, surface.vkHandle, pPresentModeCount, null).catchVK()
            val presentModeCount = pPresentModeCount[0]
            if (presentModeCount <= 0) throw Exception()
            val pPresentModes = allocateInt(presentModeCount)
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.vkHandle, surface.vkHandle, pPresentModeCount, pPresentModes).catchVK()

            val swapchainCreateInfo = calloc<VkSwapchainCreateInfoKHR>() {
                sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                pNext(0)
                flags(0)
                minImageCount(imagesTotal)
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

    private fun chooseSurfaceFormat(formats: VkSurfaceFormatKHR.Buffer): VkSurfaceFormatKHR {
        repeat(formats.capacity()) {
            val currentFormat = formats[it]
            if (currentFormat.colorSpace() == preferredColorSpace && currentFormat.format() in preferredImageFormats)
                return currentFormat
        }

        throw Exception()
    }

    private fun choosePresentMode() {}

    fun destroy(device: Device) {
        vkDestroySwapchainKHR(device.vkHandle, vkHandle, null)
    }


    companion object {
        private const val preferredColorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
        private val preferredImageFormats = arrayOf(
            VK_FORMAT_B8G8R8A8_SRGB,
            VK_FORMAT_R8G8B8A8_SRGB
        )
    }
}