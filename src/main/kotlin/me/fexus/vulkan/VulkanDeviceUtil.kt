package me.fexus.vulkan

import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.accessmask.IAccessMask
import me.fexus.vulkan.component.*
import me.fexus.vulkan.component.pipeline.pipelinestage.IPipelineStage
import me.fexus.vulkan.component.queuefamily.QueueFamily
import me.fexus.vulkan.component.queuefamily.capabilities.QueueFamilyCapability
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.VulkanBufferFactory
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.image.ImageLayout
import me.fexus.vulkan.descriptors.image.VulkanImage
import me.fexus.vulkan.descriptors.image.aspect.IImageAspect
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperty
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
import java.nio.ByteBuffer
import kotlin.contracts.ContractBuilder
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


class VulkanDeviceUtil(private val device: Device, private val bufferFactory: VulkanBufferFactory) {
    private val commandPool = CommandPool()
    private val firstQueue = Queue()

    val vkDeviceHandle: VkDevice; get() = device.vkHandle


    fun init() {
        val firstQueueFamily = QueueFamily(0, QueueFamilyCapability.GRAPHICS, false)
        this.firstQueue.create(device, firstQueueFamily, firstQueueFamily.index)
        commandPool.create(device, firstQueueFamily)
    }

    fun beginSingleTimeCommandBuffer(): CommandBuffer = runMemorySafe {
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

    fun endSingleTimeCommandBuffer(cmdBuf: CommandBuffer): Unit = runMemorySafe {
        vkEndCommandBuffer(cmdBuf.vkHandle)

        val pCommandBuffers = allocatePointer(1)
        pCommandBuffers.put(0, cmdBuf.vkHandle)

        val submitInfo = calloc(VkSubmitInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
            pNext(0)
            pCommandBuffers(pCommandBuffers)
        }

        vkQueueSubmit(firstQueue.vkHandle, submitInfo, 0)
        vkQueueWaitIdle(firstQueue.vkHandle)

        vkFreeCommandBuffers(device.vkHandle, commandPool.vkHandle, pCommandBuffers)
    }

    @OptIn(ExperimentalContracts::class)
    fun runSingleTimeCommands(commands: (commandBuffer: CommandBuffer) -> Unit) {
        contract { callsInPlace(commands, InvocationKind.EXACTLY_ONCE) }
        val cmdBuf = beginSingleTimeCommandBuffer()
        commands(cmdBuf)
        endSingleTimeCommandBuffer(cmdBuf)
    }

    fun stagingCopy(srcData: ByteBuffer, dstBuffer: VulkanBuffer, srcOffset: Long, dstOffset: Long, size: Long) {
        val stagingLayout = VulkanBufferConfiguration(
            size,
            MemoryProperty.HOST_COHERENT + MemoryProperty.HOST_VISIBLE,
            BufferUsage.TRANSFER_SRC
        )

        val stagingBuffer = bufferFactory.createBuffer(stagingLayout)
        assignName(stagingBuffer.vkBufferHandle, VK_OBJECT_TYPE_BUFFER, "staging_buffer_for_copy")

        stagingBuffer.put(srcOffset.toInt(), srcData)

        runMemorySafe {
            val cmdBuf = beginSingleTimeCommandBuffer()

            val copyRegion = calloc(VkBufferCopy::calloc, 1)
            copyRegion[0]
                .srcOffset(srcOffset)
                .dstOffset(dstOffset)
                .size(size)
            vkCmdCopyBuffer(cmdBuf.vkHandle, stagingBuffer.vkBufferHandle, dstBuffer.vkBufferHandle, copyRegion)

            endSingleTimeCommandBuffer(cmdBuf)
        }

        stagingBuffer.destroy()
    }

    fun cmdTransitionImageLayout(
        cmdBuf: CommandBuffer,
        image: VulkanImage,
        srcAccessMask: IAccessMask,
        dstAccessMask: IAccessMask,
        srcLayout: ImageLayout,
        dstLayout: ImageLayout,
        srcPipelineStage: IPipelineStage,
        dstPipelineStage: IPipelineStage
    ) = runMemorySafe {
        val subResourceRange = calloc(VkImageSubresourceRange::calloc) {
            aspectMask(image.config.imageAspect.vkBits)
            baseMipLevel(0)
            levelCount(1)
            baseArrayLayer(0)
            layerCount(1)
        }

        val imageBarrier = calloc(VkImageMemoryBarrier::calloc, 1)
        imageBarrier[0]
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            .pNext(0)
            .image(image.vkImageHandle)
            .srcAccessMask(srcAccessMask.vkBits)
            .dstAccessMask(dstAccessMask.vkBits)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .oldLayout(srcLayout.vkValue)
            .newLayout(dstLayout.vkValue)
            .subresourceRange(subResourceRange)

        vkCmdPipelineBarrier(
            cmdBuf.vkHandle, srcPipelineStage.vkBits, dstPipelineStage.vkBits, 0,
            null, null, imageBarrier
        )
    }

    fun cmdTransitionImageLayout(
        cmdBuf: CommandBuffer,
        image: Long, imageAspect: IImageAspect,
        srcAccessMask: IAccessMask,
        dstAccessMask: IAccessMask,
        srcLayout: ImageLayout,
        dstLayout: ImageLayout,
        srcPipelineStage: IPipelineStage,
        dstPipelineStage: IPipelineStage
    ) = runMemorySafe {
        val subResourceRange = calloc(VkImageSubresourceRange::calloc) {
            aspectMask(imageAspect.vkBits)
            baseMipLevel(0)
            levelCount(1)
            baseArrayLayer(0)
            layerCount(1)
        }

        val imageBarrier = calloc(VkImageMemoryBarrier::calloc, 1)
        imageBarrier[0]
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            .pNext(0)
            .image(image)
            .srcAccessMask(srcAccessMask.vkBits)
            .dstAccessMask(dstAccessMask.vkBits)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .oldLayout(srcLayout.vkValue)
            .newLayout(dstLayout.vkValue)
            .subresourceRange(subResourceRange)

        vkCmdPipelineBarrier(
            cmdBuf.vkHandle, srcPipelineStage.vkBits, dstPipelineStage.vkBits, 0,
            null, null, imageBarrier
        )
    }

    fun createBuffer(bufferConfiguration: VulkanBufferConfiguration) = bufferFactory.createBuffer(bufferConfiguration)

    fun assignName(objHandle: Long, objType: Int, name: String) = runMemorySafe {
        val debugNameInfo = calloc(VkDebugUtilsObjectNameInfoEXT::calloc) {
            sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_OBJECT_NAME_INFO_EXT)
            pNext(0)
            objectType(objType)
            pObjectName(MemoryUtil.memUTF8(name))
            objectHandle(objHandle)
        }

        EXTDebugUtils.vkSetDebugUtilsObjectNameEXT(device.vkHandle, debugNameInfo)
    }


    fun destroy() {
        commandPool.destroy(device)
    }
}