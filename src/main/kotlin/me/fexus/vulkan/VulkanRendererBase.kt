package me.fexus.vulkan

import me.fexus.examples.RenderApplication
import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.CommandPool
import me.fexus.vulkan.component.Surface
import me.fexus.vulkan.component.Swapchain
import me.fexus.vulkan.descriptors.buffer.VulkanBufferFactory
import me.fexus.vulkan.descriptors.image.VulkanImageFactory
import me.fexus.vulkan.exception.catchVK
import me.fexus.vulkan.component.pipeline.pipelinestage.PipelineStage
import me.fexus.vulkan.component.Queue
import me.fexus.vulkan.component.queuefamily.capabilities.QueueFamilyCapabilities
import me.fexus.vulkan.component.queuefamily.capabilities.QueueFamilyCapability
import me.fexus.vulkan.component.queuefamily.QueueFamily
import me.fexus.vulkan.component.Fence
import me.fexus.vulkan.component.Semaphore
import me.fexus.vulkan.extension.DeviceExtension
import me.fexus.vulkan.util.FramePreparation
import me.fexus.vulkan.util.FrameSubmitData
import me.fexus.vulkan.util.ImageExtent2D
import me.fexus.window.Window
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBufferBeginInfo
import org.lwjgl.vulkan.VkPresentInfoKHR
import org.lwjgl.vulkan.VkQueueFamilyProperties
import org.lwjgl.vulkan.VkSubmitInfo


abstract class VulkanRendererBase(protected val window: Window): RenderApplication {
    private val core = VulkanCore()
    protected val bufferFactory = VulkanBufferFactory()
    protected val imageFactory = VulkanImageFactory()
    protected val surface = Surface()
    protected val uniqueQueueFamilies = mutableListOf<QueueFamily>()
    protected val graphicsQueue = Queue()
    protected val presentQueue = Queue()
    protected val swapchain = Swapchain()
    protected val commandPool = CommandPool()
    protected val commandBuffers = Array(FRAMES_IN_FLIGHT) { CommandBuffer() }
    protected val imageAvailableSemaphores = Array(FRAMES_IN_FLIGHT) { Semaphore() }
    protected val renderFinishedSemaphores = Array(FRAMES_IN_FLIGHT) { Semaphore() }
    protected val inFlightFences = Array(FRAMES_IN_FLIGHT) { Fence() }

    protected var currentFrame: Int = 0; private set
    protected var currentFrameInFlight: Int = 0; private set

    protected val device; get() = core.device


    fun initVulkanCore(extensions: List<DeviceExtension> = emptyList()): VulkanRendererBase {
        core.enabledExtensions.addAll(extensions)
        core.createInstance()
        surface.create(core.instance, window)
        core.createPhysicalDevice()
        uniqueQueueFamilies.addAll(findUniqueQueueFamilies())
        core.createDevice(uniqueQueueFamilies)

        val graphicsCapableQueueFamily = getQueueFamilyWithCapabilities(QueueFamilyCapability.GRAPHICS)
        graphicsQueue.create(device, graphicsCapableQueueFamily, 0)
        presentQueue.create(device, uniqueQueueFamilies.first { it.supportsPresent }, 0)

        val extent = window.extent2D
        swapchain.create(surface, core.physicalDevice, device, FRAMES_TOTAL, ImageExtent2D(extent.x, extent.y), uniqueQueueFamilies)

        commandPool.create(device, graphicsCapableQueueFamily)
        commandBuffers.forEach { it.create(device, commandPool) }

        imageAvailableSemaphores.forEach { it.create(device) }
        renderFinishedSemaphores.forEach { it.create(device) }
        inFlightFences.forEach { it.create(device) }

        bufferFactory.init(core.physicalDevice, device)
        imageFactory.init(core.physicalDevice, device)

        return this
    }


    fun prepareFrame(): FramePreparation {
        return runMemorySafe {
            val pWaitFence = allocateLong(1)
            pWaitFence.put(0, inFlightFences[currentFrameInFlight].vkHandle)
            vkWaitForFences(device.vkHandle, pWaitFence, true, Long.MAX_VALUE)

            val pImageIndex = allocateInt(1)
            val resultAcquire = vkAcquireNextImageKHR(
                device.vkHandle,
                swapchain.vkHandle,
                Long.MAX_VALUE,
                imageAvailableSemaphores[currentFrameInFlight].vkHandle,
                0,
                pImageIndex
            )
            val imageIndex = pImageIndex[0]

            if (resultAcquire == VK_ERROR_OUT_OF_DATE_KHR) {
                resizeSwapchain()
                return@runMemorySafe FramePreparation(false, imageIndex)
            }

            val pResetFence = allocateLong(1)
            pResetFence.put(0, inFlightFences[currentFrameInFlight].vkHandle)
            vkResetFences(device.vkHandle, pResetFence)

            if (resultAcquire == VK_SUCCESS || resultAcquire == VK_SUBOPTIMAL_KHR)
                return@runMemorySafe FramePreparation(true, imageIndex)

            resultAcquire.catchVK()
            throw Exception()
        }
    }

    abstract fun recordFrame(preparation: FramePreparation, delta: Float): FrameSubmitData

