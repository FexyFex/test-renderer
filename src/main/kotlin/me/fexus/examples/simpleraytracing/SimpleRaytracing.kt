package me.fexus.examples.simpleraytracing

import me.fexus.camera.CameraPerspective
import me.fexus.math.mat.Mat4
import me.fexus.math.vec.IVec3
import me.fexus.math.vec.Vec3
import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.model.CubeModel
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
import me.fexus.vulkan.component.pipeline.pipelinestage.PipelineStage
import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage
import me.fexus.vulkan.descriptors.image.sampler.AddressMode
import me.fexus.vulkan.descriptors.image.sampler.Filtering
import me.fexus.vulkan.descriptors.image.sampler.VulkanSampler
import me.fexus.vulkan.descriptors.image.sampler.VulkanSamplerLayout
import me.fexus.vulkan.extension.AccelerationStructureKHRExtension
import me.fexus.vulkan.extension.BufferDeviceAddressKHRExtension
import me.fexus.vulkan.extension.DeferredHostOperationsKHRExtension
import me.fexus.vulkan.extension.RayTracingPipelineKHRExtension
import me.fexus.vulkan.raytracing.*
import me.fexus.vulkan.raytracing.accelerationstructure.BottomLevelAccelerationStructureConfiguration
import me.fexus.vulkan.raytracing.accelerationstructure.BottomLevelAccelerationStructure
import me.fexus.vulkan.raytracing.accelerationstructure.TopLevelAccelerationStructure
import me.fexus.vulkan.raytracing.accelerationstructure.TopLevelAccelerationStructureConfiguration
import me.fexus.vulkan.util.ImageExtent2D
import me.fexus.vulkan.util.ImageExtent3D
import me.fexus.window.Window
import me.fexus.window.input.InputHandler
import me.fexus.window.input.Key
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRAccelerationStructure.*
import org.lwjgl.vulkan.KHRDynamicRendering.*
import org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR
import org.lwjgl.vulkan.VK12.*
import java.nio.ByteBuffer
import java.nio.ByteOrder


class SimpleRaytracing: VulkanRendererBase(createWindow()) {
    companion object {
        private const val EXTENT = 16
        private const val BLOCKS_PER_CHUNK = EXTENT * EXTENT * EXTENT

        @JvmStatic
        fun main(args: Array<String>) {
            SimpleRaytracing().start()
        }

        private fun createWindow() = Window("Parallax Voxel Raytracing") {
            windowVisible()
            enableResizable()
            setInitialWindowSize(1280,720)
            enableDecoration()
            setInitialWindowPosition(400, 300)
            enableAutoIconify()
        }

        private fun Boolean.toInt(): Int = if (this) 1 else 0
    }

    private var time: Double = 0.0

    private val camera = CameraPerspective(window.aspect)

    private lateinit var depthAttachment: VulkanImage
    private lateinit var vertexBuffer: VulkanBuffer
    private lateinit var cameraBuffer: VulkanBuffer
    private lateinit var cobbleImage: VulkanImage
    private lateinit var sampler: VulkanSampler
    private lateinit var objTransformBuffer: VulkanBuffer
    private lateinit var instanceDataBuffer: VulkanBuffer
    private val topLevelAS = TopLevelAccelerationStructure()
    private val bottomLevelAS = BottomLevelAccelerationStructure()
    private val descriptorPool = DescriptorPool()
    private val descriptorSetLayout = DescriptorSetLayout()
    private val descriptorSet = DescriptorSet()
    private val raytracingPipeline = RaytracingPipeline()

    private val cubePosition = Vec3(0f, 0f, -5f)
    private val cubeTransform = Mat4(1f).translate(cubePosition)
    private val cobbleTexture = TextureLoader("textures/parallaxvoxelraytracing/cobblestone.png")

    private val inputHandler = InputHandler(window)


    fun start() {
        val extraExtensions = listOf(
            AccelerationStructureKHRExtension,
            RayTracingPipelineKHRExtension,
            BufferDeviceAddressKHRExtension,
            DeferredHostOperationsKHRExtension
        )
        initVulkanCore(extraExtensions)
        createDescriptors()
        initDescriptorData()
        buildAccelerationStructures()
        startRenderLoop(window, this)
    }

