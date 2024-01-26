package me.fexus.examples.compute

import me.fexus.camera.Camera2D
import me.fexus.examples.Globals
import me.fexus.math.vec.Vec2
import me.fexus.math.vec.Vec4
import me.fexus.memory.runMemorySafe
import me.fexus.model.QuadModel
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
import me.fexus.vulkan.component.pipeline.specializationconstant.SpecializationConstantInt
import me.fexus.vulkan.descriptors.image.sampler.AddressMode
import me.fexus.vulkan.descriptors.image.sampler.Filtering
import me.fexus.vulkan.descriptors.image.sampler.VulkanSampler
import me.fexus.vulkan.descriptors.image.sampler.VulkanSamplerConfiguration
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


class Compute : VulkanRendererBase(createWindow()) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Compute().start()
        }

        private fun createWindow() = Window("Mass GPU computing") {
            windowVisible()
            enableResizable()
            setInitialWindowSize(1067, 600)
            enableDecoration()
            setInitialWindowPosition(400, 300)
            enableAutoIconify()
        }

        private fun Boolean.toInt(): Int = if (this) 1 else 0

        private const val MAX_PARTICLE_COUNT = 1 shl 12
        private const val STORAGE_BUFFER_ARRAY_SIZE = 8
        private const val WORKGROUP_SIZE_X = 16
    }

    private var tickCounter: Long = 0L
    private val camera = Camera2D(Vec2(0f), Vec2(16f, 9f))

    private lateinit var depthAttachment: VulkanImage
    private lateinit var spriteVertexBuffer: VulkanBuffer
    private lateinit var spriteIndexBuffer: VulkanBuffer
    private lateinit var generalInfoBuffer: VulkanBuffer
    private lateinit var sampler: VulkanSampler
    private val descriptorPool = DescriptorPool()
    private val descriptorSetLayout = DescriptorSetLayout()
    private val descriptorSet = DescriptorSet()
    private val graphicsPipeline = GraphicsPipeline()

    private val computePipeline = ComputePipeline()

    private lateinit var particleInitialDataBuffer: VulkanBuffer
    private lateinit var particlePositionBuffer: VulkanBuffer

    private val inputHandler = InputHandler(window)


    fun start() {
        initVulkanCore()
        initObjects()
        startRenderLoop(window, this)
    }

    private fun initObjects() {
        val depthAttachmentImageLayout = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D,
            ImageExtent3D(swapchain.imageExtent, 1),
            1, 1, 1,
            ImageColorFormat.D32_SFLOAT, ImageTiling.OPTIMAL,
            ImageAspect.DEPTH, ImageUsage.DEPTH_STENCIL_ATTACHMENT,
            MemoryProperty.DEVICE_LOCAL
        )
        this.depthAttachment = imageFactory.createImage(depthAttachmentImageLayout)

        // -- VERTEX BUFFER --
        val vertexBufferSize = QuadModel.vertices.size * QuadModel.Vertex.SIZE_BYTES
        val vertexBufferData = ByteBuffer.allocate(vertexBufferSize)
        vertexBufferData.order(ByteOrder.LITTLE_ENDIAN)
        QuadModel.vertices.forEachIndexed { index, vertex ->
            val offset = index * QuadModel.Vertex.SIZE_BYTES
            vertex.writeToByteBuffer(vertexBufferData, offset)
        }
        val vertexBufferLayout = VulkanBufferConfiguration(
            vertexBufferSize.toLong(),
            MemoryProperty.DEVICE_LOCAL, BufferUsage.VERTEX_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.spriteVertexBuffer = bufferFactory.createBuffer(vertexBufferLayout)
        // Staging
        val stagingVertexBufferLayout = VulkanBufferConfiguration(
            vertexBufferSize.toLong(),
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.TRANSFER_SRC
        )
        val stagingVertexBuffer = bufferFactory.createBuffer(stagingVertexBufferLayout)
        stagingVertexBuffer.put(0, vertexBufferData)
        // Copy from Staging to Vertex Buffer
        runMemorySafe {
            val cmdBuf = deviceUtil.beginSingleTimeCommandBuffer()

            val copyRegion = calloc(VkBufferCopy::calloc, 1)
            copyRegion[0]
                .srcOffset(0)
                .dstOffset(0)
                .size(vertexBufferSize.toLong())
            vkCmdCopyBuffer(
                cmdBuf.vkHandle,
                stagingVertexBuffer.vkBufferHandle,
                spriteVertexBuffer.vkBufferHandle,
                copyRegion
            )

            deviceUtil.endSingleTimeCommandBuffer(cmdBuf)
        }
        stagingVertexBuffer.destroy()
        // -- VERTEX BUFFER --

        // -- INDEX BUFFER --
        val indexBufferSize = QuadModel.indices.size * Int.SIZE_BYTES
        val indexBufferData = ByteBuffer.allocate(indexBufferSize)
        indexBufferData.order(ByteOrder.LITTLE_ENDIAN)
        QuadModel.indices.forEachIndexed { index, vertexIndex ->
            val offset = index * Int.SIZE_BYTES
            indexBufferData.putInt(offset, vertexIndex)
        }
        val indexBufferConfig = VulkanBufferConfiguration(
            indexBufferSize.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.INDEX_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.spriteIndexBuffer = bufferFactory.createBuffer(indexBufferConfig)
        val stagingIndexBufferConfig = VulkanBufferConfiguration(
            indexBufferSize.toLong(),
            MemoryProperty.HOST_COHERENT + MemoryProperty.HOST_VISIBLE,
            BufferUsage.TRANSFER_SRC
        )
        val stagingIndexBuffer = bufferFactory.createBuffer(stagingIndexBufferConfig)
        stagingIndexBuffer.put(0, indexBufferData)

        runMemorySafe {
            val cmdBuf = deviceUtil.beginSingleTimeCommandBuffer()

            val copyRegion = calloc(VkBufferCopy::calloc, 1)
            copyRegion[0]
                .srcOffset(0)
                .dstOffset(0)
                .size(indexBufferSize.toLong())
            vkCmdCopyBuffer(
                cmdBuf.vkHandle,
                stagingIndexBuffer.vkBufferHandle,
                spriteIndexBuffer.vkBufferHandle,
                copyRegion
            )

            deviceUtil.endSingleTimeCommandBuffer(cmdBuf)
        }
        stagingIndexBuffer.destroy()
        // -- INDEX BUFFER --

        // -- CAMERA BUFFER --
        val cameraBufferLayout = VulkanBufferConfiguration(
            128L, MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT, BufferUsage.UNIFORM_BUFFER
        )
        this.generalInfoBuffer = bufferFactory.createBuffer(cameraBufferLayout)
        // -- CAMERA BUFFER --

        // -- SAMPLER --
        val samplerLayout = VulkanSamplerConfiguration(AddressMode.REPEAT, 1, Filtering.LINEAR)
        this.sampler = imageFactory.createSampler(samplerLayout)
        // -- SAMPLER --

        // Descriptor Sets and Pipeline
        val poolPlan = DescriptorPoolPlan(
            Globals.framesTotal, DescriptorPoolCreateFlag.FREE_DESCRIPTOR_SET,
            listOf(
                DescriptorPoolSize(DescriptorType.UNIFORM_BUFFER, 16),
                DescriptorPoolSize(DescriptorType.STORAGE_BUFFER, 16),
                DescriptorPoolSize(DescriptorType.SAMPLED_IMAGE, 16),
                DescriptorPoolSize(DescriptorType.SAMPLER, 16)
            )
        )
        this.descriptorPool.create(device, poolPlan)

        val setLayoutPlan = DescriptorSetLayoutPlan(
            DescriptorSetLayoutCreateFlag.NONE,
            listOf(
                DescriptorSetLayoutBinding(
                    0, 1,
                    DescriptorType.UNIFORM_BUFFER,
                    ShaderStage.COMPUTE + ShaderStage.VERTEX + ShaderStage.FRAGMENT,
                    DescriptorSetLayoutBindingFlag.NONE
                ),
                DescriptorSetLayoutBinding(
                    1, STORAGE_BUFFER_ARRAY_SIZE,
                    DescriptorType.STORAGE_BUFFER,
                    ShaderStage.COMPUTE + ShaderStage.VERTEX + ShaderStage.FRAGMENT,
                    DescriptorSetLayoutBindingFlag.PARTIALLY_BOUND
                )
            )
        )
        this.descriptorSetLayout.create(device, setLayoutPlan)
        this.descriptorSet.create(device, descriptorPool, descriptorSetLayout)

        createGraphicsComponents()
        createComputeComponents()

        // Update Descriptor Set
        val descWriteCameraBuf = DescriptorBufferWrite(
            0, DescriptorType.UNIFORM_BUFFER, 1, this.descriptorSet, 0,
            listOf(DescriptorBufferInfo(generalInfoBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE))
        )
        val descWriteBufferArr = DescriptorBufferWrite(
            1, DescriptorType.STORAGE_BUFFER, 2, this.descriptorSet, 0,
            listOf(
                DescriptorBufferInfo(particleInitialDataBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE),
                DescriptorBufferInfo(particlePositionBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE)
            )
        )
        this.descriptorSet.update(device, descWriteCameraBuf, descWriteBufferArr)
    }

    private fun createGraphicsComponents() {
        val pipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC4, 0),
                VertexAttribute(1, VertexAttributeFormat.VEC4, 16),
            ),
            PushConstantsLayout(128, shaderStages = ShaderStage.COMPUTE + ShaderStage.BOTH),
            ClassLoader.getSystemResource("shaders/compute/standard_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/compute/standard_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.VIEWPORT, DynamicState.SCISSOR),
            blendEnable = true
        )
        this.graphicsPipeline.create(device, listOf(descriptorSetLayout), pipelineConfig)
    }

    private fun createComputeComponents() {
        val computePipelineConfig = ComputePipelineConfiguration(
            ClassLoader.getSystemResource("shaders/compute/particle_compute.spv").readBytes(),
            pushConstantsLayout = PushConstantsLayout(128, shaderStages = ShaderStage.COMPUTE + ShaderStage.BOTH),
            specializationConstants = listOf(
                SpecializationConstantInt(0, STORAGE_BUFFER_ARRAY_SIZE),
                SpecializationConstantInt(1, WORKGROUP_SIZE_X)
            )
        )
        this.computePipeline.create(device, descriptorSetLayout, computePipelineConfig)

        val initialDataBufferConfig = VulkanBufferConfiguration(
            4096L,
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.STORAGE_BUFFER + BufferUsage.TRANSFER_DST + BufferUsage.TRANSFER_SRC
        )
        this.particleInitialDataBuffer = bufferFactory.createBuffer(initialDataBufferConfig)

        val positionBufferConfig = VulkanBufferConfiguration(
            32L * MAX_PARTICLE_COUNT,
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.STORAGE_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.particlePositionBuffer = bufferFactory.createBuffer(positionBufferConfig)
    }

    override fun recordFrame(preparation: FramePreparation, delta: Float): FrameSubmitData = runMemorySafe {
        handleInput()

        val data = ByteBuffer.allocate(128)
        data.order(ByteOrder.LITTLE_ENDIAN)
        camera.position.toByteBuffer(data, 0)
        camera.extent.toByteBuffer(data, 8)
        data.putInt(16, tickCounter.toInt())
        generalInfoBuffer.put(0, data)

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
                .float32(0, 0.1f)
                .float32(1, 0.1f)
                .float32(2, 0.1f)
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

        // COMPUTE
        // synchronize the read and write ops
        val computeBarrier = calloc(VkBufferMemoryBarrier::calloc, 2)
        with(computeBarrier[0]) {
            sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
            pNext(0L)
            buffer(particlePositionBuffer.vkBufferHandle)
            srcAccessMask(VK_ACCESS_SHADER_READ_BIT)
            dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            offset(0L)
            size(VK_WHOLE_SIZE)
        }
        with(computeBarrier[1]) {
            sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
            pNext(0L)
            buffer(particleInitialDataBuffer.vkBufferHandle)
            srcAccessMask(VK_ACCESS_SHADER_READ_BIT)
            dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            offset(0L)
            size(VK_WHOLE_SIZE)
        }
        vkCmdPipelineBarrier(
            commandBuffer.vkHandle,
            VK_PIPELINE_STAGE_VERTEX_SHADER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
            0, null, computeBarrier, null
        )

        // actual compute here
        val pDescriptorSets = allocateLong(1)
        pDescriptorSets.put(0, descriptorSet.vkHandle)

        val pPushConstants = allocate(128)
        Vec4(0f, 0f, 1f, 1f).toByteBuffer(pPushConstants, 0)
        pPushConstants.putFloat(16, 0.2f)

        vkCmdBindDescriptorSets(
            commandBuffer.vkHandle,
            VK_PIPELINE_BIND_POINT_COMPUTE,
            computePipeline.vkLayoutHandle,
            0,
            pDescriptorSets,
            null
        )
        vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.vkHandle)
        vkCmdPushConstants(
            commandBuffer.vkHandle,
            computePipeline.vkLayoutHandle,
            VK_SHADER_STAGE_COMPUTE_BIT or VK_SHADER_STAGE_FRAGMENT_BIT or VK_SHADER_STAGE_VERTEX_BIT,
            0,
            pPushConstants
        )
        vkCmdDispatch(commandBuffer.vkHandle, MAX_PARTICLE_COUNT / MAX_PARTICLE_COUNT, 1, 1)

        with(computeBarrier[0]) {
            srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
            dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
        }
        with(computeBarrier[1]) {
            srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
            dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
        }
        vkCmdPipelineBarrier(
            commandBuffer.vkHandle,
            VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_VERTEX_SHADER_BIT,
            0, null, computeBarrier, null
        )
        // COMPUTE

        // Continue with graphics
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

            val pVertexBuffers = allocateLong(1)
            pVertexBuffers.put(0, spriteVertexBuffer.vkBufferHandle)
            val pOffsets = allocateLong(1)
            pOffsets.put(0, 0L)

            val bindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS

            vkCmdSetViewport(commandBuffer.vkHandle, 0, viewport)
            vkCmdSetScissor(commandBuffer.vkHandle, 0, scissor)
            vkCmdBindDescriptorSets(
                commandBuffer.vkHandle,
                bindPoint,
                graphicsPipeline.vkLayoutHandle,
                0,
                pDescriptorSets,
                null
            )
            vkCmdBindPipeline(commandBuffer.vkHandle, bindPoint, graphicsPipeline.vkHandle)
            vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
            vkCmdBindIndexBuffer(commandBuffer.vkHandle, spriteIndexBuffer.vkBufferHandle, 0L, VK_INDEX_TYPE_UINT32)
            vkCmdPushConstants(
                commandBuffer.vkHandle,
                graphicsPipeline.vkLayoutHandle,
                VK_SHADER_STAGE_COMPUTE_BIT or VK_SHADER_STAGE_FRAGMENT_BIT or VK_SHADER_STAGE_VERTEX_BIT,
                0,
                pPushConstants
            )
            vkCmdDrawIndexed(commandBuffer.vkHandle, 6, MAX_PARTICLE_COUNT, 0, 0, 0)

            tickCounter++
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

    private fun handleInput() {
        val xMove = inputHandler.isKeyDown(Key.A).toInt() - inputHandler.isKeyDown(Key.D).toInt()
        val yMove = inputHandler.isKeyDown(Key.W).toInt() - inputHandler.isKeyDown(Key.S).toInt()

        camera.position.x += xMove * 0.1f
        camera.position.y += yMove * 0.1f
    }

    override fun onResizeDestroy() {
        depthAttachment.destroy()
    }

    override fun onResizeRecreate(newExtent2D: ImageExtent2D) {
        val depthAttachmentImageLayout = VulkanImageConfiguration(
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
        spriteVertexBuffer.destroy()
        spriteIndexBuffer.destroy()
        generalInfoBuffer.destroy()
        depthAttachment.destroy()
        descriptorPool.destroy(device)
        descriptorSetLayout.destroy(device)
        graphicsPipeline.destroy(device)
        computePipeline.destroy(device)
        particlePositionBuffer.destroy()
        particleInitialDataBuffer.destroy()
        super.destroy()
    }
}