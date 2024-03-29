package me.fexus.examples.coolvoxelrendering.world

import me.fexus.examples.coolvoxelrendering.misc.DescriptorFactory
import me.fexus.examples.coolvoxelrendering.util.MeshUploader
import me.fexus.examples.coolvoxelrendering.util.TextureUploader
import me.fexus.memory.runMemorySafe
import me.fexus.model.CubemapModel
import me.fexus.texture.TextureLoader
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.pipeline.*
import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.image.*
import me.fexus.vulkan.descriptors.image.aspect.ImageAspect
import me.fexus.vulkan.descriptors.image.sampler.AddressMode
import me.fexus.vulkan.descriptors.image.sampler.Filtering
import me.fexus.vulkan.descriptors.image.sampler.VulkanSampler
import me.fexus.vulkan.descriptors.image.sampler.VulkanSamplerConfiguration
import me.fexus.vulkan.descriptors.image.usage.ImageUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
import me.fexus.vulkan.util.ImageExtent3D
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import java.nio.ByteBuffer
import java.nio.ByteOrder


class Cubemap(private val deviceUtil: VulkanDeviceUtil, private val descriptorFactory: DescriptorFactory) {
    private val meshUploader = MeshUploader(deviceUtil)
    private val textureUploader = TextureUploader(deviceUtil)
    val pipeline = GraphicsPipeline()
    private lateinit var vertexBuffer: VulkanBuffer
    private lateinit var sampler: VulkanSampler
    private var vertexCount = -1

    private val cubemapTexture = TextureLoader("textures/surroundsound/cubemap.jpg")
    lateinit var imageArray: VulkanImage; private set


    fun init() {
        val linearSamplerConfig = VulkanSamplerConfiguration(AddressMode.CLAMP_TO_EDGE, 1, Filtering.LINEAR)
        this.sampler = descriptorFactory.createSampler(linearSamplerConfig)

        createImageArray()
        createMeshBuffers()
        createPipeline()
    }