    private fun createDescriptors() {
        camera.position = Vec3(0f, 0f, 5f)

        // -- DESCRIPTOR POOL --
        val poolPlan = DescriptorPoolPlan(
            16, DescriptorPoolCreateFlag.FREE_DESCRIPTOR_SET, listOf(
                DescriptorPoolSize(DescriptorType.UNIFORM_BUFFER, 16),
                DescriptorPoolSize(DescriptorType.STORAGE_BUFFER, 16),
                DescriptorPoolSize(DescriptorType.SAMPLED_IMAGE, 16),
                DescriptorPoolSize(DescriptorType.SAMPLER, 16)
            )
        )
        this.descriptorPool.create(device, poolPlan)
        // -- DESCRIPTOR POOL --

        // -- DEPTH ATTACHMENT IMAGE --
        val depthAttachmentImageLayout = VulkanImageLayout(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D,
            ImageExtent3D(swapchain.imageExtent, 1),
            1, 1, 1,
            ImageColorFormat.D32_SFLOAT, ImageTiling.OPTIMAL,
            ImageAspect.DEPTH, ImageUsage.DEPTH_STENCIL_ATTACHMENT,
            MemoryProperty.DEVICE_LOCAL
        )
        this.depthAttachment = imageFactory.createImage(depthAttachmentImageLayout)
        // -- DEPTH ATTACHMENT IMAGE --

        // VERTEX BUFFER
        val cubeVertexBufSize = CubeModel.vertices.size * CubeModel.Vertex.SIZE_BYTES
        val vertexBufferLayout = VulkanBufferLayout(
            cubeVertexBufSize.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.VERTEX_BUFFER + BufferUsage.TRANSFER_DST +
                    BufferUsage.SHADER_DEVICE_ADDRESS + BufferUsage.STORAGE_BUFFER +
                    BufferUsage.ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_KHR
        )
        this.vertexBuffer = bufferFactory.createBuffer(vertexBufferLayout)

        // -- CAMERA BUFFER --
        val cameraBufferLayout = VulkanBufferLayout(
            256L,
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.UNIFORM_BUFFER
        )
        this.cameraBuffer = bufferFactory.createBuffer(cameraBufferLayout)
        // -- CAMERA BUFFER --

        // -- TEXTURES --
        val imageLayout = VulkanImageLayout(
                ImageType.TYPE_2D, ImageViewType.TYPE_2D, ImageExtent3D(cobbleTexture.width, cobbleTexture.height, 1),
                1, 1, 1, ImageColorFormat.R8G8B8A8_SRGB, ImageTiling.OPTIMAL,
                ImageAspect.COLOR, ImageUsage.SAMPLED + ImageUsage.TRANSFER_DST, MemoryProperty.DEVICE_LOCAL
        )
        this.cobbleImage = imageFactory.createImage(imageLayout)
        // -- TEXTURES --

        // -- SAMPLER --
        val samplerLayout = VulkanSamplerLayout(AddressMode.REPEAT, 1, Filtering.NEAREST)
        this.sampler = imageFactory.createSampler(samplerLayout)
        // -- SAMPLER --

        // -- DESCRIPTOR SET LAYOUT --
        val setLayoutPlan = DescriptorSetLayoutPlan(
            DescriptorSetLayoutCreateFlag.NONE, listOf(
                DescriptorSetLayoutBinding(
                    0, 1,
                    DescriptorType.UNIFORM_BUFFER,
                    ShaderStage.VERTEX + ShaderStage.RAYGEN,
                    DescriptorSetLayoutBindingFlag.NONE
                ),
                DescriptorSetLayoutBinding(
                    1, 1,
                    DescriptorType.STORAGE_BUFFER,
                    ShaderStage.BOTH,
                    DescriptorSetLayoutBindingFlag.NONE
                ),
                DescriptorSetLayoutBinding(
                    2, 1,
                    DescriptorType.SAMPLED_IMAGE,
                    ShaderStage.FRAGMENT + ShaderStage.CLOSEST_HIT,
                    DescriptorSetLayoutBindingFlag.NONE
                ),
                DescriptorSetLayoutBinding(
                    3, 1,
                    DescriptorType.SAMPLER,
                    ShaderStage.FRAGMENT + ShaderStage.CLOSEST_HIT,
                    DescriptorSetLayoutBindingFlag.NONE
                ),
            )
        )
        this.descriptorSetLayout.create(device, setLayoutPlan)
        // -- DESCRIPTOR SET LAYOUT --

        // -- DESCRIPTOR SET --
        this.descriptorSet.create(device, descriptorPool, descriptorSetLayout)

        val descWriteCameraBuf = DescriptorBufferWrite(
            0, DescriptorType.UNIFORM_BUFFER, 1, this.descriptorSet, 0,
            listOf(DescriptorBufferInfo(cameraBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE))
        )
        val descWriteCobbleImg = DescriptorImageWrite(
            2, DescriptorType.SAMPLED_IMAGE, 1, this.descriptorSet, 0,
            listOf(DescriptorImageInfo(0L, this.cobbleImage.vkImageViewHandle, ImageLayout.SHADER_READ_ONLY_OPTIMAL))
        )
        val descSampler = DescriptorImageWrite(
            3, DescriptorType.SAMPLER, 1, this.descriptorSet, 0,
            listOf(DescriptorImageInfo(this.sampler.vkHandle, 0L, ImageLayout.SHADER_READ_ONLY_OPTIMAL))
        )

        this.descriptorSet.update(device, descWriteCameraBuf, descWriteCobbleImg, descSampler)
        // -- DESCRIPTOR SET --

        // -- RAYTRACING PIPELINE --
        val raygenShaderCode = ByteArray(8) { 0 }
        val config = RaytracingPipelineConfiguration(
            listOf(
                RaytracingShaderStage(ShaderStage.RAYGEN, RaytracingShaderGroupType.GENERAL, raygenShaderCode)
            ),
            PushConstantsLayout(128, shaderStages = ShaderStage.VERTEX + ShaderStage.CLOSEST_HIT)
        )
        this.raytracingPipeline.create(device, descriptorSetLayout, config)
        // -- RAYTRACING PIPELINE --
    }

