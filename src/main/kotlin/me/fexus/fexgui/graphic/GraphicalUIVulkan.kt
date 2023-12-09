package me.fexus.fexgui.graphic

import me.fexus.fexgui.graphic.vulkan.IndexedVulkanImage
import me.fexus.fexgui.graphic.vulkan.util.ImageBlit
import me.fexus.memory.runMemorySafe
import me.fexus.model.QuadModel
import me.fexus.vulkan.accessmask.IAccessMask
import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.pipeline.GraphicsPipeline
import me.fexus.vulkan.component.pipeline.pipelinestage.IPipelineStage
import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.image.ImageLayout
import me.fexus.vulkan.descriptors.image.VulkanImage
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkImageBlit
import org.lwjgl.vulkan.VkImageMemoryBarrier
import org.lwjgl.vulkan.VkImageSubresourceRange
import java.nio.ByteBuffer


interface GraphicalUIVulkan {
    val images: MutableList<IndexedVulkanImage>
    val pipeline: GraphicsPipeline
    val vertexBuffer: VulkanBuffer
    val indexBuffer: VulkanBuffer


    fun beginGUICommandRecordContext(cmdBuf: CommandBuffer, recordBlock: CommandBufferContext.() -> Unit) {
        val context = CommandBufferContext(cmdBuf, pipeline)
        context.recordBlock()
    }


    class CommandBufferContext(private val commandBuffer: CommandBuffer, private val pipeline: GraphicsPipeline) {
        fun bindPipeline() {
            vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.vkHandle)
        }

        fun bindVertexBuffer(buffer: VulkanBuffer) = runMemorySafe {
            val pVertexBuffers = allocateLongValues(buffer.vkBufferHandle)
            val pOffsets = allocateLongValues(0L)
            vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
        }

        fun bindIndexBuffer(buffer: VulkanBuffer) {
            vkCmdBindIndexBuffer(commandBuffer.vkHandle, buffer.vkBufferHandle, 0L, VK_INDEX_TYPE_UINT32)
        }

        fun drawIndexed() {
            vkCmdDrawIndexed(commandBuffer.vkHandle, QuadModel.indices.size,
                1, 0, 0, 0)
        }

        fun pushConstants(buffer: ByteBuffer) {
            vkCmdPushConstants(commandBuffer.vkHandle, pipeline.vkLayoutHandle, ShaderStage.BOTH.vkBits, 0, buffer)
        }

        fun blitImage(srcImage: VulkanImage, dstImage: VulkanImage, blits: List<ImageBlit>) = runMemorySafe {
            val pRegions = calloc(VkImageBlit::calloc, blits.size)
            pRegions.forEachIndexed { index, vkImageBlit ->
                val b = blits[index]
                vkImageBlit.srcSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1)
                vkImageBlit.dstSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1)
                vkImageBlit.srcOffsets(0).set(b.srcRegion.min.x, b.srcRegion.min.y, b.srcRegion.min.z)
                vkImageBlit.srcOffsets(1).set(b.srcRegion.max.x, b.srcRegion.max.y, b.srcRegion.max.z)
                vkImageBlit.dstOffsets(0).set(b.dstRegion.min.x, b.dstRegion.min.y, b.dstRegion.min.z)
                vkImageBlit.dstOffsets(1).set(b.dstRegion.max.x, b.dstRegion.max.y, b.dstRegion.max.z)
            }

            vkCmdBlitImage(
                commandBuffer.vkHandle,
                srcImage.vkImageHandle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                dstImage.vkImageHandle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                pRegions, VK_FILTER_NEAREST
            )
        }

        fun transitionImageLayout(
            image: VulkanImage,
            srcAccessMask: IAccessMask, dstAccessMask: IAccessMask,
            oldLayout: ImageLayout, newLayout: ImageLayout,
            srcPipelineStage: IPipelineStage, dstPipelineStage: IPipelineStage
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
                .oldLayout(oldLayout.vkValue)
                .newLayout(newLayout.vkValue)
                .subresourceRange(subResourceRange)

            vkCmdPipelineBarrier(
                commandBuffer.vkHandle, srcPipelineStage.vkBits, dstPipelineStage.vkBits, 0,
                null, null, imageBarrier
            )
        }
    }
}