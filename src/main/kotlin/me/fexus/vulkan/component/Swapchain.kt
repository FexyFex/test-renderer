package me.fexus.vulkan.component

import me.fexus.examples.Globals
import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.exception.catchVK
import me.fexus.vulkan.component.queuefamily.QueueFamily
import me.fexus.vulkan.component.queuefamily.capabilities.QueueFamilyCapability
import me.fexus.vulkan.descriptors.image.ImageColorFormat
import me.fexus.vulkan.util.ImageExtent2D
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK12.*
import java.nio.IntBuffer


class Swapchain {
    var vkHandle: Long = 0L; private set
    var images = Array(Globals.FRAMES_TOTAL) { 0L }
    var imageViews = Array(Globals.FRAMES_TOTAL) { 0L }

    lateinit var imageColorFormat: ImageColorFormat; private set
    lateinit var imageExtent: ImageExtent2D; private set

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
            val capabilities = calloc(VkSurfaceCapabilitiesKHR::calloc)
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.vkHandle, surface.vkHandle, capabilities).catchVK()

            // Formats
            val pFormatCount = allocateInt(1)
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.vkHandle, surface.vkHandle, pFormatCount, null).catchVK()
            val formatCount = pFormatCount[0]
            if (formatCount <= 0) throw Exception()
            val formats = calloc(VkSurfaceFormatKHR::calloc, formatCount)
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.vkHandle, surface.vkHandle, pFormatCount, formats).catchVK()
            val surfaceFormat = chooseSurfaceFormat(formats)
            this@Swapchain.imageColorFormat = ImageColorFormat.values().first { it.vkValue == surfaceFormat.format() }

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

            val swapchainCreateInfo = calloc(VkSwapchainCreateInfoKHR::calloc) {
                sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                pNext(0)
                flags(0)
                minImageCount(imagesTotal)
                imageFormat(surfaceFormat.format())
                imageColorSpace(surfaceFormat.colorSpace())
                imageExtent(calloc(VkExtent2D::calloc) { width(imageExtent.width); height(imageExtent.height) })
                imageArrayLayers(1)
                imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT)
                imageSharingMode(imageSharingMode)
                pQueueFamilyIndices(pQueueFamilyIndices)
                preTransform(preTransform)
                compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                presentMode(presentMode)
                clipped(true)
                oldSwapchain(0)
                surface(surface.vkHandle)
            }

            this@Swapchain.imageExtent = imageExtent

            val pSwapchainHandle = allocateLong(1)
            vkCreateSwapchainKHR(device.vkHandle, swapchainCreateInfo, null, pSwapchainHandle).catchVK()
            val swapchainHandle = pSwapchainHandle[0]

            createImages(device, swapchainHandle)

            return@runMemorySafe swapchainHandle
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
        var bestMode = VK_PRESENT_MODE_MAILBOX_KHR //VK_PRESENT_MODE_FIFO_KHR
        for (i in 0 until pPresentModes.capacity()) {
            val currentPresentMode = pPresentModes[i]
            // We directly return the preferred mode if we find it
            if (currentPresentMode == preferredPresentMode) return currentPresentMode
            // Relaxed is still better than normal FIFO
            if (currentPresentMode == VK_PRESENT_MODE_FIFO_RELAXED_KHR) bestMode = currentPresentMode
        }
        return bestMode
    }

    private fun createImages(device: Device, swapchainHandle: Long) = runMemorySafe {
        val pImageCount = allocateInt(1)
        vkGetSwapchainImagesKHR(device.vkHandle, swapchainHandle, pImageCount, null)
        val imageCount = pImageCount[0]
        val pImages = allocateLong(imageCount)
        vkGetSwapchainImagesKHR(device.vkHandle, swapchainHandle, pImageCount, pImages)
        repeat(imageCount) {
            val image = pImages[it]
            this@Swapchain.images[it] = image

            val imageViewCreateInfo = calloc(VkImageViewCreateInfo::calloc) {
                sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                pNext(0)
                image(image)
                viewType(VK_IMAGE_VIEW_TYPE_2D)
                format(this@Swapchain.imageColorFormat.vkValue)
                components()
                    .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .a(VK_COMPONENT_SWIZZLE_IDENTITY)
                subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1)
            }

            val pImageViewHandle = allocateLong(1)
            vkCreateImageView(device.vkHandle, imageViewCreateInfo, null, pImageViewHandle)
            this@Swapchain.imageViews[it] = pImageViewHandle[0]
        }
    }

    fun destroy(device: Device) {
        imageViews.forEach { vkDestroyImageView(device.vkHandle, it, null) }
        vkDestroySwapchainKHR(device.vkHandle, vkHandle, null)
    }


    companion object {
        private const val preferredColorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
        private val preferredImageFormats = arrayOf(VK_FORMAT_B8G8R8A8_SRGB)

        private const val preferredPresentMode = VK_PRESENT_MODE_MAILBOX_KHR
    }
}