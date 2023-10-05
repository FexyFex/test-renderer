package me.fexus.vulkan

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.queue.Queue
import me.fexus.vulkan.queue.QueueFamilyCapabilities
import me.fexus.vulkan.queue.QueueFamilyCapability
import me.fexus.vulkan.queue.QueueFamily
import me.fexus.vulkan.swapchain.Swapchain
import me.fexus.window.Window
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR
import org.lwjgl.vulkan.VK10.VK_TRUE
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties
import org.lwjgl.vulkan.VkQueueFamilyProperties


class VulkanRenderer(private val window: Window) {
    private val core = VulkanCore()
    private val surface = Surface()
    private val uniqueQueueFamilies = mutableListOf<QueueFamily>()
    private val graphicsQueue = Queue()
    private val presentQueue = Queue()
    private val computeQueue = Queue()
    private val swapchain = Swapchain()


    fun init(): VulkanRenderer {
        core.createInstance()
        surface.create(core.instance, window)
        core.createPhysicalDevice()
        uniqueQueueFamilies.addAll(findUniqueQueueFamilies())
        core.createDevice(uniqueQueueFamilies)
        graphicsQueue.create(core.device, getQueueFamilyWithCapabilities(QueueFamilyCapability.GRAPHICS), 0)
        presentQueue.create(core.device, uniqueQueueFamilies.first { it.supportsPresent }, 0)
        computeQueue.create(core.device, getQueueFamilyWithCapabilities(QueueFamilyCapability.COMPUTE), 0)

        return this
    }


    fun drawFrame() {

    }


    private fun findUniqueQueueFamilies(): List<QueueFamily> {
        val uniqueQueueFamilies = mutableListOf<QueueFamily>()

        runMemorySafe {
            val pQueueFamilyCount = allocateInt(1)
            vkGetPhysicalDeviceQueueFamilyProperties(core.physicalDevice.vkHandle, pQueueFamilyCount, null)
            val queueFamilyCount = pQueueFamilyCount[0]

            val pQueueFamilies = calloc<VkQueueFamilyProperties, VkQueueFamilyProperties.Buffer>(queueFamilyCount)
            vkGetPhysicalDeviceQueueFamilyProperties(core.physicalDevice.vkHandle, pQueueFamilyCount, pQueueFamilies)

            for (i in 0 until queueFamilyCount) {
                val queueFamilyProps = pQueueFamilies[i]

                var capabilities: QueueFamilyCapabilities = QueueFamilyCapability.NONE
                QueueFamilyCapability.values().forEach {
                    if (queueFamilyProps.queueFlags() and it.vkBits != 0)
                        capabilities += it
                }

                val pPresentSupport = allocateInt(1)
                vkGetPhysicalDeviceSurfaceSupportKHR(core.physicalDevice.vkHandle, i, surface.vkHandle, pPresentSupport)
                val presentSupported = pPresentSupport[0] == VK_TRUE

                val fullQueueFamily = QueueFamily(i, capabilities, presentSupported)
                uniqueQueueFamilies.add(fullQueueFamily)
            }
        }

        return uniqueQueueFamilies
    }


    private fun getQueueFamilyWithCapabilities(cap: QueueFamilyCapabilities): QueueFamily {
        return uniqueQueueFamilies.first { cap.vkBits and it.capabilities.vkBits == cap.vkBits }
    }


    fun destroy() {
        surface.destroy(core.instance)
        core.destroy()
    }
}