package me.fexus.examples.parallaxvoxelraytracing

import me.fexus.camera.CameraPerspective
import me.fexus.math.mat.Mat4
import me.fexus.math.vec.Vec3
import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.texture.TextureLoader
import me.fexus.vulkan.util.FramePreparation
import me.fexus.vulkan.util.FrameSubmitData
import me.fexus.vulkan.VulkanRendererBase
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
import me.fexus.vulkan.descriptors.buffer.VulkanBufferLayout
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.image.*
import me.fexus.vulkan.descriptors.image.aspect.ImageAspect
import me.fexus.vulkan.descriptors.image.usage.ImageUsage
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperty
import me.fexus.vulkan.component.pipeline.stage.PipelineStage
import me.fexus.vulkan.descriptors.image.sampler.AddressMode
import me.fexus.vulkan.descriptors.image.sampler.Filtering
import me.fexus.vulkan.descriptors.image.sampler.VulkanSampler
import me.fexus.vulkan.descriptors.image.sampler.VulkanSamplerLayout
import me.fexus.vulkan.util.ImageExtent2D
import me.fexus.vulkan.util.ImageExtent3D
import me.fexus.window.Window
import me.fexus.window.input.InputHandler
import me.fexus.window.input.Key
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRDynamicRendering.*
import org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR
import org.lwjgl.vulkan.VK12.*
import java.nio.ByteBuffer
import java.nio.ByteOrder


