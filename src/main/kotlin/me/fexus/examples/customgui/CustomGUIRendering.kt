package me.fexus.examples.customgui

import me.fexus.examples.customgui.gui.logic.LogicalGUI
import me.fexus.examples.customgui.gui.graphic.GlyphAtlas
import me.fexus.examples.customgui.gui.logic.component.*
import me.fexus.math.vec.IVec2
import me.fexus.memory.runMemorySafe
import me.fexus.model.QuadModel
import me.fexus.texture.TextureLoader
import me.fexus.vulkan.util.FramePreparation
import me.fexus.vulkan.util.FrameSubmitData
import me.fexus.vulkan.VulkanRendererBase
import me.fexus.vulkan.accessmask.AccessMask
import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.descriptor.pool.DescriptorPool
import me.fexus.vulkan.component.descriptor.pool.DescriptorPoolPlan
import me.fexus.vulkan.component.descriptor.pool.DescriptorPoolSize
import me.fexus.vulkan.component.descriptor.pool.flags.DescriptorPoolCreateFlag
import me.fexus.vulkan.component.descriptor.set.DescriptorSet
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayout
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayoutBinding
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayoutPlan
import me.fexus.vulkan.component.descriptor.set.layout.bindingflags.DescriptorSetLayoutBindingFlag
import me.fexus.vulkan.component.descriptor.set.layout.createflags.DescriptorSetLayoutCreateFlag
import me.fexus.vulkan.component.descriptor.write.DescriptorBufferInfo
import me.fexus.vulkan.component.descriptor.write.DescriptorBufferWrite
import me.fexus.vulkan.component.descriptor.write.DescriptorImageInfo
import me.fexus.vulkan.component.descriptor.write.DescriptorImageWrite
import me.fexus.vulkan.component.pipeline.*
import me.fexus.vulkan.descriptors.DescriptorType
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.image.*
import me.fexus.vulkan.descriptors.image.aspect.ImageAspect
import me.fexus.vulkan.descriptors.image.usage.ImageUsage
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperty
import me.fexus.vulkan.component.pipeline.pipelinestage.PipelineStage
import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage
import me.fexus.vulkan.descriptors.image.sampler.AddressMode
import me.fexus.vulkan.descriptors.image.sampler.Filtering
import me.fexus.vulkan.descriptors.image.sampler.VulkanSampler
import me.fexus.vulkan.descriptors.image.sampler.VulkanSamplerConfiguration
import me.fexus.vulkan.util.ImageExtent2D
import me.fexus.vulkan.util.ImageExtent3D
import me.fexus.window.Window
import me.fexus.window.input.InputHandler
import me.fexus.window.input.Key
import me.fexus.window.input.event.InputEventSubscriber
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRDynamicRendering.*
import org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR
import org.lwjgl.vulkan.VK12.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min