    fun submitFrame(frameSubmitData: FrameSubmitData) {
        if (!frameSubmitData.doSubmit) return

        runMemorySafe {
            val pWaitSemaphores = allocateLong(1 + frameSubmitData.additionalWaitSemaphores.size)
            val pCommandBuffers = allocatePointer(1 + frameSubmitData.additionalCommandBuffers.size)
            val pSignalSemaphores = allocateLong(1 + frameSubmitData.additionalSignalSemaphores.size)
            val pWaitStages = allocateInt(1 + frameSubmitData.additionalWaitStages.size)

            pWaitSemaphores.put(0, imageAvailableSemaphores[currentFrameInFlight].vkHandle)
            frameSubmitData.additionalWaitSemaphores.forEachIndexed { index, semaphore ->
                pWaitSemaphores.put(index + 1, semaphore.vkHandle)
            }

            pCommandBuffers.put(0, commandBuffers[currentFrameInFlight].vkHandle)
            frameSubmitData.additionalCommandBuffers.forEachIndexed { index, commandBuffer ->
                pCommandBuffers.put(index + 1, commandBuffer.vkHandle)
            }

            pSignalSemaphores.put(0, renderFinishedSemaphores[currentFrameInFlight].vkHandle)
            frameSubmitData.additionalSignalSemaphores.forEachIndexed { index, semaphore ->
                pSignalSemaphores.put(index+ 1, semaphore.vkHandle)
            }

            pWaitStages.put(0, PipelineStage.COLOR_ATTACHMENT_OUTPUT.vkBits)
            frameSubmitData.additionalWaitStages.forEachIndexed { index, waitStage ->
                pWaitStages.put(index + 1, waitStage.vkBits)
            }

            val submitInfo = calloc(VkSubmitInfo::calloc) {
                sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                pNext(0)
                waitSemaphoreCount(pWaitSemaphores.capacity())
                pWaitSemaphores(pWaitSemaphores)
                pCommandBuffers(pCommandBuffers)
                pSignalSemaphores(pSignalSemaphores)
                pWaitDstStageMask(pWaitStages)
            }

            vkQueueSubmit(graphicsQueue.vkHandle, submitInfo, inFlightFences[currentFrameInFlight].vkHandle).catchVK()

            val pImageIndices = allocateInt(1)
            pImageIndices.put(0, frameSubmitData.imageIndex)

            val pSwapchains = allocateLong(1)
            pSwapchains.put(0, swapchain.vkHandle)

            val presentInfo = calloc(VkPresentInfoKHR::calloc) {
                sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                pNext(0)
                pWaitSemaphores(pSignalSemaphores)
                swapchainCount(1)
                pSwapchains(pSwapchains)
                pImageIndices(pImageIndices)
                pResults(null)
            }

            val presentResult = vkQueuePresentKHR(presentQueue.vkHandle, presentInfo)

            if (presentResult == VK_ERROR_OUT_OF_DATE_KHR || presentResult == VK_SUBOPTIMAL_KHR) {
                resizeSwapchain()
            } else if (presentResult != VK_SUCCESS) presentResult.catchVK()

            currentFrame = (currentFrame + 1) % FRAMES_TOTAL
            currentFrameInFlight = (currentFrameInFlight + 1) % FRAMES_IN_FLIGHT
        }
    }


    private fun resizeSwapchain() {
        window.waitForFramebufferResize()
        device.waitIdle().catchVK()

        swapchain.destroy(device)
        onResizeDestroy()

        val winExtent = window.extent2D
        val newExtent = ImageExtent2D(winExtent.x, winExtent.y)

        swapchain.create(
            surface,
            core.physicalDevice,
            device,
            FRAMES_TOTAL,
            newExtent,
            uniqueQueueFamilies
        )
        onResizeRecreate(newExtent)
    }

    abstract fun onResizeDestroy()
    abstract fun onResizeRecreate(newExtent2D: ImageExtent2D)


    private fun findUniqueQueueFamilies(): List<QueueFamily> {
        val uniqueQueueFamilies = mutableListOf<QueueFamily>()

        runMemorySafe {
            val pQueueFamilyCount = allocateInt(1)
            vkGetPhysicalDeviceQueueFamilyProperties(core.physicalDevice.vkHandle, pQueueFamilyCount, null)
            val queueFamilyCount = pQueueFamilyCount[0]

            val pQueueFamilies = calloc(VkQueueFamilyProperties::calloc, queueFamilyCount)
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

    protected fun beginSingleTimeCommandBuffer(): CommandBuffer = runMemorySafe {
        val cmdBuf = CommandBuffer().create(device, commandPool)

        val beginInfo = calloc(VkCommandBufferBeginInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            pNext(0)
            flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
            pInheritanceInfo(null)
        }

        vkBeginCommandBuffer(cmdBuf.vkHandle, beginInfo)

        return@runMemorySafe cmdBuf
    }

    protected fun endSingleTimeCommandBuffer(cmdBuf: CommandBuffer): Unit = runMemorySafe {
        vkEndCommandBuffer(cmdBuf.vkHandle)

        val pCommandBuffers = allocatePointer(1)
        pCommandBuffers.put(0, cmdBuf.vkHandle)

        val submitInfo = calloc(VkSubmitInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
            pNext(0)
            pCommandBuffers(pCommandBuffers)
        }

        vkQueueSubmit(graphicsQueue.vkHandle, submitInfo, 0)
        vkQueueWaitIdle(graphicsQueue.vkHandle)

        vkFreeCommandBuffers(device.vkHandle, commandPool.vkHandle, pCommandBuffers)
    }


    open fun destroy() {
        device.waitIdle()
        imageAvailableSemaphores.forEach { it.destroy(device) }
        renderFinishedSemaphores.forEach { it.destroy(device) }
        inFlightFences.forEach { it.destroy(device) }
        commandPool.destroy(device)
        swapchain.destroy(device)
        surface.destroy(core.instance)
        core.destroy()
    }


    companion object {
        const val FRAMES_TOTAL = 3
        const val FRAMES_IN_FLIGHT = 2
    }
}