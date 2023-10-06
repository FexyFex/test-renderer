package me.fexus.vulkan

import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.vulkan.descriptors.buffer.VulkanBufferFactory
import me.fexus.vulkan.exception.catchVK
import me.fexus.vulkan.pipeline.stage.PipelineStage
import me.fexus.vulkan.queue.Queue
import me.fexus.vulkan.queue.family.capabilities.QueueFamilyCapabilities
import me.fexus.vulkan.queue.family.capabilities.QueueFamilyCapability
import me.fexus.vulkan.queue.family.QueueFamily
import me.fexus.vulkan.sync.Fence
import me.fexus.vulkan.sync.Semaphore
import me.fexus.window.Window
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPresentInfoKHR
import org.lwjgl.vulkan.VkQueueFamilyProperties
import org.lwjgl.vulkan.VkSubmitInfo


abstract class VulkanRendererBase(protected val window: Window) {
    protected val core = VulkanCore()
    protected val bufferFactory = VulkanBufferFactory()
    protected val surface = Surface()
    protected val uniqueQueueFamilies = mutableListOf<QueueFamily>()
    protected val graphicsQueue = Queue()
    protected val presentQueue = Queue()
    protected val computeQueue = Queue()
    protected val swapchain = Swapchain()
    protected val commandPool = CommandPool()
    protected val commandBuffers = Array(FRAMES_IN_FLIGHT) { CommandBuffer() }
    protected val imageAvailableSemaphores = Array(FRAMES_IN_FLIGHT) { Semaphore() }
    protected val renderFinishedSemaphores = Array(FRAMES_IN_FLIGHT) { Semaphore() }
    protected val inFlightFences = Array(FRAMES_IN_FLIGHT) { Fence() }

    protected var currentFrame: Int = 0; private set
    protected var currentFrameInFlight: Int = 0; private set


    fun initVulkanCore(): VulkanRendererBase {
        core.createInstance()
        surface.create(core.instance, window)
        core.createPhysicalDevice()
        uniqueQueueFamilies.addAll(findUniqueQueueFamilies())
        core.createDevice(uniqueQueueFamilies)

        val graphicsCapableQueueFamily = getQueueFamilyWithCapabilities(QueueFamilyCapability.GRAPHICS)
        val computeCapableQueueFamily = getQueueFamilyWithCapabilities(QueueFamilyCapability.COMPUTE)
        graphicsQueue.create(core.device, graphicsCapableQueueFamily, 0)
        presentQueue.create(core.device, uniqueQueueFamilies.first { it.supportsPresent }, 0)
        computeQueue.create(core.device, computeCapableQueueFamily, 0)
        val extent = window.extent2D
        swapchain.create(surface, core.physicalDevice, core.device, FRAMES_TOTAL, ImageExtent2D(extent.x, extent.y), uniqueQueueFamilies)

        commandPool.create(core.device, graphicsCapableQueueFamily)
        commandBuffers.forEach { it.create(core.device, commandPool) }

        imageAvailableSemaphores.forEach { it.create(core.device) }
        renderFinishedSemaphores.forEach { it.create(core.device) }
        inFlightFences.forEach { it.create(core.device) }

        bufferFactory.init(core.physicalDevice, core.device)

        return this
    }


    fun prepareFrame(): FramePreparation {
        return runMemorySafe {
            val pWaitFence = allocateLong(1)
            pWaitFence.put(0, inFlightFences[currentFrameInFlight].vkHandle)
            vkWaitForFences(core.device.vkHandle, pWaitFence, true, Long.MAX_VALUE)

            val pImageIndex = allocateInt(1)
            val resultAcquire = vkAcquireNextImageKHR(
                core.device.vkHandle,
                swapchain.vkHandle,
                Long.MAX_VALUE,
                imageAvailableSemaphores[currentFrameInFlight].vkHandle,
                0,
                pImageIndex
            )
            val imageIndex = pImageIndex[0]

            val pResetFence = allocateLong(1)
            pResetFence.put(0, inFlightFences[currentFrameInFlight].vkHandle)
            vkResetFences(core.device.vkHandle, pResetFence)

            if (resultAcquire == VK_SUCCESS) return@runMemorySafe FramePreparation(true, imageIndex)
            if (resultAcquire == VK_ERROR_OUT_OF_DATE_KHR) {
                resizeSwapchain()
                return@runMemorySafe FramePreparation(false, imageIndex)
            }

            resultAcquire.catchVK()
            throw Exception()
        }
    }

    abstract fun recordFrame(preparation: FramePreparation): FrameSubmitData

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

            val submitInfo = calloc<VkSubmitInfo>() {
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

            val presentInfo = calloc<VkPresentInfoKHR>() {
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
        core.device.waitIdle().catchVK()
        destroySwapchain()
        recreateSwapchain()
    }

    private fun destroySwapchain() {
        swapchain.destroy(core.device)
    }

    private fun recreateSwapchain() {
        val newExtentVec = window.extent2D
        val newExtent = ImageExtent2D(newExtentVec.x, newExtentVec.y)
        swapchain.create(
            surface,
            core.physicalDevice,
            core.device,
            FRAMES_TOTAL,
            newExtent,
            uniqueQueueFamilies
        )
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


    open fun destroy() {
        swapchain.destroy(core.device)
        surface.destroy(core.instance)
        core.destroy()
    }


    companion object {
        const val FRAMES_TOTAL = 3
        const val FRAMES_IN_FLIGHT = 2
    }
}