class CustomGUIRendering: VulkanRendererBase(createWindow()), InputEventSubscriber {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CustomGUIRendering().start()
        }

        private fun createWindow() = Window("Custom GUI Rendering") {
            windowVisible()
            enableResizable()
            setInitialWindowSize(1067,600)
            enableDecoration()
            setInitialWindowPosition(400, 300)
            enableAutoIconify()
        }
    }

    private val inputHandler = InputHandler(window)

    private val images = mutableMapOf<Int, VulkanImage>()
    private lateinit var glyphImage: VulkanImage
    private lateinit var depthAttachment: VulkanImage
    private lateinit var vertexBuffer: VulkanBuffer
    private lateinit var indexBuffer: VulkanBuffer
    private lateinit var screenInfoBuffer: VulkanBuffer
    private lateinit var sampler: VulkanSampler
    private val descriptorPool = DescriptorPool()
    private val descriptorSetLayout = DescriptorSetLayout()
    private val descriptorSet = DescriptorSet()
    private val pipeline = GraphicsPipeline()

    private val glyphAtlas = GlyphAtlas()
    private val gui = LogicalGUI()


    fun start() {
        initVulkanCore()
        initObjects()
        createGUIDescriptors()
        createGUI()
        createDescriptorSet()
        subscribe(inputHandler)
        startRenderLoop(window, this)
    }


    private fun initObjects() {
        createAttachmentImages()
        createMeshBuffers()

        // -- SCREEN INFO BUFFER --
        val cameraBufferConfig = VulkanBufferConfiguration(
            64L,
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.UNIFORM_BUFFER
        )
        this.screenInfoBuffer = deviceUtil.createBuffer(cameraBufferConfig)
        writeScreenInfoBuffer(window.extent2D)
        // -- SCREEN INFO BUFFER --

        // -- DEFAULT SAMPLER --
        val samplerConfig = VulkanSamplerConfiguration(AddressMode.CLAMP_TO_EDGE, 1, Filtering.NEAREST)
        this.sampler = deviceUtil.createSampler(samplerConfig)
        // -- DEFAULT SAMPLER --
    }

    private fun createAttachmentImages() {
        val depthAttachmentImageLayout = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D,
            ImageExtent3D(swapchain.imageExtent, 1),
            1, 1, 1,
            ImageColorFormat.D32_SFLOAT, ImageTiling.OPTIMAL,
            ImageAspect.DEPTH, ImageUsage.DEPTH_STENCIL_ATTACHMENT,
            MemoryProperty.DEVICE_LOCAL
        )
        this.depthAttachment = imageFactory.createImage(depthAttachmentImageLayout)
    }

    private fun createMeshBuffers() {
        // VERETX BUFFER
        val vertexBufferSize = QuadModel.Vertex.SIZE_BYTES * QuadModel.vertices.size
        val vertexBufferConfig = VulkanBufferConfiguration(
            vertexBufferSize.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.VERTEX_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.vertexBuffer = deviceUtil.createBuffer(vertexBufferConfig)

        val vertexByteBuffer = ByteBuffer.allocate(vertexBufferSize)
        vertexByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        QuadModel.vertices.forEachIndexed { index, vertex ->
            val offset = index * QuadModel.Vertex.SIZE_BYTES
            vertex.writeToByteBuffer(vertexByteBuffer, offset)
        }

        deviceUtil.stagingCopy(vertexByteBuffer, vertexBuffer, 0L, 0L, vertexBufferSize.toLong())
        // VERETX BUFFER

        // INDEX BUFFER
        val indexBufferSize = QuadModel.indices.size * Int.SIZE_BYTES
        val indexBufferConfig = VulkanBufferConfiguration(
            indexBufferSize.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.INDEX_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.indexBuffer = deviceUtil.createBuffer(indexBufferConfig)

        val indexByteBuffer = ByteBuffer.allocate(indexBufferSize)
        indexByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        QuadModel.indices.forEachIndexed { index, cubeIndex ->
            val offset = index * Int.SIZE_BYTES
            indexByteBuffer.putInt(offset, cubeIndex)
        }

        deviceUtil.stagingCopy(indexByteBuffer, indexBuffer, 0L, 0L, indexBufferSize.toLong())
        // INDEX BUFFER
    }


    private fun createGUIDescriptors() {
        val texture0 = TextureLoader("textures/customgui/texture0.jpg")
        val image0Config = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D, ImageExtent3D(texture0.width, texture0.height, 1),
            1, 1, 1, ImageColorFormat.R8G8B8A8_SRGB, ImageTiling.OPTIMAL,
            ImageAspect.COLOR, ImageUsage.SAMPLED + ImageUsage.TRANSFER_DST, MemoryProperty.DEVICE_LOCAL
        )
        val image0 = deviceUtil.createImage(image0Config)
        this.images[0] = image0
        val stagingBufConfig = VulkanBufferConfiguration(
            texture0.imageSize,
            MemoryProperty.HOST_COHERENT + MemoryProperty.HOST_VISIBLE,
            BufferUsage.TRANSFER_SRC
        )
        val img0stagingBuf = deviceUtil.createBuffer(stagingBufConfig)

        img0stagingBuf.copy(MemoryUtil.memAddress(texture0.pixels), 0, texture0.imageSize)

        runMemorySafe {
            deviceUtil.runSingleTimeCommands { cmdBuf ->
                deviceUtil.cmdTransitionImageLayout(
                    cmdBuf, image0,
                    AccessMask.NONE, AccessMask.TRANSFER_WRITE,
                    ImageLayout.UNDEFINED, ImageLayout.TRANSFER_DST_OPTIMAL,
                    PipelineStage.BOTTOM_OF_PIPE, PipelineStage.TRANSFER
                )

                val subResource = calloc(VkImageSubresourceLayers::calloc) {
                    set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1)
                }
                val offset3D = calloc(VkOffset3D::calloc) { set(0, 0, 0) }
                val extent3D = calloc(VkExtent3D::calloc) { set(texture0.width, texture0.height, 1) }
                val copyRegion = calloc(VkBufferImageCopy::calloc, 1)
                copyRegion[0].set(0L, 0, 0, subResource, offset3D, extent3D)

                vkCmdCopyBufferToImage(
                    cmdBuf.vkHandle,
                    img0stagingBuf.vkBufferHandle, image0.vkImageHandle,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copyRegion
                )

                deviceUtil.cmdTransitionImageLayout(
                    cmdBuf, image0,
                    AccessMask.TRANSFER_WRITE, AccessMask.SHADER_READ,
                    ImageLayout.TRANSFER_DST_OPTIMAL, ImageLayout.SHADER_READ_ONLY_OPTIMAL,
                    PipelineStage.TRANSFER, PipelineStage.FRAGMENT_SHADER
                )
            }
        }

        img0stagingBuf.destroy()

        val textImageConfig = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D,
            ImageExtent3D(12 * glyphAtlas.glyphWidth, glyphAtlas.glyphHeight, 1),
            1, 1, 1, ImageColorFormat.R8G8B8A8_SRGB, ImageTiling.OPTIMAL,
            ImageAspect.COLOR, ImageUsage.TRANSFER_DST + ImageUsage.SAMPLED, MemoryProperty.DEVICE_LOCAL
        )
        images[1] = deviceUtil.createImage(textImageConfig)

        val glyphImageConfig = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D,
            ImageExtent3D(glyphAtlas.texture.width, glyphAtlas.texture.height, 1),
            1, 1, 1, ImageColorFormat.R8G8B8A8_SRGB, ImageTiling.OPTIMAL,
            ImageAspect.COLOR, ImageUsage.SAMPLED + ImageUsage.TRANSFER_SRC + ImageUsage.TRANSFER_DST,
            MemoryProperty.DEVICE_LOCAL
        )
        this.glyphImage = deviceUtil.createImage(glyphImageConfig)

        val glyphStagingBufConfig = VulkanBufferConfiguration(
            glyphAtlas.texture.imageSize,
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.TRANSFER_SRC
        )
        val glyphStagingBuf = deviceUtil.createBuffer(glyphStagingBufConfig)
        glyphStagingBuf.copy(MemoryUtil.memAddress(glyphAtlas.texture.pixels), 0L, glyphAtlas.texture.imageSize)

        runMemorySafe {
            val cmdBuf = deviceUtil.beginSingleTimeCommandBuffer()

            deviceUtil.cmdTransitionImageLayout(
                cmdBuf, images[1]!!,
                AccessMask.NONE, AccessMask.SHADER_READ,
                ImageLayout.UNDEFINED, ImageLayout.SHADER_READ_ONLY_OPTIMAL,
                PipelineStage.BOTTOM_OF_PIPE, PipelineStage.FRAGMENT_SHADER
            )

            deviceUtil.cmdTransitionImageLayout(
                cmdBuf, glyphImage,
                AccessMask.NONE, AccessMask.TRANSFER_WRITE,
                ImageLayout.UNDEFINED, ImageLayout.TRANSFER_DST_OPTIMAL,
                PipelineStage.BOTTOM_OF_PIPE, PipelineStage.TRANSFER
            )

            val subResourceLayer = calloc(VkImageSubresourceLayers::calloc) {
                set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1)
            }
            val extent3D = calloc(VkExtent3D::calloc) { set(glyphAtlas.texture.width, glyphAtlas.texture.height, 1) }
            val offset3D = calloc(VkOffset3D::calloc) { set(0, 0, 0) }
            val copy = calloc(VkBufferImageCopy::calloc, 1)
            copy[0].set(0L, 0, 0, subResourceLayer, offset3D, extent3D)

            vkCmdCopyBufferToImage(
                cmdBuf.vkHandle,
                glyphStagingBuf.vkBufferHandle, glyphImage.vkImageHandle,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copy
            )

            deviceUtil.cmdTransitionImageLayout(
                cmdBuf, glyphImage,
                AccessMask.TRANSFER_WRITE, AccessMask.TRANSFER_READ,
                ImageLayout.TRANSFER_DST_OPTIMAL, ImageLayout.TRANSFER_SRC_OPTIMAL,
                PipelineStage.TRANSFER, PipelineStage.TRANSFER
            )

            deviceUtil.endSingleTimeCommandBuffer(cmdBuf)
        }

        glyphStagingBuf.destroy()
    }


    private fun createGUI() {
        gui.addComponent(textRect)
        gui.addComponent(textureRect)
    }


    private fun createDescriptorSet() {
        // Descriptor Sets and Pipeline
        val poolPlan = DescriptorPoolPlan(
            FRAMES_TOTAL, DescriptorPoolCreateFlag.FREE_DESCRIPTOR_SET,
            listOf(
                DescriptorPoolSize(DescriptorType.UNIFORM_BUFFER, 4),
                DescriptorPoolSize(DescriptorType.SAMPLED_IMAGE, 16),
                DescriptorPoolSize(DescriptorType.SAMPLER, 1)
            )
        )
        this.descriptorPool.create(device, poolPlan)

        val setLayoutPlan = DescriptorSetLayoutPlan(
            DescriptorSetLayoutCreateFlag.NONE,
            listOf(
                DescriptorSetLayoutBinding(
                    0, 1,
                    DescriptorType.UNIFORM_BUFFER,
                    ShaderStage.VERTEX,
                    DescriptorSetLayoutBindingFlag.NONE
                ),
                DescriptorSetLayoutBinding(
                    1, 16,
                    DescriptorType.SAMPLED_IMAGE,
                    ShaderStage.FRAGMENT,
                    DescriptorSetLayoutBindingFlag.PARTIALLY_BOUND
                ),
                DescriptorSetLayoutBinding(
                    2, 1,
                    DescriptorType.SAMPLER,
                    ShaderStage.FRAGMENT,
                    DescriptorSetLayoutBindingFlag.NONE
                )
            )
        )
        this.descriptorSetLayout.create(device, setLayoutPlan)

        this.descriptorSet.create(device, descriptorPool, descriptorSetLayout)

        val pipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC4, 0),
                VertexAttribute(1, VertexAttributeFormat.VEC4, 16),
            ),
            PushConstantsLayout(128),
            ClassLoader.getSystemResource("shaders/customgui/standard_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/customgui/standard_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.VIEWPORT, DynamicState.SCISSOR),
            blendEnable = true, cullMode = CullMode.NONE
        )
        this.pipeline.create(device, descriptorSetLayout, pipelineConfig)

        // Update Descriptor Set
        val descWriteCameraBuf = DescriptorBufferWrite(
            0, DescriptorType.UNIFORM_BUFFER, 1, this.descriptorSet, 0,
            listOf(DescriptorBufferInfo(screenInfoBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE))
        )
        val descWriteTextures = DescriptorImageWrite(
            1, DescriptorType.SAMPLED_IMAGE, images.size, this.descriptorSet, 0,
            images.map { DescriptorImageInfo(0L, it.value.vkImageViewHandle, ImageLayout.SHADER_READ_ONLY_OPTIMAL) }
        )
        val descWriteSampler = DescriptorImageWrite(
            2, DescriptorType.SAMPLER, 1, this.descriptorSet, 0,
            listOf(DescriptorImageInfo(sampler.vkHandle, 0L , ImageLayout.SHADER_READ_ONLY_OPTIMAL))
        )

        this.descriptorSet.update(device, descWriteCameraBuf, descWriteTextures, descWriteSampler)
    }


    private fun frameUpdate() {
        handleInput()
    }

    override fun recordFrame(preparation: FramePreparation, delta: Float): FrameSubmitData = runMemorySafe {
        frameUpdate()

        val width: Int = swapchain.imageExtent.width
        val height: Int = swapchain.imageExtent.height

        val cmdBeginInfo = calloc(VkCommandBufferBeginInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            pNext(0)
            flags(0)
            pInheritanceInfo(null)
        }

        val commandBuffer = commandBuffers[currentFrameInFlight]
        val swapchainImage = swapchain.images[preparation.imageIndex]
        val swapchainImageView = swapchain.imageViews[preparation.imageIndex]

        val clearValueColor = calloc(VkClearValue::calloc) {
            color()
                .float32(0, 0.2f)
                .float32(1, 0.2f)
                .float32(2, 0.2f)
                .float32(3, 1.0f)
        }

        val clearValueDepth = calloc(VkClearValue::calloc) {
            color()
                .float32(0, 0f)
                .float32(1, 0f)
                .float32(2, 0f)
                .float32(3, 0f)
        }

        val defaultColorAttachment = calloc(VkRenderingAttachmentInfoKHR::calloc, 1)
        defaultColorAttachment[0]
            .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR)
            .pNext(0)
            .imageLayout(VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR)
            .resolveMode(VK_RESOLVE_MODE_NONE)
            .resolveImageView(0)
            .resolveImageLayout(0)
            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            .clearValue(clearValueColor)
            .imageView(swapchainImageView)

        val defaultDepthAttachment = calloc(VkRenderingAttachmentInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR)
            pNext(0)
            imageLayout(VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR)
            resolveMode(VK_RESOLVE_MODE_NONE)
            resolveImageView(0)
            resolveImageLayout(0)
            loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            clearValue(clearValueDepth)
            imageView(depthAttachment.vkImageViewHandle)
        }

        val renderArea = calloc(VkRect2D::calloc) {
            extent().width(width).height(height)
        }

        val defaultRendering = calloc(VkRenderingInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_RENDERING_INFO_KHR)
            pNext(0)
            flags(0)
            renderArea(renderArea)
            layerCount(1)
            viewMask(0)
            pColorAttachments(defaultColorAttachment)
            pDepthAttachment(defaultDepthAttachment)
            pStencilAttachment(null)
        }

        vkBeginCommandBuffer(commandBuffer.vkHandle, cmdBeginInfo)

        val swapToRenderingBarrier = calloc(VkImageMemoryBarrier::calloc, 1)
        with(swapToRenderingBarrier[0]) {
            sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            pNext(0)
            srcAccessMask(0)
            dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            oldLayout(ImageLayout.UNDEFINED.vkValue)
            newLayout(ImageLayout.COLOR_ATTACHMENT_OPTIMAL.vkValue)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            image(swapchainImage)
            subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1)
        }

        vkCmdPipelineBarrier(
            commandBuffer.vkHandle,
            PipelineStage.TOP_OF_PIPE.vkBits, PipelineStage.COLOR_ATTACHMENT_OUTPUT.vkBits,
            0, null, null, swapToRenderingBarrier
        )

        val guiComponents = gui.getAllComponents().filterIsInstance<SpatialComponent>()
        updateGUIText(commandBuffer, guiComponents.filterIsInstance<TextComponent>())

        vkCmdBeginRenderingKHR(commandBuffer.vkHandle, defaultRendering)
        runMemorySafe {
            val viewport = calloc(VkViewport::calloc, 1)
            viewport[0].set(0f, 0f, width.toFloat(), height.toFloat(), 1f, 0f)

            val scissor = calloc(VkRect2D::calloc, 1)
            scissor[0].offset().x(0).y(0)
            scissor[0].extent().width(width).height(height)

            val pDescriptorSets = allocateLong(1)
            pDescriptorSets.put(0, descriptorSet.vkHandle)

            val pVertexBuffers = allocateLong(1)
            pVertexBuffers.put(0, vertexBuffer.vkBufferHandle)
            val pOffsets = allocateLong(1)
            pOffsets.put(0, 0L)

            val pPushConstants = allocate(128)

            val bindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS
            val indexCount = QuadModel.indices.size

            vkCmdSetViewport(commandBuffer.vkHandle, 0, viewport)
            vkCmdSetScissor(commandBuffer.vkHandle, 0, scissor)
            vkCmdBindDescriptorSets(commandBuffer.vkHandle, bindPoint, pipeline.vkLayoutHandle, 0, pDescriptorSets, null)
            vkCmdBindPipeline(commandBuffer.vkHandle, bindPoint, pipeline.vkHandle)
            vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
            vkCmdBindIndexBuffer(commandBuffer.vkHandle, indexBuffer.vkBufferHandle, 0L, VK_INDEX_TYPE_UINT32)

            guiComponents.forEach {
                it.localPosition.toByteBuffer(pPushConstants, 0)
                it.extent.toByteBuffer(pPushConstants, 8)
                pPushConstants.putInt(16, it.localPosition.z)
                pPushConstants.putInt(20, it.alignment.bits)
                if (it is TexturedComponent) {
                    pPushConstants.putInt(24, it.textureIndex)
                }
                vkCmdPushConstants(commandBuffer.vkHandle, pipeline.vkLayoutHandle, ShaderStage.BOTH.vkBits, 0, pPushConstants)
                vkCmdDrawIndexed(commandBuffer.vkHandle, indexCount, 1, 0, 0, 0)
            }
        }
        vkCmdEndRenderingKHR(commandBuffer.vkHandle)

        // Transition Swapchain Image Layouts:
        val swapToPresentBarrier = calloc(VkImageMemoryBarrier::calloc, 1)
        with(swapToPresentBarrier[0]) {
            sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            pNext(0)
            srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            dstAccessMask(0)
            oldLayout(ImageLayout.COLOR_ATTACHMENT_OPTIMAL.vkValue)
            newLayout(ImageLayout.PRESENT_SRC.vkValue)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            image(swapchainImage)
            subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1)
        }

        vkCmdPipelineBarrier(
            commandBuffer.vkHandle,
            PipelineStage.COLOR_ATTACHMENT_OUTPUT.vkBits, PipelineStage.BOTTOM_OF_PIPE.vkBits,
            0, null, null, swapToPresentBarrier
        )

        vkEndCommandBuffer(commandBuffer.vkHandle)

        return@runMemorySafe FrameSubmitData(preparation.acquireSuccessful, preparation.imageIndex)
    }

    private fun updateGUIText(cmdBuf: CommandBuffer, textComponents: List<TextComponent>) {
        textComponents.filter { it.textRequiresUpdate }.forEach {
            // Transition the component's texture to TRANSFER_DST first
            val targetImage = images[it.textureIndex]!!
            deviceUtil.cmdTransitionImageLayout(
                cmdBuf, targetImage,
                AccessMask.SHADER_READ, AccessMask.TRANSFER_WRITE,
                ImageLayout.SHADER_READ_ONLY_OPTIMAL, ImageLayout.TRANSFER_DST_OPTIMAL,
                PipelineStage.FRAGMENT_SHADER, PipelineStage.TRANSFER
            )

            runMemorySafe {
                val pClearColor = calloc(VkClearColorValue::calloc) {
                    this.float32(0, 0f)
                    this.float32(1, 0f)
                    this.float32(2, 0f)
                    this.float32(3, 0f)
                }

                val pRange = calloc(VkImageSubresourceRange::calloc) {
                    set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1)
                }

                vkCmdClearColorImage(
                    cmdBuf.vkHandle, targetImage.vkImageHandle,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    pClearColor, pRange
                )
            }

            if (it.text.isNotEmpty())
                blitCharacters(cmdBuf, it.text, targetImage)

            deviceUtil.cmdTransitionImageLayout(
                cmdBuf, targetImage,
                AccessMask.TRANSFER_WRITE, AccessMask.SHADER_READ,
                ImageLayout.TRANSFER_DST_OPTIMAL, ImageLayout.SHADER_READ_ONLY_OPTIMAL,
                PipelineStage.TRANSFER, PipelineStage.FRAGMENT_SHADER
            )

            it.textRequiresUpdate = false
        }
    }

    private fun blitCharacters(cmdBuf: CommandBuffer, text: String, dstImage: VulkanImage) = runMemorySafe {
        val glyphHeight = dstImage.config.extent.height
        val glyphWidth = (glyphHeight / glyphAtlas.glyphHeight) * glyphAtlas.glyphWidth
        val pRegions = calloc(VkImageBlit::calloc, text.length)
        val maxX = dstImage.config.extent.width

        repeat(text.length) { i ->
            val glyphBounds = glyphAtlas.getGlyphBounds(text[i])

            val dstMin = IVec2(min(i * glyphWidth, maxX), 0)
            val dstMax = IVec2(min(i * glyphWidth + glyphWidth, maxX), glyphHeight)

            pRegions[i].srcSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1)
            pRegions[i].dstSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1)
            pRegions[i].srcOffsets(0).x(glyphBounds.min.x).y(glyphBounds.min.y).z(0)
            pRegions[i].srcOffsets(1).x(glyphBounds.max.x).y(glyphBounds.max.y).z(1)
            pRegions[i].dstOffsets(0).x(dstMin.x).y(dstMin.y).z(0)
            pRegions[i].dstOffsets(1).x(dstMax.x).y(dstMax.y).z(1)
        }

        vkCmdBlitImage(
            cmdBuf.vkHandle,
            glyphImage.vkImageHandle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
            dstImage.vkImageHandle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            pRegions, VK_FILTER_NEAREST
        )
    }

    private fun handleInput() {}

    override fun onCharTyped(char: Char) {
        println(char)
        textRect.text += char
    }

    override fun onKeyPressed(key: Key) {
        if (key == Key.BACKSPACE)
            textRect.text = if (textRect.text.isBlank()) "" else textRect.text.substring(0, textRect.text.length - 1)
    }

    private fun writeScreenInfoBuffer(extent: IVec2) {
        val buf = ByteBuffer.allocate(64)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(0, extent.x)
        buf.putInt(Int.SIZE_BYTES, extent.y)

        screenInfoBuffer.put(0, buf)
    }

    override fun onResizeDestroy() {
        depthAttachment.destroy()
    }

    override fun onResizeRecreate(newExtent2D: ImageExtent2D) {
        createAttachmentImages()
        writeScreenInfoBuffer(IVec2(newExtent2D.width, newExtent2D.height))
    }

    override fun destroy() {
        device.waitIdle()
        glyphImage.destroy()
        vertexBuffer.destroy()
        indexBuffer.destroy()
        screenInfoBuffer.destroy()
        depthAttachment.destroy()
        images.forEach { it.value.destroy() }
        sampler.destroy(device)
        descriptorPool.destroy(device)
        descriptorSetLayout.destroy(device)
        pipeline.destroy(device)
        super.destroy()
    }
}