class ParallaxVoxelRaytracing: VulkanRendererBase(createWindow()) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ParallaxVoxelRaytracing().start()
        }

        private fun createWindow() = Window("Parallax Mapping") {
            windowVisible()
            enableResizable()
            setInitialWindowSize(1067,600)
            enableDecoration()
            setInitialWindowPosition(400, 300)
            enableAutoIconify()
        }

        private fun Boolean.toInt(): Int = if (this) 1 else 0
    }

    private val camera = CameraPerspective(window.aspect)

    private lateinit var depthAttachment: VulkanImage
    private lateinit var vertexBuffer: VulkanBuffer
    private lateinit var cameraBuffer: VulkanBuffer
    private lateinit var blockBuffer: VulkanBuffer
    private lateinit var imageArray: VulkanImage
    private lateinit var sampler: VulkanSampler
    private val descriptorPool = DescriptorPool()
    private val descriptorSetLayout = DescriptorSetLayout()
    private val descriptorSet = DescriptorSet()
    private val pipeline = GraphicsPipeline()

    private val planePosition = Vec3(0f, 0f, 0f)
    private val modelMatrix = Mat4(1f).translate(planePosition)

    private val inputHandler = InputHandler(window)

    private var heightScale: Float = 0.5f


    fun start() {
        initVulkanCore()
        initObjects()
        startRenderLoop(window, this)
    }

    private fun initObjects() {
        val depthAttachmentImageLayout = VulkanImageLayout(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D,
            ImageExtent3D(swapchain.imageExtent, 1),
            1, 1, 1,
            ImageColorFormat.D32_SFLOAT, ImageTiling.OPTIMAL,
            ImageAspect.DEPTH, ImageUsage.DEPTH_STENCIL_ATTACHMENT,
            MemoryProperty.DEVICE_LOCAL
        )
        this.depthAttachment = imageFactory.createImage(depthAttachmentImageLayout)

        // -- VERTEX BUFFER --
        val vertexBufferData = ByteBuffer.allocate(ParallaxMappingQuadModel.SIZE_BYTES)
        vertexBufferData.order(ByteOrder.LITTLE_ENDIAN)
        ParallaxMappingQuadModel.vertices.forEachIndexed { index, fl ->
            val offset = index * Float.SIZE_BYTES
            vertexBufferData.putFloat(offset, fl)
        }
        val vertexBufferLayout = VulkanBufferLayout(
            ParallaxMappingQuadModel.SIZE_BYTES.toLong(),
            MemoryProperty.DEVICE_LOCAL, BufferUsage.VERTEX_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.vertexBuffer = bufferFactory.createBuffer(vertexBufferLayout)
        // Staging
        val stagingVertexBufferLayout = VulkanBufferLayout(
            ParallaxMappingQuadModel.SIZE_BYTES.toLong(),
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.TRANSFER_SRC
        )
        val stagingVertexBuffer = bufferFactory.createBuffer(stagingVertexBufferLayout)
        stagingVertexBuffer.put(device, vertexBufferData, 0)
        // Copy from Staging to Vertex Buffer
        runMemorySafe {
            val cmdBuf = beginSingleTimeCommandBuffer()

            val copyRegion = calloc<VkBufferCopy, VkBufferCopy.Buffer>(1)
            copyRegion[0]
                .srcOffset(0)
                .dstOffset(0)
                .size(ParallaxMappingQuadModel.SIZE_BYTES.toLong())
            vkCmdCopyBuffer(cmdBuf.vkHandle, stagingVertexBuffer.vkBufferHandle, vertexBuffer.vkBufferHandle, copyRegion)

            endSingleTimeCommandBuffer(cmdBuf)
        }
        stagingVertexBuffer.destroy()
        // -- VERTEX BUFFER --

        // -- CAMERA BUFFER --
        val cameraBufferLayout = VulkanBufferLayout(
            128L, MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT, BufferUsage.UNIFORM_BUFFER
        )
        this.cameraBuffer = bufferFactory.createBuffer(cameraBufferLayout)
        // -- CAMERA BUFFER --

        // -- BLOCK BUFFER --
        val blockBufferLayout = VulkanBufferLayout(
            4 * 4 * 4L,
            MemoryProperty.HOST_COHERENT + MemoryProperty.HOST_VISIBLE,
            BufferUsage.STORAGE_BUFFER
        )
        this.blockBuffer = bufferFactory.createBuffer(blockBufferLayout)
        // -- BLOCK BUFFER --

        // -- IMAGES --
        val diffTexture = TextureLoader("textures/parallaxmapping/diffuse.jpg")
        val dispTexture = TextureLoader("textures/parallaxmapping/displacement.png")
        val normTexture = TextureLoader("textures/parallaxmapping/normal.jpg")
        val imageArrayLayout = VulkanImageLayout(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D_ARRAY, ImageExtent3D(diffTexture.width, diffTexture.height, 1),
            1, 1, 3, ImageColorFormat.R8G8B8A8_SRGB, ImageTiling.OPTIMAL,
            ImageAspect.COLOR, ImageUsage.SAMPLED + ImageUsage.TRANSFER_DST, MemoryProperty.DEVICE_LOCAL
        )
        this.imageArray = imageFactory.createImage(imageArrayLayout)

        val stagingImageBufferLayout = VulkanBufferLayout(
            diffTexture.imageSize * 3,
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.TRANSFER_SRC
        )
        val imgStagingBuf = bufferFactory.createBuffer(stagingImageBufferLayout)
        imgStagingBuf.put(device, diffTexture.pixels, 0)
        imgStagingBuf.put(device, dispTexture.pixels, diffTexture.imageSize.toInt())
        imgStagingBuf.put(device, normTexture.pixels, dispTexture.imageSize.toInt() * 2)
        runMemorySafe {
            val cmdBuf = beginSingleTimeCommandBuffer()

            val subResourceRange = calloc<VkImageSubresourceRange>() {
                aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                baseMipLevel(0)
                levelCount(1)
                baseArrayLayer(0)
                layerCount(3)
            }
            val imageBarrier = calloc<VkImageMemoryBarrier, VkImageMemoryBarrier.Buffer>(1)
            imageBarrier[0]
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .pNext(0)
                .image(this@ParallaxVoxelRaytracing.imageArray.vkImageHandle)
                .srcAccessMask(0)
                .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                .subresourceRange(subResourceRange)

            vkCmdPipelineBarrier(
                cmdBuf.vkHandle, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
                null, null, imageBarrier
            )

            val copyRegions = calloc<VkBufferImageCopy, VkBufferImageCopy.Buffer>(3)
            copyRegions[0].bufferOffset(0L)
            copyRegions[0].imageExtent().width(diffTexture.width).height(diffTexture.height).depth(1)
            copyRegions[0].imageOffset().x(0).y(0).z(0)
            copyRegions[0].imageSubresource()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .mipLevel(0)
                .baseArrayLayer(0)
                .layerCount(1)

            copyRegions[1].bufferOffset(diffTexture.imageSize)
            copyRegions[1].imageExtent().width(diffTexture.width).height(diffTexture.height).depth(1)
            copyRegions[1].imageOffset().x(0).y(0).z(0)
            copyRegions[1].imageSubresource()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .mipLevel(0)
                .baseArrayLayer(1)
                .layerCount(1)

            copyRegions[2].bufferOffset(diffTexture.imageSize)
            copyRegions[2].imageExtent().width(diffTexture.width).height(diffTexture.height).depth(1)
            copyRegions[2].imageOffset().x(0).y(0).z(0)
            copyRegions[2].imageSubresource()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .mipLevel(0)
                .baseArrayLayer(2)
                .layerCount(1)

            vkCmdCopyBufferToImage(
                cmdBuf.vkHandle,
                imgStagingBuf.vkBufferHandle, this@ParallaxVoxelRaytracing.imageArray.vkImageHandle,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copyRegions
            )

            imageBarrier[0]
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

            vkCmdPipelineBarrier(cmdBuf.vkHandle,
                VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
                null, null, imageBarrier
            )

            endSingleTimeCommandBuffer(cmdBuf)
        }
        imgStagingBuf.destroy()
        // -- IMAGES --

        // -- SAMPLER --
        val samplerLayout = VulkanSamplerLayout(AddressMode.REPEAT, 1, Filtering.LINEAR)
        this.sampler = imageFactory.createSampler(samplerLayout)
        // -- SAMPLER --

        // Descriptor Sets and Pipeline
        val poolPlan = DescriptorPoolPlan(
            FRAMES_TOTAL, DescriptorPoolCreateFlag.FREE_DESCRIPTOR_SET,
            listOf(
                DescriptorPoolSize(DescriptorType.UNIFORM_BUFFER, 1),
                DescriptorPoolSize(DescriptorType.STORAGE_BUFFER, 1),
                DescriptorPoolSize(DescriptorType.SAMPLED_IMAGE, 1),
                DescriptorPoolSize(DescriptorType.SAMPLER, 1)
            )
        )
        this.descriptorPool.create(device, poolPlan)

        val setLayoutPlan = DescriptorSetLayoutPlan(
            DescriptorSetLayoutCreateFlag.NONE,
            listOf(
                DescriptorSetLayoutBinding(0, 1, DescriptorType.UNIFORM_BUFFER, ShaderStage.BOTH, DescriptorSetLayoutBindingFlag.NONE),
                DescriptorSetLayoutBinding(1, 1, DescriptorType.STORAGE_BUFFER, ShaderStage.BOTH, DescriptorSetLayoutBindingFlag.NONE),
                DescriptorSetLayoutBinding(2, 1, DescriptorType.SAMPLED_IMAGE, ShaderStage.BOTH, DescriptorSetLayoutBindingFlag.NONE),
                DescriptorSetLayoutBinding(3, 1, DescriptorType.SAMPLER, ShaderStage.BOTH, DescriptorSetLayoutBindingFlag.NONE)
            )
        )
        this.descriptorSetLayout.create(device, setLayoutPlan)

        this.descriptorSet.create(device, descriptorPool, descriptorSetLayout)

        val pipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC4, 0),
                VertexAttribute(1, VertexAttributeFormat.VEC4, 16),
                VertexAttribute(2, VertexAttributeFormat.VEC4, 32),
                VertexAttribute(3, VertexAttributeFormat.VEC4, 48),
                VertexAttribute(4, VertexAttributeFormat.VEC4, 64)
            ),
            PushConstantsLayout(128),
            ClassLoader.getSystemResource("shaders/parallaxmapping/vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/parallaxmapping/frag.spv").readBytes(),
            blendEnable = true
        )
        this.pipeline.create(device, descriptorSetLayout, pipelineConfig)

        // Update Descrfiptor Set
        val descWriteCameraBuf = DescriptorBufferWrite(
            0, DescriptorType.UNIFORM_BUFFER, 1, this.descriptorSet, 0,
            listOf(DescriptorBufferInfo(cameraBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE))
        )
        val descWriteBlockBuf = DescriptorBufferWrite(
            1, DescriptorType.STORAGE_BUFFER, 1, this.descriptorSet, 0,
            listOf(DescriptorBufferInfo(blockBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE))
        )
        val descWriteTextures = DescriptorImageWrite(
            2, DescriptorType.SAMPLED_IMAGE, 1, this.descriptorSet, 0,
            listOf(
                DescriptorImageInfo(0L, imageArray.vkImageViewHandle, ImageLayout.SHADER_READ_ONLY_OPTIMAL),
            )
        )
        val descWriteSampler = DescriptorImageWrite(
            3, DescriptorType.SAMPLER, 1, this.descriptorSet, 0,
            listOf(DescriptorImageInfo(this.sampler.vkHandle, 0L, ImageLayout.SHADER_READ_ONLY_OPTIMAL))
        )

        this.descriptorSet.update(device, descWriteCameraBuf, descWriteBlockBuf, descWriteTextures, descWriteSampler)
    }

    override fun recordFrame(preparation: FramePreparation): FrameSubmitData = runMemorySafe {
        handleInput()

        val view = camera.calculateView()
        val proj = camera.calculateReverseZProjection()
        val data = ByteBuffer.allocate(128)
        data.order(ByteOrder.LITTLE_ENDIAN)
        view.toByteBuffer(data, 0)
        proj.toByteBuffer(data, 64)
        cameraBuffer.put(device, data, 0)

        val width: Int = swapchain.imageExtent.width
        val height: Int = swapchain.imageExtent.height

        val cmdBeginInfo = calloc<VkCommandBufferBeginInfo>() {
            sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            pNext(0)
            flags(0)
            pInheritanceInfo(null)
        }

        val commandBuffer = commandBuffers[currentFrameInFlight]
        val swapchainImage = swapchain.images[preparation.imageIndex]
        val swapchainImageView = swapchain.imageViews[preparation.imageIndex]

        val clearValueColor = calloc<VkClearValue> {
            color()
                .float32(0, 0.5f)
                .float32(1, 0.2f)
                .float32(2, 0.6f)
                .float32(3, 1.0f)
        }

        val clearValueDepth = calloc<VkClearValue> {
            color()
                .float32(0, 0f)
                .float32(1, 0f)
                .float32(2, 0f)
                .float32(3, 0f)
        }

        val defaultColorAttachment = calloc<VkRenderingAttachmentInfoKHR, VkRenderingAttachmentInfoKHR.Buffer>(1)
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

        val defaultDepthAttachment = calloc<VkRenderingAttachmentInfoKHR>() {
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

        val renderArea = calloc<VkRect2D>() {
            extent().width(width).height(height)
        }

        val defaultRendering = calloc<VkRenderingInfoKHR>() {
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

        val swapToRenderingBarrier = calloc<VkImageMemoryBarrier, VkImageMemoryBarrier.Buffer>(1)
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

        vkCmdBeginRenderingKHR(commandBuffer.vkHandle, defaultRendering)
        runMemorySafe {
            val viewport = calloc<VkViewport, VkViewport.Buffer>(1)
            viewport[0].set(0f, 0f, width.toFloat(), height.toFloat(), 1f, 0f)

            val scissor = calloc<VkRect2D, VkRect2D.Buffer>(1)
            scissor[0].offset().x(0).y(0)
            scissor[0].extent().width(width).height(height)

            val pDescriptorSets = allocateLong(1)
            pDescriptorSets.put(0, descriptorSet.vkHandle)

            val pVertexBuffers = allocateLong(1)
            pVertexBuffers.put(0, vertexBuffer.vkBufferHandle)
            val pOffsets = allocateLong(1)
            pOffsets.put(0, 0L)

            val pPushConstants = allocate(128)
            modelMatrix.toByteBuffer(pPushConstants, 0)
            Vec3(5f, 1.0f, 1f).toByteBuffer(pPushConstants, 64)
            (-camera.position).toByteBuffer(pPushConstants, 80)
            pPushConstants.putFloat(96, heightScale)

            val bindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS

            vkCmdSetViewport(commandBuffer.vkHandle, 0, viewport)
            vkCmdSetScissor(commandBuffer.vkHandle, 0, scissor)
            vkCmdBindDescriptorSets(commandBuffer.vkHandle, bindPoint, pipeline.vkLayoutHandle, 0, pDescriptorSets, null)
            vkCmdBindPipeline(commandBuffer.vkHandle, bindPoint, pipeline.vkHandle)
            vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
            vkCmdPushConstants(commandBuffer.vkHandle, pipeline.vkLayoutHandle, ShaderStage.BOTH.vkBits, 0, pPushConstants)
            vkCmdDraw(commandBuffer.vkHandle, 6, 1, 0, 0)
        }
        vkCmdEndRenderingKHR(commandBuffer.vkHandle)

        // Transition Swapchain Image Layouts:
        val swapToPresentBarrier = calloc<VkImageMemoryBarrier, VkImageMemoryBarrier.Buffer>(1)
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

    private fun handleInput() {
        val rotY = inputHandler.isKeyDown(Key.ARROW_RIGHT).toInt() - inputHandler.isKeyDown(Key.ARROW_LEFT).toInt()
        val rotX = inputHandler.isKeyDown(Key.ARROW_DOWN).toInt() - inputHandler.isKeyDown(Key.ARROW_UP).toInt()
        camera.rotation.x += rotX.toFloat() * 1.2f
        camera.rotation.y += rotY.toFloat() * 1.2f

        val xMove = inputHandler.isKeyDown(Key.A).toInt() - inputHandler.isKeyDown(Key.D).toInt()
        val yMove = inputHandler.isKeyDown(Key.LSHIFT).toInt() - inputHandler.isKeyDown(Key.SPACE).toInt()
        val zMove = inputHandler.isKeyDown(Key.W).toInt() - inputHandler.isKeyDown(Key.S).toInt()

        camera.position.x += xMove * 0.1f
        camera.position.y += yMove * 0.1f
        camera.position.z += zMove * 0.1f

        val heightScaleChange = inputHandler.isKeyDown(Key.O).toInt() - inputHandler.isKeyDown(Key.P).toInt()
        this.heightScale += heightScaleChange * 0.05f
    }

    override fun onResizeDestroy() {
        depthAttachment.destroy()
    }

    override fun onResizeRecreate(newExtent2D: ImageExtent2D) {
        val depthAttachmentImageLayout = VulkanImageLayout(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D,
            ImageExtent3D(newExtent2D, 1),
            1, 1, 1,
            ImageColorFormat.D32_SFLOAT, ImageTiling.OPTIMAL,
            ImageAspect.DEPTH, ImageUsage.DEPTH_STENCIL_ATTACHMENT,
            MemoryProperty.DEVICE_LOCAL
        )
        depthAttachment = imageFactory.createImage(depthAttachmentImageLayout)
    }

    override fun destroy() {
        device.waitIdle()
        sampler.destroy(device)
        imageArray.destroy()
        vertexBuffer.destroy()
        cameraBuffer.destroy()
        blockBuffer.destroy()
        depthAttachment.destroy()
        descriptorPool.destroy(device)
        descriptorSetLayout.destroy(device)
        pipeline.destroy(device)
        super.destroy()
    }
}