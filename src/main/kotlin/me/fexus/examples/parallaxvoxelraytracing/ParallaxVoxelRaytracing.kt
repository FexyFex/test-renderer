package me.fexus.examples.parallaxvoxelraytracing

import me.fexus.camera.CameraPerspective
import me.fexus.math.mat.Mat4
import me.fexus.math.repeatCubed
import me.fexus.math.vec.Vec3
import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
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
import me.fexus.vulkan.extension.ExtendedDynamicState3Extension
import me.fexus.vulkan.util.ImageExtent2D
import me.fexus.vulkan.util.ImageExtent3D
import me.fexus.window.Window
import me.fexus.window.input.InputHandler
import me.fexus.window.input.Key
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTExtendedDynamicState3.vkCmdSetPolygonModeEXT
import org.lwjgl.vulkan.KHRDynamicRendering.*
import org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR
import org.lwjgl.vulkan.VK12.*
import java.nio.ByteBuffer
import java.nio.ByteOrder



class ParallaxVoxelRaytracing: VulkanRendererBase(createWindow()) {
    companion object {
        private const val EXTENT = 8

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
    private lateinit var vertexBufferWireFrame: VulkanBuffer
    private lateinit var cameraBuffer: VulkanBuffer
    private lateinit var blockBuffer: VulkanBuffer
    private val descriptorPool = DescriptorPool()
    private val descriptorSetLayout = DescriptorSetLayout()
    private val descriptorSet = DescriptorSet()
    private val pipeline = GraphicsPipeline()
    private val pipelineWireframe = GraphicsPipeline()

    private val planePosition = Vec3(3f, 3f, 7.4999f)
    private val modelMatrix = Mat4(1f).translate(planePosition).scale(Vec3(4f))

    private val inputHandler = InputHandler(window)


    fun start() {
        initVulkanCore()
        initObjects()
        startRenderLoop(window, this)
    }

    private fun initObjects() {
        camera.position = Vec3(0f, 0f, -3f)

        val depthAttachmentImageLayout = VulkanImageLayout(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D,
            ImageExtent3D(swapchain.imageExtent, 1),
            1, 1, 1,
            ImageColorFormat.D32_SFLOAT, ImageTiling.OPTIMAL,
            ImageAspect.DEPTH, ImageUsage.DEPTH_STENCIL_ATTACHMENT,
            MemoryProperty.DEVICE_LOCAL
        )
        this.depthAttachment = imageFactory.createImage(depthAttachmentImageLayout)

        // -- VERTEX BUFFERS --
        val vertexBufferData = ByteBuffer.allocate(QuadModel.SIZE_BYTES)
        vertexBufferData.order(ByteOrder.LITTLE_ENDIAN)
        QuadModel.vertices.forEachIndexed { index, fl ->
            val offset = index * Float.SIZE_BYTES
            vertexBufferData.putFloat(offset, fl)
        }
        val vertexBufferLayout = VulkanBufferLayout(
            QuadModel.SIZE_BYTES.toLong(),
            MemoryProperty.DEVICE_LOCAL, BufferUsage.VERTEX_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.vertexBuffer = bufferFactory.createBuffer(vertexBufferLayout)
        // Staging
        val stagingVertexBufferLayout = VulkanBufferLayout(
            QuadModel.SIZE_BYTES.toLong(),
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
                .size(QuadModel.SIZE_BYTES.toLong())
            vkCmdCopyBuffer(cmdBuf.vkHandle, stagingVertexBuffer.vkBufferHandle, vertexBuffer.vkBufferHandle, copyRegion)

            endSingleTimeCommandBuffer(cmdBuf)
        }
        stagingVertexBuffer.destroy()

        val wireFrameBuffoon = ByteBuffer.allocate(QuadWireFrame.SIZE_BYTES)
        wireFrameBuffoon.order(ByteOrder.LITTLE_ENDIAN)
        QuadWireFrame.vertices.forEachIndexed { index, fl ->
            val offset = index * Float.SIZE_BYTES
            wireFrameBuffoon.putFloat(offset, fl)
        }
        val wireFrameBufLayout = VulkanBufferLayout(
            QuadWireFrame.SIZE_BYTES.toLong(), MemoryProperty.DEVICE_LOCAL,
            BufferUsage.VERTEX_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.vertexBufferWireFrame = bufferFactory.createBuffer(wireFrameBufLayout)
        val stagingBufWireFrameLayout = VulkanBufferLayout(
            QuadWireFrame.SIZE_BYTES.toLong(), MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.TRANSFER_SRC
        )
        val stagingBufWireFrame = bufferFactory.createBuffer(stagingBufWireFrameLayout)
        stagingBufWireFrame.put(device, wireFrameBuffoon, 0)

        runMemorySafe {
            val cmdBuf = beginSingleTimeCommandBuffer()

            val copyRegion = calloc<VkBufferCopy, VkBufferCopy.Buffer>(1)
            copyRegion[0]
                .srcOffset(0)
                .dstOffset(0)
                .size(QuadWireFrame.SIZE_BYTES.toLong())
            vkCmdCopyBuffer(cmdBuf.vkHandle, stagingBufWireFrame.vkBufferHandle, vertexBufferWireFrame.vkBufferHandle, copyRegion)

            endSingleTimeCommandBuffer(cmdBuf)
        }
        stagingBufWireFrame.destroy()
        // -- VERTEX BUFFERS --

        // -- CAMERA BUFFER --
        val cameraBufferLayout = VulkanBufferLayout(
            128L, MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT, BufferUsage.UNIFORM_BUFFER
        )
        this.cameraBuffer = bufferFactory.createBuffer(cameraBufferLayout)
        // -- CAMERA BUFFER --

        // -- BLOCK BUFFER --
        val blockBufferSize = EXTENT * EXTENT * EXTENT * Int.SIZE_BYTES
        val blockBufferLayout = VulkanBufferLayout(
            blockBufferSize.toLong(),
            MemoryProperty.HOST_COHERENT + MemoryProperty.HOST_VISIBLE,
            BufferUsage.STORAGE_BUFFER
        )
        this.blockBuffer = bufferFactory.createBuffer(blockBufferLayout)
        val blockBuffoon = ByteBuffer.allocate(blockBufferSize)
        blockBuffoon.order(ByteOrder.LITTLE_ENDIAN)
        repeatCubed(EXTENT) { x, y, z ->
            val offset = ((z * EXTENT * EXTENT) + (y * EXTENT) + x) * Int.SIZE_BYTES
            val block = if (x == 0 && y == 0 && z == 0) 1 else
                if (Math.random() < 0.3) 1 else 0
            blockBuffoon.putInt(offset, block)
        }
        this.blockBuffer.put(device, blockBuffoon, 0)
        // -- BLOCK BUFFER --

        // Descriptor Sets and Pipeline
        val poolPlan = DescriptorPoolPlan(
            FRAMES_TOTAL, DescriptorPoolCreateFlag.FREE_DESCRIPTOR_SET,
            listOf(
                DescriptorPoolSize(DescriptorType.UNIFORM_BUFFER, 1),
                DescriptorPoolSize(DescriptorType.STORAGE_BUFFER, 1)
            )
        )
        this.descriptorPool.create(device, poolPlan)

        val setLayoutPlan = DescriptorSetLayoutPlan(
            DescriptorSetLayoutCreateFlag.NONE,
            listOf(
                DescriptorSetLayoutBinding(0, 1, DescriptorType.UNIFORM_BUFFER, ShaderStage.BOTH, DescriptorSetLayoutBindingFlag.NONE),
                DescriptorSetLayoutBinding(1, 1, DescriptorType.STORAGE_BUFFER, ShaderStage.BOTH, DescriptorSetLayoutBindingFlag.NONE),
            )
        )
        this.descriptorSetLayout.create(device, setLayoutPlan)

        this.descriptorSet.create(device, descriptorPool, descriptorSetLayout)

        // Pipelines
        val pipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC4, 0),
                VertexAttribute(1, VertexAttributeFormat.VEC4, 16),
                VertexAttribute(2, VertexAttributeFormat.VEC4, 32),
                VertexAttribute(3, VertexAttributeFormat.VEC4, 48),
                VertexAttribute(4, VertexAttributeFormat.VEC4, 64)
            ),
            PushConstantsLayout(128),
            ClassLoader.getSystemResource("shaders/parallaxvoxelraytracing/vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/parallaxvoxelraytracing/frag.spv").readBytes(),
            listOf(DynamicState.VIEWPORT, DynamicState.SCISSOR),
            blendEnable = true, primitive = Primitive.TRIANGLES
        )
        this.pipeline.create(device, descriptorSetLayout, pipelineConfig)

        val pipelineWireframeConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC4, 0),
                VertexAttribute(1, VertexAttributeFormat.VEC4, 16),
                VertexAttribute(2, VertexAttributeFormat.VEC4, 32),
                VertexAttribute(3, VertexAttributeFormat.VEC4, 48),
                VertexAttribute(4, VertexAttributeFormat.VEC4, 64)
            ),
            PushConstantsLayout(128),
            ClassLoader.getSystemResource("shaders/parallaxvoxelraytracing/vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/parallaxvoxelraytracing/frag.spv").readBytes(),
            listOf(DynamicState.VIEWPORT, DynamicState.SCISSOR),
            blendEnable = true, primitive = Primitive.LINES
        )
        this.pipelineWireframe.create(device, descriptorSetLayout, pipelineWireframeConfig)

        // Update Descrfiptor Set
        val descWriteCameraBuf = DescriptorBufferWrite(
            0, DescriptorType.UNIFORM_BUFFER, 1, this.descriptorSet, 0,
            listOf(DescriptorBufferInfo(cameraBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE))
        )
        val descWriteBlockBuf = DescriptorBufferWrite(
            1, DescriptorType.STORAGE_BUFFER, 1, this.descriptorSet, 0,
            listOf(DescriptorBufferInfo(blockBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE))
        )

        this.descriptorSet.update(device, descWriteCameraBuf, descWriteBlockBuf)
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
            (-camera.position).toByteBuffer(pPushConstants, 64)
            pPushConstants.putInt(80, 0)

            val bindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS

            // parallax voxel rendering
            vkCmdSetViewport(commandBuffer.vkHandle, 0, viewport)
            vkCmdSetScissor(commandBuffer.vkHandle, 0, scissor)
            vkCmdBindDescriptorSets(commandBuffer.vkHandle, bindPoint, pipeline.vkLayoutHandle, 0, pDescriptorSets, null)
            vkCmdBindPipeline(commandBuffer.vkHandle, bindPoint, pipeline.vkHandle)
            vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
            vkCmdPushConstants(commandBuffer.vkHandle, pipeline.vkLayoutHandle, ShaderStage.BOTH.vkBits, 0, pPushConstants)
            vkCmdDraw(commandBuffer.vkHandle, 6, 1, 0, 0)

            // outline rendering
            pPushConstants.putInt(80, 1)
            pVertexBuffers.put(0, vertexBufferWireFrame.vkBufferHandle)

            vkCmdBindPipeline(commandBuffer.vkHandle, bindPoint, pipelineWireframe.vkHandle)
            vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
            vkCmdPushConstants(commandBuffer.vkHandle, pipeline.vkLayoutHandle, ShaderStage.BOTH.vkBits, 0, pPushConstants)
            vkCmdDraw(commandBuffer.vkHandle, 8, 1, 0, 0)
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
        vertexBuffer.destroy()
        vertexBufferWireFrame.destroy()
        cameraBuffer.destroy()
        blockBuffer.destroy()
        depthAttachment.destroy()
        descriptorPool.destroy(device)
        descriptorSetLayout.destroy(device)
        pipeline.destroy(device)
        pipelineWireframe.destroy(device)
        super.destroy()
    }
}