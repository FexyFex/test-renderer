package me.fexus.examples.coolvoxelrendering

import me.fexus.memory.runMemorySafe
import me.fexus.texture.TextureLoader
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.image.*
import me.fexus.vulkan.descriptors.image.aspect.ImageAspect
import me.fexus.vulkan.descriptors.image.usage.IImageUsage
import me.fexus.vulkan.descriptors.image.usage.ImageUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
import me.fexus.vulkan.util.ImageExtent3D
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK12
import org.lwjgl.vulkan.VkBufferImageCopy
import org.lwjgl.vulkan.VkImageMemoryBarrier
import org.lwjgl.vulkan.VkImageSubresourceRange


class TextureUploader(private val deviceUtil: VulkanDeviceUtil) {

    fun uploadTexture(texture: TextureLoader, usage: IImageUsage): VulkanImage {
        val imageConfig = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D, ImageExtent3D(texture.width, texture.height, 1),
            1, 1, 1, ImageColorFormat.B8G8R8A8_SRGB, ImageTiling.OPTIMAL,
            ImageAspect.COLOR, usage, MemoryPropertyFlag.DEVICE_LOCAL
        )
        val image = deviceUtil.createImage(imageConfig)

        val stagingBufferConfig = VulkanBufferConfiguration(
            texture.imageSize,
            MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.TRANSFER_SRC
        )
        val stagingBuffer = deviceUtil.createBuffer(stagingBufferConfig)
        stagingBuffer.put(0, texture.pixels)

        runMemorySafe {
            val cmdBuf = deviceUtil.beginSingleTimeCommandBuffer()

            val subResourceRange = calloc(VkImageSubresourceRange::calloc) {
                aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
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
                .srcAccessMask(0)
                .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                .subresourceRange(subResourceRange)

            vkCmdPipelineBarrier(
                cmdBuf.vkHandle, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                0, null, null, imageBarrier
            )

            val copyRegion = calloc(VkBufferImageCopy::calloc, 1)
            with(copyRegion[0]) {
                bufferOffset(0L)
                imageExtent().width(texture.width).height(texture.height).depth(1)
                imageOffset().x(0).y(0).z(0)
                imageSubresource()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0)
                    .baseArrayLayer(0)
                    .layerCount(1)
            }

            vkCmdCopyBufferToImage(
                cmdBuf.vkHandle,
                stagingBuffer.vkBufferHandle, image.vkImageHandle,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copyRegion
            )

            with(imageBarrier[0]) {
                srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            }

            vkCmdPipelineBarrier(
                cmdBuf.vkHandle,
                VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                0, null, null, imageBarrier
            )

            deviceUtil.endSingleTimeCommandBuffer(cmdBuf)
        }

        stagingBuffer.destroy()

        return image
    }
}