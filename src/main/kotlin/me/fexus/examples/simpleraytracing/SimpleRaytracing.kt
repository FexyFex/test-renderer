package me.fexus.examples.simpleraytracing

import me.fexus.camera.CameraPerspective
import me.fexus.math.mat.Mat4
import me.fexus.math.vec.Vec3
import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.model.CubeModel
import me.fexus.texture.TextureLoader
import me.fexus.vulkan.util.FramePreparation
import me.fexus.vulkan.util.FrameSubmitData
import me.fexus.vulkan.VulkanRendererBase
import me.fexus.vulkan.accessmask.AccessMask
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
import me.fexus.vulkan.component.descriptor.write.*
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
import me.fexus.vulkan.descriptors.image.sampler.VulkanSamplerLayout
import me.fexus.vulkan.extension.AccelerationStructureKHRExtension
import me.fexus.vulkan.extension.BufferDeviceAddressKHRExtension
import me.fexus.vulkan.extension.DeferredHostOperationsKHRExtension
import me.fexus.vulkan.extension.RayTracingPipelineKHRExtension
import me.fexus.vulkan.raytracing.*
import me.fexus.vulkan.raytracing.accelerationstructure.*
import me.fexus.vulkan.util.ImageExtent2D
import me.fexus.vulkan.util.ImageExtent3D
import me.fexus.window.Window
import me.fexus.window.input.InputHandler
import me.fexus.window.input.Key
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRAccelerationStructure.*
import org.lwjgl.vulkan.KHRDynamicRendering.*
import org.lwjgl.vulkan.KHRRayTracingPipeline.*
import org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR
import org.lwjgl.vulkan.VK12.*
import java.nio.ByteBuffer
import java.nio.ByteOrder


