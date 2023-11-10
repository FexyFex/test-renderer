package me.fexus.examples.instancedrendering

import me.fexus.camera.CameraPerspective
import me.fexus.examples.hardwarevoxelraytracing.voxelanimation.model.AnimatedBlobModel
import me.fexus.math.mat.Mat4
import me.fexus.math.vec.Vec3
import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.model.CubeModelPositionsOnly
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
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.image.*
import me.fexus.vulkan.descriptors.image.aspect.ImageAspect
import me.fexus.vulkan.descriptors.image.usage.ImageUsage
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperty
import me.fexus.vulkan.component.pipeline.pipelinestage.PipelineStage
import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage
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


class InstancedRendering: VulkanRendererBase(createWindow()) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            InstancedRendering().start()
        }

        private fun createWindow() = Window("Instanced Rendering") {
            windowVisible()
            enableResizable()
            setInitialWindowSize(1067,600)
            enableDecoration()
            setInitialWindowPosition(400, 300)
            enableAutoIconify()
        }

        private fun Boolean.toInt(): Int = if (this) 1 else 0
    }

    private val inputHandler = InputHandler(window)

    private val camera = CameraPerspective(window.aspect)

    private val voxelModel = AnimatedBlobModel()

    private lateinit var depthAttachment: VulkanImage
    private lateinit var vertexBuffer: VulkanBuffer
    private lateinit var indexBuffer: VulkanBuffer
    private lateinit var wireframeIndexBuffer: VulkanBuffer
    private lateinit var cameraBuffer: VulkanBuffer
    private lateinit var instanceDataBuffer: VulkanBuffer
    private val descriptorPool = DescriptorPool()
    private val descriptorSetLayout = DescriptorSetLayout()
    private val descriptorSet = DescriptorSet()
    private val pipeline = GraphicsPipeline()
    private val wireframePipeline = GraphicsPipeline()



    fun start() {
        initVulkanCore()
        initObjects()
        startRenderLoop(window, this)
    }

    private fun initObjects() {
        camera.position = Vec3(-4f, -4f, -16f)

        createAttachmentImages()
        createMeshBuffers()

        // -- CAMERA BUFFER --
        val cameraBufferConfig = VulkanBufferConfiguration(
            128L,
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.UNIFORM_BUFFER
        )
        this.cameraBuffer = bufferFactory.createBuffer(cameraBufferConfig)
        // -- CAMERA BUFFER --

        // -- INSTANCE DATA BUFFER --
        val instanceBufferSize = voxelModel.voxelGrid.voxelCount * VoxelData.SIZE_BYTES
        val instanceBufferConfig = VulkanBufferConfiguration(
            instanceBufferSize.toLong(),
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.STORAGE_BUFFER
        )
        this.instanceDataBuffer = deviceUtil.createBuffer(instanceBufferConfig)
        voxelModel.updateModel()
        updateInstanceDataBuffer()
        // -- INSTANCE DATA BUFFER --

        // Descriptor Sets and Pipeline
        val poolPlan = DescriptorPoolPlan(
            FRAMES_TOTAL, DescriptorPoolCreateFlag.FREE_DESCRIPTOR_SET,
            listOf(
                DescriptorPoolSize(DescriptorType.UNIFORM_BUFFER, 4),
                DescriptorPoolSize(DescriptorType.STORAGE_BUFFER, 4)
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

        val pipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC4, 0),
            ),
            PushConstantsLayout(128),
            ClassLoader.getSystemResource("shaders/instancedrendering/standard_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/instancedrendering/standard_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.VIEWPORT, DynamicState.SCISSOR),
            blendEnable = false, cullMode = CullMode.BACKFACE
        )
        this.pipeline.create(device, descriptorSetLayout, pipelineConfig)

        val wireframePipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC4, 0),
            ),
            PushConstantsLayout(128),
            ClassLoader.getSystemResource("shaders/instancedrendering/wireframe_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/instancedrendering/standard_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.VIEWPORT, DynamicState.SCISSOR),
            blendEnable = false, primitive = Primitive.LINES
        )
        this.wireframePipeline.create(device, descriptorSetLayout, wireframePipelineConfig)

        // Update Descrfiptor Set
        val descWriteCameraBuf = DescriptorBufferWrite(
            0, DescriptorType.UNIFORM_BUFFER, 1, this.descriptorSet, 0,
            listOf(DescriptorBufferInfo(cameraBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE))
        )

        val descWriteInstanceBuf = DescriptorBufferWrite(
            1, DescriptorType.STORAGE_BUFFER, 1, this.descriptorSet, 0,
            listOf(DescriptorBufferInfo(instanceDataBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE))
        )

        this.descriptorSet.update(device, descWriteCameraBuf, descWriteInstanceBuf)
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
        val vertexBufferSize = CubeModelPositionsOnly.Vertex.SIZE_BYTES * CubeModelPositionsOnly.vertices.size
        val vertexBufferConfig = VulkanBufferConfiguration(
            vertexBufferSize.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.VERTEX_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.vertexBuffer = bufferFactory.createBuffer(vertexBufferConfig)

        val vertexByteBuffer = ByteBuffer.allocate(vertexBufferSize)
        vertexByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        CubeModelPositionsOnly.vertices.forEachIndexed { index, vertex ->
            val offset = index * CubeModelPositionsOnly.Vertex.SIZE_BYTES
            vertex.writeToByteBuffer(vertexByteBuffer, offset)
        }

        deviceUtil.stagingCopy(vertexByteBuffer, vertexBuffer, 0L, 0L, vertexBufferSize.toLong())
        // VERETX BUFFER

        // INDEX BUFFER
        val indexBufferSize = CubeModelPositionsOnly.indices.size * Int.SIZE_BYTES
        val indexBufferConfig = VulkanBufferConfiguration(
            indexBufferSize.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.INDEX_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.indexBuffer = bufferFactory.createBuffer(indexBufferConfig)

        val indexByteBuffer = ByteBuffer.allocate(indexBufferSize)
        indexByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        CubeModelPositionsOnly.indices.forEachIndexed { index, cubeIndex ->
            val offset = index * Int.SIZE_BYTES
            indexByteBuffer.putInt(offset, cubeIndex)
        }

        deviceUtil.stagingCopy(indexByteBuffer, indexBuffer, 0L, 0L, indexBufferSize.toLong())
        // INDEX BUFFER

        // WIREFRAME INDEX BUFFER
        val wireframeIndexBufferSize = CubeModelPositionsOnly.wireframeIndices.size * Int.SIZE_BYTES
        val wireframeIndexBufferConfig = VulkanBufferConfiguration(
            wireframeIndexBufferSize.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.INDEX_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.wireframeIndexBuffer = bufferFactory.createBuffer(wireframeIndexBufferConfig)

        val wireframeIndexByteBuffer = ByteBuffer.allocate(wireframeIndexBufferSize)
        wireframeIndexByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        CubeModelPositionsOnly.wireframeIndices.forEachIndexed { index, cubeIndex ->
            val offset = index * Int.SIZE_BYTES
            wireframeIndexByteBuffer.putInt(offset, cubeIndex)
        }

        deviceUtil.stagingCopy(wireframeIndexByteBuffer, wireframeIndexBuffer, 0L, 0L, wireframeIndexBufferSize.toLong())
        // WIREFRAME INDEX BUFFER
    }

    private fun updateInstanceDataBuffer() {
        val extent = voxelModel.voxelGrid.extent
        val byteBuffer = ByteBuffer.allocate(instanceDataBuffer.config.size.toInt())
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        voxelModel.voxelGrid.forEachVoxel { x, y, z, voxelType ->
            val offset = (z * extent * extent + y * extent + x) * VoxelData.SIZE_BYTES
            val voxelData = VoxelData(Vec3(x,y,z), voxelType.color.colorVec4)
            voxelData.toByteBuffer(byteBuffer, offset)
        }
        instanceDataBuffer.put(0, byteBuffer)
    }

    private fun frameUpdate(delta: Float) {
        handleInput(delta)

        val view = camera.calculateView()
        val proj = camera.calculateProjection()
        val data = ByteBuffer.allocate(128)
        data.order(ByteOrder.LITTLE_ENDIAN)
        view.toByteBufferColumnMajor(data, 0)
        proj.toByteBufferColumnMajor(data, 64)
        cameraBuffer.put(0, data)

        voxelModel.tick(delta)
        updateInstanceDataBuffer()
    }

    override fun recordFrame(preparation: FramePreparation, delta: Float): FrameSubmitData = runMemorySafe {
        frameUpdate(delta)

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
                .float32(0, 0.5f)
                .float32(1, 0.2f)
                .float32(2, 0.6f)
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
            val indexCount = CubeModelPositionsOnly.indices.size
            val wireframeIndexCount = CubeModelPositionsOnly.wireframeIndices.size
            val instanceCount = voxelModel.voxelGrid.voxelCount

            vkCmdSetViewport(commandBuffer.vkHandle, 0, viewport)
            vkCmdSetScissor(commandBuffer.vkHandle, 0, scissor)
            vkCmdBindDescriptorSets(commandBuffer.vkHandle, bindPoint, pipeline.vkLayoutHandle, 0, pDescriptorSets, null)
            vkCmdBindPipeline(commandBuffer.vkHandle, bindPoint, pipeline.vkHandle)
            vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
            vkCmdBindIndexBuffer(commandBuffer.vkHandle, indexBuffer.vkBufferHandle, 0L, VK_INDEX_TYPE_UINT32)
            vkCmdPushConstants(commandBuffer.vkHandle, pipeline.vkLayoutHandle, ShaderStage.BOTH.vkBits, 0, pPushConstants)
            vkCmdDrawIndexed(commandBuffer.vkHandle, indexCount, instanceCount, 0, 0, 0)

            vkCmdBindPipeline(commandBuffer.vkHandle, bindPoint, wireframePipeline.vkHandle)
            Mat4(1f)
                .translate(Vec3(voxelModel.voxelGrid.extent.toFloat() / 2f - 0.5f))
                .scale(Vec3(voxelModel.voxelGrid.extent.toFloat()))
                .toByteBufferColumnMajor(pPushConstants, 0)
            vkCmdPushConstants(commandBuffer.vkHandle, wireframePipeline.vkLayoutHandle, ShaderStage.BOTH.vkBits, 0, pPushConstants)
            vkCmdBindIndexBuffer(commandBuffer.vkHandle, wireframeIndexBuffer.vkBufferHandle, 0L, VK_INDEX_TYPE_UINT32)
            vkCmdDrawIndexed(commandBuffer.vkHandle, wireframeIndexCount, 1, 0, 0, 0)
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

    private fun handleInput(delta: Float) {
        val rotY = inputHandler.isKeyDown(Key.ARROW_RIGHT).toInt() - inputHandler.isKeyDown(Key.ARROW_LEFT).toInt()
        val rotX = inputHandler.isKeyDown(Key.ARROW_UP).toInt() - inputHandler.isKeyDown(Key.ARROW_DOWN).toInt()
        camera.rotation.x += rotX.toFloat() * 1.2f
        camera.rotation.y += rotY.toFloat() * 1.2f

        val xMove = inputHandler.isKeyDown(Key.A).toInt() - inputHandler.isKeyDown(Key.D).toInt()
        val yMove = inputHandler.isKeyDown(Key.SPACE).toInt() - inputHandler.isKeyDown(Key.LSHIFT).toInt()
        val zMove = inputHandler.isKeyDown(Key.W).toInt() - inputHandler.isKeyDown(Key.S).toInt()

        camera.position.x += xMove * 0.1f
        camera.position.y += yMove * 0.1f
        camera.position.z += zMove * 0.1f
    }

    override fun onResizeDestroy() {
        depthAttachment.destroy()
    }

    override fun onResizeRecreate(newExtent2D: ImageExtent2D) {
        createAttachmentImages()
    }

    override fun destroy() {
        device.waitIdle()
        vertexBuffer.destroy()
        indexBuffer.destroy()
        wireframeIndexBuffer.destroy()
        cameraBuffer.destroy()
        instanceDataBuffer.destroy()
        depthAttachment.destroy()
        descriptorPool.destroy(device)
        descriptorSetLayout.destroy(device)
        pipeline.destroy(device)
        wireframePipeline.destroy(device)
        super.destroy()
    }
}