    private fun initDescriptorData() {
        // VERTEX BUFFER
        val cubeVertexBufSize = CubeModel.vertices.size * CubeModel.Vertex.SIZE_BYTES
        val vertexBufferData = ByteBuffer.allocate(cubeVertexBufSize)
        vertexBufferData.order(ByteOrder.LITTLE_ENDIAN)
        CubeModel.vertices.forEachIndexed { vertIndex, vert ->
            vert.toFloatArray().forEachIndexed { flIndex, fl ->
                val offset = vertIndex * CubeModel.Vertex.SIZE_BYTES + (flIndex * Float.SIZE_BYTES)
                vertexBufferData.putFloat(offset, fl)
            }
        }
        val stagingVertexBufferLayout = VulkanBufferLayout(
            cubeVertexBufSize.toLong(),
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.TRANSFER_SRC
        )
        val stagingVertexBuffer = bufferFactory.createBuffer(stagingVertexBufferLayout)
        stagingVertexBuffer.put(0, vertexBufferData)
        runMemorySafe {
            val cmdBuf = beginSingleTimeCommandBuffer()

            val copyRegion = calloc(VkBufferCopy::calloc, 1)
            copyRegion[0]
                .srcOffset(0)
                .dstOffset(0)
                .size(cubeVertexBufSize.toLong())
            vkCmdCopyBuffer(cmdBuf.vkHandle, stagingVertexBuffer.vkBufferHandle, vertexBuffer.vkBufferHandle, copyRegion)

            endSingleTimeCommandBuffer(cmdBuf)
        }
        stagingVertexBuffer.destroy()
        // VERTEX BUFFER


        // TEXTURE IMAGE
        val stagingImageBufLayout = VulkanBufferLayout(
            cobbleTexture.imageSize,
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.TRANSFER_SRC
        )
        val stagingBufImg = bufferFactory.createBuffer(stagingImageBufLayout)
        stagingBufImg.put(0, cobbleTexture.pixels)
        runMemorySafe {
            val cmdBuf = beginSingleTimeCommandBuffer()

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
                .image(this@SimpleRaytracing.cobbleImage.vkImageHandle)
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

            val copyRegions = calloc(VkBufferImageCopy::calloc, 1)
            copyRegions[0].bufferOffset(0L)
            copyRegions[0].imageExtent().width(cobbleTexture.width).height(cobbleTexture.height).depth(1)
            copyRegions[0].imageOffset().x(0).y(0).z(0)
            copyRegions[0].imageSubresource()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .mipLevel(0)
                .baseArrayLayer(0)
                .layerCount(1)

            vkCmdCopyBufferToImage(
                cmdBuf.vkHandle,
                stagingBufImg.vkBufferHandle, this@SimpleRaytracing.cobbleImage.vkImageHandle,
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
        stagingBufImg.destroy()
        // TEXTURE IMAGE
    }

    private fun buildAccelerationStructures() = runSingleTimeCommands {
        val bottomLevelASConfig = BottomLevelAccelerationStructureConfiguration(
                CubeModel.vertices.size / 3,
                vertexBuffer, null, objTransformBuffer,
                CubeModel.Vertex.SIZE_BYTES
        )
        this.bottomLevelAS.createAndBuild(device, bufferFactory, it, bottomLevelASConfig)

        val topLevelASConfig = TopLevelAccelerationStructureConfiguration(
                this.cubeTransform, instanceDataBuffer, bottomLevelAS
        )
        this.topLevelAS.createAndBuild(device, bufferFactory, it, topLevelASConfig)
    }

    override fun recordFrame(preparation: FramePreparation, delta: Float): FrameSubmitData = runMemorySafe {
        time += delta

        handleInput()

        val view = camera.calculateView()
        val proj = camera.calculateReverseZProjection()
        val data = ByteBuffer.allocate(128)
        data.order(ByteOrder.LITTLE_ENDIAN)
        view.toByteBuffer(data, 0)
        proj.toByteBuffer(data, 64)
        cameraBuffer.put(0, data)

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

            val pVertexBuffers = allocateLong(1)
            pVertexBuffers.put(0, vertexBuffer.vkBufferHandle)
            val pOffsets = allocateLong(1)
            pOffsets.put(0, 0L)
            val pDescriptorSets = allocateLong(1)
            pDescriptorSets.put(0, descriptorSet.vkHandle)

            val bindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS

            vkCmdSetViewport(commandBuffer.vkHandle, 0, viewport)
            vkCmdSetScissor(commandBuffer.vkHandle, 0, scissor)

            vkCmdBindDescriptorSets(
                commandBuffer.vkHandle,
                bindPoint,
                pipeline.vkLayoutHandle,
                0,
                pDescriptorSets,
                null
            )

            val pPushConstants = allocate(128)

            vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
            vkCmdBindPipeline(commandBuffer.vkHandle, bindPoint, pipeline.vkHandle)
            vkCmdPushConstants(
                commandBuffer.vkHandle,
                pipeline.vkLayoutHandle,
                ShaderStage.BOTH.vkBits,
                0,
                pPushConstants
            )
            vkCmdDraw(commandBuffer.vkHandle, CubeModel.vertices.size, 1, 0, 0)
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

        if (inputHandler.isKeyDown(Key.P)) {
            println("Camera Pos: ${-camera.position}")
            println("Camera Rot: ${camera.rotation}")
        }
    }

    fun worldPosToChunkPos(worldPos: IVec3): IVec3 {
        return IVec3(worldPos.x / EXTENT - if (worldPos.x < 0 && worldPos.x % EXTENT != 0) 1 else 0,
            worldPos.y / EXTENT - if (worldPos.y < 0 && worldPos.y % EXTENT != 0) 1 else 0,
            worldPos.z / EXTENT - if (worldPos.z < 0 && worldPos.z % EXTENT != 0) 1 else 0)
    }

    fun worldPosToChunkPos(worldPos: Vec3): IVec3 {
        return IVec3(worldPos.x / EXTENT - if (worldPos.x < 0 || (worldPos.x) % EXTENT == 0f) 1 else 0,
            worldPos.y / EXTENT - if (worldPos.y < 0 || (worldPos.y) % EXTENT == 0f) 1 else 0,
            worldPos.z / EXTENT - if (worldPos.z < 0 || (worldPos.z) % EXTENT == 0f) 1 else 0)
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
        cobbleImage.destroy()
        vertexBuffer.destroy()
        sampler.destroy(device)
        cameraBuffer.destroy()
        depthAttachment.destroy()
        descriptorPool.destroy(device)
        descriptorSetLayout.destroy(device)
        super.destroy()
    }
}