class SimpleRaytracing: VulkanRendererBase(createWindow()) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SimpleRaytracing().start()
        }

        private fun createWindow() = Window("Simple Raytracing") {
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
    private lateinit var storageImage: VulkanImage
    private lateinit var sampler: VulkanSampler
    private lateinit var objTransformBuffer: VulkanBuffer
    private lateinit var instanceDataBuffer: VulkanBuffer
    private lateinit var raytracingProperties: RaytracingProperties
    private val topLevelAS = TopLevelAccelerationStructure()
    private val bottomLevelAS = BottomLevelAccelerationStructure()
    private val descriptorPool = DescriptorPool()
    private val descriptorSetLayout = DescriptorSetLayout()
    private val descriptorSet = DescriptorSet()
    private val raytracingPipeline = RaytracingPipeline()
    lateinit var raygenShaderBindingTable: VulkanBuffer
    lateinit var missShaderBindingTable: VulkanBuffer
    lateinit var chitShaderBindingTable: VulkanBuffer

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
        createRaytracingPipeline()
        initDescriptors()
        createShaderBindingTable()
        writeDescriptorSets()
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
                DescriptorPoolSize(DescriptorType.SAMPLER, 16),
                DescriptorPoolSize(DescriptorType.ACCELERATION_STRUCTURE, 16)
            )
        )
        this.descriptorPool.create(device, poolPlan)
        // -- DESCRIPTOR POOL --

        // -- DEPTH ATTACHMENT IMAGE --
        val depthAttachmentImageLayout = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D,
            ImageExtent3D(swapchain.imageExtent, 1),
            1, 1, 1,
            ImageColorFormat.D32_SFLOAT, ImageTiling.OPTIMAL,
            ImageAspect.DEPTH, ImageUsage.DEPTH_STENCIL_ATTACHMENT,
            MemoryProperty.DEVICE_LOCAL
        )
        this.depthAttachment = imageFactory.createImage(depthAttachmentImageLayout)
        assignName(this.depthAttachment.vkImageHandle, VK_OBJECT_TYPE_IMAGE, "depth_image")
        // -- DEPTH ATTACHMENT IMAGE --

        // -- VERTEX BUFFER --
        val cubeVertexBufSize = CubeModel.vertices.size * CubeModel.Vertex.SIZE_BYTES
        val vertexBufferLayout = VulkanBufferConfiguration(
            cubeVertexBufSize.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.VERTEX_BUFFER + BufferUsage.TRANSFER_DST +
                    BufferUsage.SHADER_DEVICE_ADDRESS + BufferUsage.STORAGE_BUFFER +
                    BufferUsage.ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_KHR
        )
        this.vertexBuffer = bufferFactory.createBuffer(vertexBufferLayout)
        assignName(this.vertexBuffer.vkBufferHandle, VK_OBJECT_TYPE_BUFFER, "vertex_buffer")
        // -- VERTEX BUFFER --

        // -- TRANSFORM DATA BUFFER --
        val transformBufLayout = VulkanBufferConfiguration(
            64L,
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.SHADER_DEVICE_ADDRESS +
                    BufferUsage.ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_KHR +
                    BufferUsage.TRANSFER_DST
        )
        this.objTransformBuffer = bufferFactory.createBuffer(transformBufLayout)
        assignName(this.objTransformBuffer.vkBufferHandle, VK_OBJECT_TYPE_BUFFER, "transform_buffer")
        // -- TRANSFORM DATA BUFFER --

        // -- INSTANCE DATA BUFFER
        val instanceDataBufLayout = VulkanBufferConfiguration(
            AccelerationStructureInstance.SIZE_BYTES.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.SHADER_DEVICE_ADDRESS +
                    BufferUsage.ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_KHR +
                    BufferUsage.TRANSFER_DST
        )
        this.instanceDataBuffer = bufferFactory.createBuffer(instanceDataBufLayout)
        assignName(this.instanceDataBuffer.vkBufferHandle, VK_OBJECT_TYPE_BUFFER, "instance_data_buffer")
        // -- INSTANCE DATA BUFFER

        // -- CAMERA BUFFER --
        val cameraBufferLayout = VulkanBufferConfiguration(
            256L,
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.UNIFORM_BUFFER
        )
        this.cameraBuffer = bufferFactory.createBuffer(cameraBufferLayout)
        assignName(this.cameraBuffer.vkBufferHandle, VK_OBJECT_TYPE_BUFFER, "camera_uniform_buffer")
        // -- CAMERA BUFFER --

        // -- TEXTURES --
        val cobbleImageConfig = VulkanImageConfiguration(
                ImageType.TYPE_2D, ImageViewType.TYPE_2D, ImageExtent3D(cobbleTexture.width, cobbleTexture.height, 1),
                1, 1, 1, ImageColorFormat.R8G8B8A8_SRGB, ImageTiling.OPTIMAL,
                ImageAspect.COLOR, ImageUsage.SAMPLED + ImageUsage.TRANSFER_DST, MemoryProperty.DEVICE_LOCAL
        )
        this.cobbleImage = imageFactory.createImage(cobbleImageConfig)
        assignName(this.cobbleImage.vkImageHandle, VK_OBJECT_TYPE_IMAGE, "cobble_image")
        // -- TEXTURES --

        // -- STORAGE IMAGE --
        val storageImageLayout = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D, ImageExtent3D(swapchain.imageExtent, 1), 1,
            VK_SAMPLE_COUNT_1_BIT, 1, ImageColorFormat.B8G8R8A8_UNORM, ImageTiling.OPTIMAL, ImageAspect.COLOR,
            ImageUsage.TRANSFER_SRC + ImageUsage.STORAGE, MemoryProperty.DEVICE_LOCAL
        )
        this.storageImage = imageFactory.createImage(storageImageLayout)
        assignName(this.storageImage.vkImageHandle, VK_OBJECT_TYPE_IMAGE, "storage_image")
        runSingleTimeCommands { cmdBuf ->
            cmdTransitionImageLayout(
                cmdBuf, this@SimpleRaytracing.storageImage,
                AccessMask.NONE, AccessMask.SHADER_READ,
                ImageLayout.UNDEFINED, ImageLayout.GENERAL,
                PipelineStage.TOP_OF_PIPE, PipelineStage.FRAGMENT_SHADER
            )
        }
        // -- STORAGE IMAGE --

        // -- SAMPLER --
        val samplerLayout = VulkanSamplerLayout(AddressMode.REPEAT, 1, Filtering.NEAREST)
        this.sampler = imageFactory.createSampler(samplerLayout)
        assignName(this.sampler.vkHandle, VK_OBJECT_TYPE_SAMPLER, "soompler")
        // -- SAMPLER --

        // -- DESCRIPTOR SET LAYOUT --
        val setLayoutPlan = DescriptorSetLayoutPlan(
            DescriptorSetLayoutCreateFlag.NONE, listOf(
                DescriptorSetLayoutBinding(
                    0, 1,
                    DescriptorType.ACCELERATION_STRUCTURE,
                    ShaderStage.RAYGEN,
                    DescriptorSetLayoutBindingFlag.NONE
                ),
                DescriptorSetLayoutBinding(
                    1, 1,
                    DescriptorType.STORAGE_IMAGE,
                    ShaderStage.RAYGEN,
                    DescriptorSetLayoutBindingFlag.NONE
                ),
                DescriptorSetLayoutBinding(
                    2, 1,
                    DescriptorType.UNIFORM_BUFFER,
                    ShaderStage.RAYGEN,
                    DescriptorSetLayoutBindingFlag.NONE
                ),
            )
        )
        this.descriptorSetLayout.create(device, setLayoutPlan)
        // -- DESCRIPTOR SET LAYOUT --
    }

    private fun createRaytracingPipeline() {
        val raygenShaderCode = ClassLoader.getSystemResource("shaders/simpleraytracing/raytracing/rgen.spv").readBytes()
        val missShaderCode = ClassLoader.getSystemResource("shaders/simpleraytracing/raytracing/rmiss.spv").readBytes()
        val chitShaderCode = ClassLoader.getSystemResource("shaders/simpleraytracing/raytracing/rchit.spv").readBytes()
        val config = RaytracingPipelineConfiguration(
            listOf(
                RaytracingShaderStage(ShaderStage.RAYGEN, RaytracingShaderGroupType.GENERAL, raygenShaderCode),
                RaytracingShaderStage(ShaderStage.MISS, RaytracingShaderGroupType.GENERAL, missShaderCode),
                RaytracingShaderStage(ShaderStage.CLOSEST_HIT, RaytracingShaderGroupType.TRIANGLES_HIT, chitShaderCode)
            ),
            PushConstantsLayout(128, shaderStages = ShaderStage.VERTEX + ShaderStage.CLOSEST_HIT)
        )
        this.raytracingPipeline.create(device, descriptorSetLayout, config)
    }

    private fun initDescriptors() {
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
        bufferCopy(vertexBufferData, this.vertexBuffer, 0L, 0L, cubeVertexBufSize.toLong())
        // VERTEX BUFFER

        // TRANSFORM BUFFER
        val transformByteBuffer = ByteBuffer.allocate(Mat4.SIZE_BYTES)
        transformByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        cubeTransform.toByteBuffer(transformByteBuffer, 0)
        bufferCopy(transformByteBuffer, this.objTransformBuffer, 0L, 0L, Mat4.SIZE_BYTES.toLong())
        // TRANSFORM BUFFER

        initBottomLevelAccelerationStructure()

        // INSTANCE DATA BUFFER
        val transformMatrix = TransformMatrix(cubeTransform)
        val instance = AccelerationStructureInstance(
            transformMatrix, 0, 0xFF, 0,
            VK_GEOMETRY_INSTANCE_TRIANGLE_FACING_CULL_DISABLE_BIT_KHR, bottomLevelAS.deviceAddress
        )
        val instanceByteBuffer = ByteBuffer.allocate(AccelerationStructureInstance.SIZE_BYTES)
        instanceByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        instance.toByteBuffer(instanceByteBuffer, 0)
        bufferCopy(instanceByteBuffer, this.instanceDataBuffer, 0L, 0L, AccelerationStructureInstance.SIZE_BYTES.toLong())
        // INSTANCE DATA BUFFER

        initTopLevelAccelerationStructure()

        // TEXTURE IMAGE
        val stagingImageBufLayout = VulkanBufferConfiguration(
            cobbleTexture.imageSize,
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.TRANSFER_SRC
        )
        val stagingBufImg = bufferFactory.createBuffer(stagingImageBufLayout)
        stagingBufImg.put(0, cobbleTexture.pixels, 0)
        assignName(stagingBufImg.vkBufferHandle, VK_OBJECT_TYPE_BUFFER, "img_staging_buf_cobble")
        runMemorySafe {
            val cmdBuf = beginSingleTimeCommandBuffer()
            cmdTransitionImageLayout(
                cmdBuf, this@SimpleRaytracing.cobbleImage,
                AccessMask.NONE, AccessMask.TRANSFER_WRITE,
                ImageLayout.UNDEFINED, ImageLayout.TRANSFER_DST_OPTIMAL,
                PipelineStage.TOP_OF_PIPE, PipelineStage.TRANSFER
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

            cmdTransitionImageLayout(
                cmdBuf, this@SimpleRaytracing.cobbleImage,
                AccessMask.TRANSFER_WRITE, AccessMask.SHADER_READ,
                ImageLayout.TRANSFER_DST_OPTIMAL, ImageLayout.SHADER_READ_ONLY_OPTIMAL,
                PipelineStage.TRANSFER, PipelineStage.FRAGMENT_SHADER
            )

            endSingleTimeCommandBuffer(cmdBuf)
        }
        stagingBufImg.destroy()
        // TEXTURE IMAGE
    }

    private fun initBottomLevelAccelerationStructure() {
        val bottomLevelASConfig = BottomLevelAccelerationStructureConfiguration(
            CubeModel.vertices.size / 3,
            vertexBuffer, null, objTransformBuffer,
            CubeModel.Vertex.SIZE_BYTES
        )
        this.bottomLevelAS.createAndBuild(
            device,
            bufferFactory,
            this::beginSingleTimeCommandBuffer,
            this::endSingleTimeCommandBuffer,
            bottomLevelASConfig
        )
    }

    private fun initTopLevelAccelerationStructure() {
        val topLevelASConfig = TopLevelAccelerationStructureConfiguration(instanceDataBuffer, bottomLevelAS)
        this.topLevelAS.createAndBuild(
            device,
            bufferFactory,
            this::beginSingleTimeCommandBuffer,
            this::endSingleTimeCommandBuffer,
            topLevelASConfig
        )
    }

    private fun createShaderBindingTable() = runMemorySafe {
        val raytracingProps = calloc(VkPhysicalDeviceRayTracingPipelinePropertiesKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_PIPELINE_PROPERTIES_KHR)
            pNext(0L)
        }
        val deviceProps = calloc(VkPhysicalDeviceProperties2::calloc) {
            sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2)
            pNext(raytracingProps.address())
        }
        vkGetPhysicalDeviceProperties2(physicalDevice.vkHandle, deviceProps)

        this@SimpleRaytracing.raytracingProperties = RaytracingProperties(
            raytracingProps.shaderGroupHandleSize(),
            raytracingProps.shaderGroupHandleAlignment()
        )

        val handleAlignmentSize = raytracingProperties.shaderGroupHandleAlignment
        val groupCount = raytracingPipeline.shaderGroupCount
        val sbtSize = groupCount * handleAlignmentSize

        val pData = allocate(sbtSize)
        vkGetRayTracingShaderGroupHandlesKHR(device.vkHandle, raytracingPipeline.vkHandle, 0, groupCount, pData)

        val bufferUsage = BufferUsage.SHADER_BINDING_TABLE + BufferUsage.SHADER_DEVICE_ADDRESS
        val memoryProperties = MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT
        val bufferConfig = VulkanBufferConfiguration(
            raytracingProperties.shaderGroupHandleSize.toLong(), memoryProperties, bufferUsage
        )

        this@SimpleRaytracing.raygenShaderBindingTable = bufferFactory.createBuffer(bufferConfig)
        this@SimpleRaytracing.missShaderBindingTable = bufferFactory.createBuffer(bufferConfig)
        this@SimpleRaytracing.chitShaderBindingTable = bufferFactory.createBuffer(bufferConfig)

        this@SimpleRaytracing.raygenShaderBindingTable.put(0, pData, 0, handleAlignmentSize)
        this@SimpleRaytracing.missShaderBindingTable.put(0, pData, handleAlignmentSize, handleAlignmentSize)
        this@SimpleRaytracing.chitShaderBindingTable.put(0, pData, handleAlignmentSize * 2, handleAlignmentSize)
    }

    private fun writeDescriptorSets() {
        // -- DESCRIPTOR SET --
        this.descriptorSet.create(device, descriptorPool, descriptorSetLayout)

        val descWriteAccStruct = DescriptorAccelerationStructureWrite (
                0, DescriptorType.ACCELERATION_STRUCTURE, 1, this.descriptorSet, 0,
                DescriptorBufferInfo(topLevelAS.vkHandle, 0L, VK_WHOLE_SIZE)
        )
        val storageImageWrite = DescriptorImageWrite(
                1, DescriptorType.STORAGE_IMAGE, 1, this.descriptorSet, 0,
                listOf(DescriptorImageInfo(0L, this.storageImage.vkImageViewHandle, ImageLayout.GENERAL))
        )
        val uniformBufWrite = DescriptorBufferWrite(
                2, DescriptorType.UNIFORM_BUFFER, 1, this.descriptorSet, 0,
                listOf(DescriptorBufferInfo(cameraBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE))
        )

        this.descriptorSet.update(device, descWriteAccStruct, storageImageWrite, uniformBufWrite)
        // -- DESCRIPTOR SET --
    }

    override fun recordFrame(preparation: FramePreparation, delta: Float): FrameSubmitData = runMemorySafe {
        time += delta

        handleInput()

        val view = camera.calculateView().inverse()
        val proj = camera.calculateProjection().inverse()
        val data = ByteBuffer.allocate(128)
        data.order(ByteOrder.LITTLE_ENDIAN)
        view.toByteBuffer(data, 0)
        proj.toByteBuffer(data, 64)
        cameraBuffer.put(0, data, 0)

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
            newLayout(ImageLayout.TRANSFER_DST_OPTIMAL.vkValue)
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

            val bindPoint = VK_PIPELINE_BIND_POINT_RAY_TRACING_KHR

            vkCmdSetViewport(commandBuffer.vkHandle, 0, viewport)
            vkCmdSetScissor(commandBuffer.vkHandle, 0, scissor)

            vkCmdBindDescriptorSets(
                commandBuffer.vkHandle,
                bindPoint,
                raytracingPipeline.vkLayoutHandle,
                0,
                pDescriptorSets,
                null
            )

            val rayGenEntry = calloc(VkStridedDeviceAddressRegionKHR::calloc) {
                deviceAddress(raygenShaderBindingTable.getDeviceAddress())
                stride(raytracingProperties.shaderGroupHandleAlignment.toLong())
                size(raytracingProperties.shaderGroupHandleSize.toLong())
            }
            val missEntry = calloc(VkStridedDeviceAddressRegionKHR::calloc) {
                deviceAddress(missShaderBindingTable.getDeviceAddress())
                stride(raytracingProperties.shaderGroupHandleAlignment.toLong())
                size(raytracingProperties.shaderGroupHandleSize.toLong())
            }
            val chitEntry = calloc(VkStridedDeviceAddressRegionKHR::calloc) {
                deviceAddress(chitShaderBindingTable.getDeviceAddress())
                stride(raytracingProperties.shaderGroupHandleAlignment.toLong())
                size(raytracingProperties.shaderGroupHandleSize.toLong())
            }
            val callableShaderEntry = calloc(VkStridedDeviceAddressRegionKHR::calloc) {}

            val pPushConstants = allocate(128)

            vkCmdBindPipeline(commandBuffer.vkHandle, bindPoint, raytracingPipeline.vkHandle)
            vkCmdPushConstants(
                commandBuffer.vkHandle,
                raytracingPipeline.vkLayoutHandle,
                (ShaderStage.CLOSEST_HIT + ShaderStage.VERTEX).vkBits,
                0,
                pPushConstants
            )
            vkCmdTraceRaysKHR(
                commandBuffer.vkHandle,
                rayGenEntry, missEntry, chitEntry, callableShaderEntry,
                swapchain.imageExtent.width, swapchain.imageExtent.height, 1
            )

            cmdTransitionImageLayout(
                commandBuffer, storageImage,
                AccessMask.SHADER_WRITE, AccessMask.TRANSFER_READ,
                ImageLayout.GENERAL, ImageLayout.TRANSFER_SRC_OPTIMAL,
                PipelineStage.FRAGMENT_SHADER, PipelineStage.TRANSFER
            )

            val copyRegion = calloc(VkImageCopy::calloc, 1)
            copyRegion[0].srcSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1)
            copyRegion[0].srcOffset().set(0,0,0)
            copyRegion[0].dstSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1)
            copyRegion[0].dstOffset().set(0, 0, 0)
            copyRegion[0].extent().set(swapchain.imageExtent.width, swapchain.imageExtent.height, 1)

            vkCmdCopyImage(
                commandBuffer.vkHandle,
                storageImage.vkImageHandle,
                VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                swapchainImage,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                copyRegion
            )

            cmdTransitionImageLayout(
                commandBuffer, storageImage,
                AccessMask.TRANSFER_READ, AccessMask.SHADER_WRITE,
                ImageLayout.TRANSFER_SRC_OPTIMAL, ImageLayout.GENERAL,
                PipelineStage.TRANSFER, PipelineStage.FRAGMENT_SHADER
            )
        }
        vkCmdBeginRenderingKHR(commandBuffer.vkHandle, defaultRendering)
        vkCmdEndRenderingKHR(commandBuffer.vkHandle)

        // Transition Swapchain Image Layouts:
        val swapToPresentBarrier = calloc(VkImageMemoryBarrier::calloc, 1)
        with(swapToPresentBarrier[0]) {
            sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            pNext(0)
            srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            dstAccessMask(0)
            oldLayout(ImageLayout.TRANSFER_DST_OPTIMAL.vkValue)
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
        instanceDataBuffer.destroy()
        storageImage.destroy()
        objTransformBuffer.destroy()
        topLevelAS.destroy(device)
        bottomLevelAS.destroy(device)
        raygenShaderBindingTable.destroy()
        missShaderBindingTable.destroy()
        chitShaderBindingTable.destroy()
        raytracingPipeline.destroy(device)
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