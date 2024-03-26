package me.fexus.examples.coolvoxelrendering

import me.fexus.memory.runMemorySafe
import me.fexus.texture.TextureLoader
import me.fexus.voxel.VoxelRegistry
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.image.*
import me.fexus.vulkan.descriptors.image.aspect.ImageAspect
import me.fexus.vulkan.descriptors.image.usage.ImageUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
import me.fexus.vulkan.util.ImageExtent3D
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferImageCopy
import org.lwjgl.vulkan.VkImageMemoryBarrier
import org.lwjgl.vulkan.VkImageSubresourceRange


class TextureArray(private val deviceUtil: VulkanDeviceUtil, private val descriptorFactory: DescriptorFactory) {
    lateinit var image: VulkanImage


    fun init() {
        val layerCount = VoxelRegistry.voxelCount
        val cloudTex = TextureLoader("textures/coolvoxelrendering/cloud.png")
        val grass = TextureLoader("textures/coolvoxelrendering/grass.png")
        val stoneTex = TextureLoader("textures/coolvoxelrendering/stone.png")
        val textures = arrayOf(cloudTex, stoneTex, grass)

        val imageConfig = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D_ARRAY, ImageExtent3D(grass.width, grass.height, 1),
            1, 1, layerCount, ImageColorFormat.R8G8B8A8_SRGB, ImageTiling.OPTIMAL,
            ImageAspect.COLOR, ImageUsage.SAMPLED + ImageUsage.TRANSFER_DST, MemoryPropertyFlag.DEVICE_LOCAL
        )
        this.image = descriptorFactory.createImage(imageConfig)

        val stagingBufferConfig = VulkanBufferConfiguration(
            grass.imageSize * layerCount, MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.TRANSFER_SRC
        )
        val stagingBuffer = deviceUtil.createBuffer(stagingBufferConfig)

        textures.forEachIndexed { index, tex ->
            stagingBuffer.put(index * tex.imageSize.toInt(), tex.pixels)
        }

        runMemorySafe {
            val cmdBuf = deviceUtil.beginSingleTimeCommandBuffer()

            val subResourceRange = calloc(VkImageSubresourceRange::calloc) {
                aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                baseMipLevel(0)
                levelCount(1)
                baseArrayLayer(0)
                layerCount(layerCount)
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
                imageExtent().width(grass.width).height(grass.height).depth(1)
                imageOffset().x(0).y(0).z(0)
                imageSubresource()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0)
                    .baseArrayLayer(0)
                    .layerCount(1)
            }

            var layer = 0
            textures.forEach {
                copyRegion[0].bufferOffset(layer * it.imageSize)
                copyRegion[0].imageSubresource().baseArrayLayer(layer++)

                vkCmdCopyBufferToImage(
                    cmdBuf.vkHandle,
                    stagingBuffer.vkBufferHandle, image.vkImageHandle,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copyRegion
                )
            }

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
    }


    fun destroy() {
        image.destroy()
    }
}