    fun recordRenderCommands(commandBuffer: CommandBuffer, frameIndex: Int) = runMemorySafe {
        val pVertexBuffers = allocateLongValues(vertexBuffer.vkBufferHandle)
        val pOffsets = allocateLongValues(0L)

        val pPushConstants = allocate(128)
        pPushConstants.putInt(0, imageArray.index)

        vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.vkHandle)
        vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
        vkCmdPushConstants(
            commandBuffer.vkHandle,
            pipeline.vkLayoutHandle,
            ShaderStage.BOTH.vkBits,
            0,
            pPushConstants
        )
        vkCmdDraw(commandBuffer.vkHandle, vertexCount, 1, 0, 0)
    }


    private fun createMeshBuffers() {
        val vertexBufferSize = CubemapModel.vertices.size * CubemapModel.Vertex.SIZE_BYTES
        val vertexByteBuffer = ByteBuffer.allocate(vertexBufferSize)
        vertexByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        CubemapModel.vertices.forEachIndexed { index, vertex ->
            val offset = index * CubemapModel.Vertex.SIZE_BYTES
            val floats = vertex.toFloatArray()
            floats.forEachIndexed { fIndex, fl ->
                vertexByteBuffer.putFloat(fIndex * Float.SIZE_BYTES + offset, fl)
            }
        }
        this.vertexBuffer = meshUploader.uploadBuffer(vertexByteBuffer, BufferUsage.VERTEX_BUFFER)
        this.vertexCount = CubemapModel.vertices.size
    }

    private fun createImageArray() {
        val width = cubemapTexture.width
        val height = cubemapTexture.height
        val widthPerLayer = width / 4
        val heightPerLayer = height / 3

        val imageArrayConfig = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D_ARRAY,
            ImageExtent3D(widthPerLayer, heightPerLayer, 1),
            1, 1, 6, ImageColorFormat.B8G8R8A8_SRGB,
            ImageTiling.OPTIMAL, ImageAspect.COLOR, ImageUsage.TRANSFER_DST + ImageUsage.SAMPLED,
            MemoryPropertyFlag.DEVICE_LOCAL
        )
        this.imageArray = descriptorFactory.createImage(imageArrayConfig)

        val helperImage = textureUploader.uploadTexture(cubemapTexture, ImageUsage.TRANSFER_SRC + ImageUsage.TRANSFER_DST + ImageUsage.SAMPLED)

        runMemorySafe {
            val commandBuffer = deviceUtil.beginSingleTimeCommandBuffer()

            cmdImageBarrier(
                commandBuffer.vkHandle, helperImage.vkImageHandle,
                VK_ACCESS_SHADER_READ_BIT, VK_ACCESS_TRANSFER_READ_BIT,
                VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT
            )
            cmdImageBarrier(
                commandBuffer.vkHandle, this@Cubemap.imageArray.vkImageHandle,
                0, VK_ACCESS_TRANSFER_WRITE_BIT,
                VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                layerCount = 6
            )

            val srcSubResourceLayers = calloc(VkImageSubresourceLayers::calloc) {
                set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1)
            }

            val copyRegions = calloc(VkImageCopy::calloc, 6)
            with(copyRegions[0]) {
                val dstSubResourceLayers = calloc(VkImageSubresourceLayers::calloc) {
                    set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1)
                }
                srcSubresource(srcSubResourceLayers)
                srcOffset().x(widthPerLayer).y(0).z(0)
                dstSubresource(dstSubResourceLayers)
                dstOffset().x(0).y(0).z(0)
                extent().width(widthPerLayer).height(heightPerLayer).depth(1)
            }

            with(copyRegions[1]) {
                val dstSubResourceLayers = calloc(VkImageSubresourceLayers::calloc) {
                    set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 1)
                }
                srcSubresource(srcSubResourceLayers)
                srcOffset().x(0).y(heightPerLayer).z(0)
                dstSubresource(dstSubResourceLayers)
                dstOffset().x(0).y(0).z(0)
                extent().width(widthPerLayer).height(heightPerLayer).depth(1)
            }

            with(copyRegions[2]) {
                val dstSubResourceLayers = calloc(VkImageSubresourceLayers::calloc) {
                    set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 2, 1)
                }
                srcSubresource(srcSubResourceLayers)
                srcOffset().x(widthPerLayer).y(heightPerLayer).z(0)
                dstSubresource(dstSubResourceLayers)
                dstOffset().x(0).y(0).z(0)
                extent().width(widthPerLayer).height(heightPerLayer).depth(1)
            }

            with(copyRegions[3]) {
                val dstSubResourceLayers = calloc(VkImageSubresourceLayers::calloc) {
                    set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 3, 1)
                }
                srcSubresource(srcSubResourceLayers)
                srcOffset().x(widthPerLayer * 2).y(heightPerLayer).z(0)
                dstSubresource(dstSubResourceLayers)
                dstOffset().x(0).y(0).z(0)
                extent().width(widthPerLayer).height(heightPerLayer).depth(1)
            }

            with(copyRegions[4]) {
                val dstSubResourceLayers = calloc(VkImageSubresourceLayers::calloc) {
                    set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 4, 1)
                }
                srcSubresource(srcSubResourceLayers)
                srcOffset().x(widthPerLayer * 3).y(heightPerLayer).z(0)
                dstSubresource(dstSubResourceLayers)
                dstOffset().x(0).y(0).z(0)
                extent().width(widthPerLayer).height(heightPerLayer).depth(1)
            }

            with(copyRegions[5]) {
                val dstSubResourceLayers = calloc(VkImageSubresourceLayers::calloc) {
                    set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 5, 1)
                }
                srcSubresource(srcSubResourceLayers)
                srcOffset().x(widthPerLayer).y(heightPerLayer * 2).z(0)
                dstSubresource(dstSubResourceLayers)
                dstOffset().x(0).y(0).z(0)
                extent().width(widthPerLayer).height(heightPerLayer).depth(1)
            }

            vkCmdCopyImage(
                commandBuffer.vkHandle,
                helperImage.vkImageHandle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                this@Cubemap.imageArray.vkImageHandle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                copyRegions
            )

            cmdImageBarrier(
                commandBuffer.vkHandle, this@Cubemap.imageArray.vkImageHandle,
                VK_ACCESS_TRANSFER_WRITE_BIT, VK_ACCESS_SHADER_READ_BIT,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                layerCount = 6
            )

            cmdImageBarrier(
                commandBuffer.vkHandle, helperImage.vkImageHandle,
                VK_ACCESS_TRANSFER_READ_BIT, VK_ACCESS_SHADER_READ_BIT,
                VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
            )

            deviceUtil.endSingleTimeCommandBuffer(commandBuffer)
        }

        helperImage.destroy()
    }

    private fun createPipeline() {
        val vertexShader = ClassLoader.getSystemResource("shaders/coolvoxelrendering/cubemap/cubemap_vert.spv").readBytes()
        val fragmentShader = ClassLoader.getSystemResource("shaders/coolvoxelrendering/cubemap/cubemap_frag.spv").readBytes()
        val pipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC4, 0),
                VertexAttribute(1, VertexAttributeFormat.VEC4, 16),
                VertexAttribute(2, VertexAttributeFormat.VEC4, 32)
            ),
            PushConstantsLayout(128),
            vertexShader, fragmentShader,
            dynamicStates = listOf(DynamicState.SCISSOR, DynamicState.VIEWPORT),
            cullMode = CullMode.FRONTFACE, depthTest = false, depthWrite = false
        )
        this.pipeline.create(deviceUtil.device, listOf(descriptorFactory.descriptorSetLayout), pipelineConfig)
    }


    private fun cmdImageBarrier(
        commandBuffer: VkCommandBuffer, imageHandle: Long,
        srcAccessMask: Int, dstAccessMask: Int,
        oldLayout: Int, newLayout: Int,
        srcPipelineStage: Int, dstPipelineStage: Int,
        layerCount: Int = 1
    ) = runMemorySafe {
        val subResourceRange = calloc(VkImageSubresourceRange::calloc) {
            aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
            baseMipLevel(0)
            levelCount(1)
            baseArrayLayer(0)
            layerCount(layerCount)
        }

        val barrier = calloc(VkImageMemoryBarrier::calloc, 1)
        with(barrier[0]) {
            sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            pNext(0)
            image(imageHandle)
            srcAccessMask(srcAccessMask)
            dstAccessMask(dstAccessMask)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            oldLayout(oldLayout)
            newLayout(newLayout)
            subresourceRange(subResourceRange)
        }

        vkCmdPipelineBarrier(
            commandBuffer, srcPipelineStage, dstPipelineStage,
            0, null, null, barrier
        )
    }


    fun destroy() {
        this.vertexBuffer.destroy()
        this.imageArray.destroy()
        this.pipeline.destroy()
        this.sampler.destroy()
    }
}