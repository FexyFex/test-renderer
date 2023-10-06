package me.fexus.vulkan

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.exception.catchVK
import me.fexus.vulkan.queue.family.QueueFamily
import me.fexus.vulkan.queue.family.capabilities.QueueFamilyCapability
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.VkSurfaceFormatKHR
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR
import java.nio.IntBuffer


class Swapchain {
    var vkHandle: Long = 0
    var format: Int = VK_FORMAT_UNDEFINED
    lateinit var extent: ImageExtent2D

    fun create(
        surface: Surface,
        physicalDevice: PhysicalDevice,
        device: Device,
        imagesTotal: Int,
        imageExtent: ImageExtent2D,
        uniqueQueueFamilies: List<QueueFamily>
    ): Swapchain {
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
            this@Swapchain.format = surfaceFormat.format()

            // Present Modes
            val pPresentModeCount = allocateInt(1)
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.vkHandle, surface.vkHandle, pPresentModeCount, null).catchVK()
            val presentModeCount = pPresentModeCount[0]
            if (presentModeCount <= 0) throw Exception()
            val pPresentModes = allocateInt(presentModeCount)
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.vkHandle, surface.vkHandle, pPresentModeCount, pPresentModes).catchVK()
            val presentMode = choosePresentMode(pPresentModes)

            // Queue Family Indices and Sharing Mode
            val graphicsQueueFamily = uniqueQueueFamilies.first { QueueFamilyCapability.GRAPHICS in it.capabilities }
            val presentQueueFamily = uniqueQueueFamilies.first { it.supportsPresent }
            var pQueueFamilyIndices: IntBuffer? = null
            val imageSharingMode: Int
            if (graphicsQueueFamily.index == presentQueueFamily.index) {
                imageSharingMode = VK_SHARING_MODE_EXCLUSIVE
            } else {
                pQueueFamilyIndices = allocateInt(uniqueQueueFamilies.size)
                uniqueQueueFamilies.forEachIndexed { index, queueFamily ->
                    pQueueFamilyIndices.put(index, queueFamily.index)
                }
                imageSharingMode = VK_SHARING_MODE_CONCURRENT
            }

            val preTransform = if (capabilities.supportedTransforms() and VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR != 0)
                VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR
            else
                capabilities.currentTransform()

            val swapchainCreateInfo = calloc<VkSwapchainCreateInfoKHR>() {
                sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                pNext(0)
                flags(0)
                minImageCount(imagesTotal)
                imageFormat(surfaceFormat.format())
                imageColorSpace(surfaceFormat.colorSpace())
                imageExtent(calloc<VkExtent2D>() { width(imageExtent.width); height(imageExtent.height) })
                imageArrayLayers(1)
                imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                imageSharingMode(imageSharingMode)
                pQueueFamilyIndices(pQueueFamilyIndices)
                preTransform(preTransform)
                compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                presentMode(presentMode)
                clipped(true)
                oldSwapchain(0)
                surface(surface.vkHandle)
            }

            this@Swapchain.extent = imageExtent

            val pSwapchainHandle = allocateLong(1)
            vkCreateSwapchainKHR(device.vkHandle, swapchainCreateInfo, null, pSwapchainHandle).catchVK()
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

    private fun choosePresentMode(pPresentModes: IntBuffer): Int {
        var bestMode = VK_PRESENT_MODE_FIFO_KHR
        for (i in 0 until pPresentModes.capacity()) {
            val currentPresentMode = pPresentModes[i]
            // We directly return the preferred mode if we find it
            if (currentPresentMode == preferredPresentMode) return currentPresentMode
            // Relaxed is still better than normal FIFO
            if (currentPresentMode == VK_PRESENT_MODE_FIFO_RELAXED_KHR) bestMode = currentPresentMode
        }
        return bestMode
    }

    fun destroy(device: Device) {
        vkDestroySwapchainKHR(device.vkHandle, vkHandle, null)
    }


    companion object {
        private const val preferredColorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
        private val preferredImageFormats = arrayOf(VK_FORMAT_B8G8R8A8_SRGB, VK_FORMAT_R8G8B8A8_SRGB)

        private const val preferredPresentMode = VK_PRESENT_MODE_MAILBOX_KHR
    }
}