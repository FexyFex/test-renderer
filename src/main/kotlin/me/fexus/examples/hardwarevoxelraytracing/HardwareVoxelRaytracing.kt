package me.fexus.examples.hardwarevoxelraytracing

import me.fexus.camera.CameraPerspective
import me.fexus.examples.hardwarevoxelraytracing.accelerationstructure.*
import me.fexus.math.mat.Mat4
import me.fexus.math.vec.Vec3
import me.fexus.memory.OffHeapSafeAllocator.Companion.runMemorySafe
import me.fexus.model.CubeModelPositionsOnly
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
import me.fexus.vulkan.extension.*
import me.fexus.vulkan.raytracing.*
import me.fexus.vulkan.raytracing.RaytracingShaderGroup.Companion.UNUSED
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


class HardwareVoxelRaytracing: VulkanRendererBase(createWindow()) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            HardwareVoxelRaytracing().start()
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
    private lateinit var indexBuffer: VulkanBuffer
    private lateinit var cameraBuffer: VulkanBuffer
    private lateinit var cobbleImage: VulkanImage
    private lateinit var storageImage: VulkanImage
    private lateinit var sampler: VulkanSampler
    private lateinit var objTransformBuffer: VulkanBuffer
    private lateinit var instanceDataBuffer: VulkanBuffer
    private lateinit var raytracingProperties: RaytracingProperties
    private lateinit var debugBuffer: VulkanBuffer
    private lateinit var aabbsBuffer: VulkanBuffer
    private val bottomLevelAS = AABBBottomAccelerationStructure()
    private val topLevelAS = AABBTopLevelAccelerationStructure()
    private val descriptorPool = DescriptorPool()
    private val descriptorSetLayout = DescriptorSetLayout()
    private val descriptorSet = DescriptorSet()
    private val raytracingPipeline = RaytracingPipeline()
    lateinit var raygenShaderBindingTable: VulkanBuffer
    lateinit var missShaderBindingTable: VulkanBuffer
    lateinit var closestHitShaderBindingTable: VulkanBuffer

    private val aabbPosition = Vec3(0f, 0f, -5f)
    private val aabbTransform = Mat4(1f).translate(aabbPosition)
    private val cobbleTexture = TextureLoader("textures/customvoxelraytracing/cobblestone.png")

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
        camera.fov = 30f
        camera.position = Vec3(0f, 0f, 0f)
        camera.zNear = 0.1f
        camera.zFar = 512f

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
        deviceUtil.assignName(this.descriptorPool.vkHandle, VK_OBJECT_TYPE_DESCRIPTOR_POOL, "default_desc_pool")
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
        deviceUtil.assignName(this.depthAttachment.vkImageHandle, VK_OBJECT_TYPE_IMAGE, "depth_image")
        // -- DEPTH ATTACHMENT IMAGE --

        // -- VERTEX BUFFER --
        val cubeVertexBufSize = CubeModelPositionsOnly.vertices.size * CubeModelPositionsOnly.Vertex.SIZE_BYTES
        val vertexBufferConfig = VulkanBufferConfiguration(
            cubeVertexBufSize.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.VERTEX_BUFFER + BufferUsage.TRANSFER_DST +
                    BufferUsage.SHADER_DEVICE_ADDRESS + BufferUsage.STORAGE_BUFFER +
                    BufferUsage.ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_KHR
        )
        this.vertexBuffer = bufferFactory.createBuffer(vertexBufferConfig)
        deviceUtil.assignName(this.vertexBuffer.vkBufferHandle, VK_OBJECT_TYPE_BUFFER, "vertex_buffer")
        // -- VERTEX BUFFER --

        // -- INDEX BUFFER --
        val cubeIndexBufSize = CubeModelPositionsOnly.indices.size * Int.SIZE_BYTES
        val indexBufferConfig = VulkanBufferConfiguration(
            cubeIndexBufSize.toLong(),
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.INDEX_BUFFER + BufferUsage.TRANSFER_DST +
                    BufferUsage.SHADER_DEVICE_ADDRESS + BufferUsage.STORAGE_BUFFER +
                    BufferUsage.ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_KHR
        )
        this.indexBuffer = bufferFactory.createBuffer(indexBufferConfig)
        deviceUtil.assignName(this.indexBuffer.vkBufferHandle, VK_OBJECT_TYPE_BUFFER, "index_buffer")
        // -- INDEX BUFFER --

        // -- TRANSFORM DATA BUFFER --
        val transformBufLayout = VulkanBufferConfiguration(
            64L,
            MemoryProperty.DEVICE_LOCAL,
            BufferUsage.SHADER_DEVICE_ADDRESS +
                    BufferUsage.ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_KHR +
                    BufferUsage.TRANSFER_DST
        )
        this.objTransformBuffer = bufferFactory.createBuffer(transformBufLayout)
        deviceUtil.assignName(this.objTransformBuffer.vkBufferHandle, VK_OBJECT_TYPE_BUFFER, "transform_buffer")
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
        deviceUtil.assignName(this.instanceDataBuffer.vkBufferHandle, VK_OBJECT_TYPE_BUFFER, "instance_data_buffer")
        // -- INSTANCE DATA BUFFER

        // -- CAMERA BUFFER --
        val cameraBufferLayout = VulkanBufferConfiguration(
            128L,
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.UNIFORM_BUFFER
        )
        this.cameraBuffer = bufferFactory.createBuffer(cameraBufferLayout)
        deviceUtil.assignName(this.cameraBuffer.vkBufferHandle, VK_OBJECT_TYPE_BUFFER, "camera_uniform_buffer")
        // -- CAMERA BUFFER --

        // -- TEXTURES --
        val cobbleImageConfig = VulkanImageConfiguration(
                ImageType.TYPE_2D, ImageViewType.TYPE_2D, ImageExtent3D(cobbleTexture.width, cobbleTexture.height, 1),
                1, 1, 1, ImageColorFormat.R8G8B8A8_SRGB, ImageTiling.OPTIMAL,
                ImageAspect.COLOR, ImageUsage.SAMPLED + ImageUsage.TRANSFER_DST, MemoryProperty.DEVICE_LOCAL
        )
        this.cobbleImage = imageFactory.createImage(cobbleImageConfig)
        deviceUtil.assignName(this.cobbleImage.vkImageHandle, VK_OBJECT_TYPE_IMAGE, "cobble_image")
        // -- TEXTURES --

        // -- STORAGE IMAGE --
        val storageImageLayout = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D, ImageExtent3D(swapchain.imageExtent, 1), 1,
            VK_SAMPLE_COUNT_1_BIT, 1, ImageColorFormat.B8G8R8A8_UNORM, ImageTiling.OPTIMAL, ImageAspect.COLOR,
            ImageUsage.TRANSFER_SRC + ImageUsage.STORAGE, MemoryProperty.DEVICE_LOCAL
        )
        this.storageImage = imageFactory.createImage(storageImageLayout)
        deviceUtil.assignName(this.storageImage.vkImageHandle, VK_OBJECT_TYPE_IMAGE, "storage_image")
        deviceUtil.runSingleTimeCommands { cmdBuf ->
            deviceUtil.cmdTransitionImageLayout(
                cmdBuf, this@HardwareVoxelRaytracing.storageImage,
                AccessMask.NONE, AccessMask.SHADER_READ,
                ImageLayout.UNDEFINED, ImageLayout.GENERAL,
                PipelineStage.TOP_OF_PIPE, PipelineStage.FRAGMENT_SHADER
            )
        }
        // -- STORAGE IMAGE --

        // -- SAMPLER --
        val samplerLayout = VulkanSamplerLayout(AddressMode.REPEAT, 1, Filtering.NEAREST)
        this.sampler = imageFactory.createSampler(samplerLayout)
        deviceUtil.assignName(this.sampler.vkHandle, VK_OBJECT_TYPE_SAMPLER, "soompler")
        // -- SAMPLER --

        // -- DEBUG BUFFER --
        val debugBufferConfig = VulkanBufferConfiguration(
            128L,
            MemoryProperty.HOST_COHERENT + MemoryProperty.HOST_VISIBLE,
            BufferUsage.STORAGE_BUFFER + BufferUsage.SHADER_DEVICE_ADDRESS
        )
        this.debugBuffer = deviceUtil.createBuffer(debugBufferConfig)
        deviceUtil.assignName(this.debugBuffer.vkBufferHandle, VK_OBJECT_TYPE_BUFFER, "debug_buffer")
        // -- DEBUG BUFFER --

        // -- AABBS BUFFER --
        val aabbsBufferConfig = VulkanBufferConfiguration(
                AABB.SIZE_BYTES * Scene.aabbs.size.toLong(),
                MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
                BufferUsage.STORAGE_BUFFER + BufferUsage.SHADER_DEVICE_ADDRESS
        )
        this.aabbsBuffer = deviceUtil.createBuffer(aabbsBufferConfig)
        deviceUtil.assignName(this.aabbsBuffer.vkBufferHandle, VK_OBJECT_TYPE_BUFFER, "aabbs_buffer")
        // -- AABBS BUFFER --

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
                DescriptorSetLayoutBinding(
                    3, 1,
                    DescriptorType.STORAGE_BUFFER,
                    ShaderStage.RAYGEN + ShaderStage.CLOSEST_HIT + ShaderStage.ANY_HIT + ShaderStage.INTERSECTION,
                    DescriptorSetLayoutBindingFlag.NONE
                ),
                DescriptorSetLayoutBinding(
                        4, 1,
                        DescriptorType.STORAGE_BUFFER,
                        ShaderStage.RAYGEN + ShaderStage.INTERSECTION,
                        DescriptorSetLayoutBindingFlag.NONE
                )
            )
        )
        this.descriptorSetLayout.create(device, setLayoutPlan)
        // -- DESCRIPTOR SET LAYOUT --
    }

    private fun initDescriptors() {
        // VERTEX BUFFER
        val cubeVertexBufSize = CubeModelPositionsOnly.vertices.size * CubeModelPositionsOnly.Vertex.SIZE_BYTES
        val vertexBufferData = ByteBuffer.allocate(cubeVertexBufSize)
        vertexBufferData.order(ByteOrder.LITTLE_ENDIAN)
        CubeModelPositionsOnly.vertices.forEachIndexed { vertIndex, vert ->
            vert.toFloatArray().forEachIndexed { flIndex, fl ->
                val offset = vertIndex * CubeModelPositionsOnly.Vertex.SIZE_BYTES + (flIndex * Float.SIZE_BYTES)
                vertexBufferData.putFloat(offset, fl)
            }
        }
        deviceUtil.stagingCopy(vertexBufferData, this.vertexBuffer, 0L, 0L, cubeVertexBufSize.toLong())
        // VERTEX BUFFER

        // INDEX BUFFER
        val cubeIndexBufferSize = CubeModelPositionsOnly.indices.size * Int.SIZE_BYTES
        val indexByteBuffer = ByteBuffer.allocate(cubeIndexBufferSize)
        indexByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        CubeModelPositionsOnly.indices.forEachIndexed { iIndex, cubeIndex ->
            val offset = iIndex * Int.SIZE_BYTES
            indexByteBuffer.putInt(offset, cubeIndex)
        }
        deviceUtil.stagingCopy(indexByteBuffer, this.indexBuffer, 0L, 0L, cubeIndexBufferSize.toLong())
        // INDEX BUFFER

        // TRANSFORM BUFFER
        val transformByteBuffer = ByteBuffer.allocate(Mat4.SIZE_BYTES)
        transformByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        aabbTransform.toByteBufferColumnMajor(transformByteBuffer, 0)
        deviceUtil.stagingCopy(transformByteBuffer, this.objTransformBuffer, 0L, 0L, Mat4.SIZE_BYTES.toLong())
        // TRANSFORM BUFFER

        // AABBS BUFFER
        val aabbsBufferSize = AABB.SIZE_BYTES * Scene.aabbs.size
        val aabbsByteBuffer = ByteBuffer.allocate(aabbsBufferSize)
        aabbsByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        Scene.aabbs.forEachIndexed { index, aabb ->
            val offset = index * AABB.SIZE_BYTES
            aabb.toByteBuffer(aabbsByteBuffer, offset)
        }
        this.aabbsBuffer.put(0, aabbsByteBuffer)
        // AABBS BUFFER

        initBottomLevelAccelerationStructure()

        initTopLevelAccelerationStructure()

        // TEXTURE IMAGE
        val stagingImageBufLayout = VulkanBufferConfiguration(
            cobbleTexture.imageSize,
            MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT,
            BufferUsage.TRANSFER_SRC
        )
        val stagingBufImg = bufferFactory.createBuffer(stagingImageBufLayout)
        stagingBufImg.put(0, cobbleTexture.pixels, 0)
        deviceUtil.assignName(stagingBufImg.vkBufferHandle, VK_OBJECT_TYPE_BUFFER, "img_staging_buf_cobble")
        runMemorySafe {
            val cmdBuf = deviceUtil.beginSingleTimeCommandBuffer()
            deviceUtil.cmdTransitionImageLayout(
                cmdBuf, this@HardwareVoxelRaytracing.cobbleImage,
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
                stagingBufImg.vkBufferHandle, this@HardwareVoxelRaytracing.cobbleImage.vkImageHandle,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copyRegions
            )

            deviceUtil.cmdTransitionImageLayout(
                cmdBuf, this@HardwareVoxelRaytracing.cobbleImage,
                AccessMask.TRANSFER_WRITE, AccessMask.SHADER_READ,
                ImageLayout.TRANSFER_DST_OPTIMAL, ImageLayout.SHADER_READ_ONLY_OPTIMAL,
                PipelineStage.TRANSFER, PipelineStage.FRAGMENT_SHADER
            )

            deviceUtil.endSingleTimeCommandBuffer(cmdBuf)
        }
        stagingBufImg.destroy()
        // TEXTURE IMAGE

        // DEBUG BUFFER
        debugBuffer.set(0, 0, 128L)
        // DEBUG BUFFER
    }

    private fun initBottomLevelAccelerationStructure() {
        val blasConfig = AABBBlasConfiguration(Scene.aabbs)
        this.bottomLevelAS.createAndBuild(deviceUtil, blasConfig)
        deviceUtil.assignName(this.bottomLevelAS.vkHandle, VK_OBJECT_TYPE_ACCELERATION_STRUCTURE_KHR, "bottom_level_as")
    }

    private fun initTopLevelAccelerationStructure() {
        // INSTANCE DATA BUFFER
        val transformMatrix = TransformMatrix(aabbTransform)
        val instance = AccelerationStructureInstance(
            transformMatrix, 0, 0xFF, 0,
            VK_GEOMETRY_INSTANCE_FORCE_OPAQUE_BIT_KHR, bottomLevelAS.deviceAddress
        )
        val instanceByteBuffer = ByteBuffer.allocate(AccelerationStructureInstance.SIZE_BYTES)
        instanceByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        instance.toByteBuffer(instanceByteBuffer, 0)
        deviceUtil.stagingCopy(instanceByteBuffer, this.instanceDataBuffer, 0L, 0L, AccelerationStructureInstance.SIZE_BYTES.toLong())
        // INSTANCE DATA BUFFER

        val tlasConfig = AABBTLASConfiguration(listOf(instanceDataBuffer))
        this.topLevelAS.createAndBuild(deviceUtil, tlasConfig)

        deviceUtil.assignName(this.topLevelAS.vkHandle, VK_OBJECT_TYPE_ACCELERATION_STRUCTURE_KHR, "top_level_as")
    }

    private fun createRaytracingPipeline() {
        val raygenShaderCode = ClassLoader.getSystemResource("shaders/hardwarevoxelraytracing/rgen.spv").readBytes()
        val missShaderCode = ClassLoader.getSystemResource("shaders/hardwarevoxelraytracing/rmiss.spv").readBytes()
        val chitShaderCode = ClassLoader.getSystemResource("shaders/hardwarevoxelraytracing/rchit.spv").readBytes()
        val intShaderCode = ClassLoader.getSystemResource("shaders/hardwarevoxelraytracing/rint.spv").readBytes()
        val config = RaytracingPipelineConfiguration(
            listOf(
                RaytracingShaderStage(ShaderStage.RAYGEN, raygenShaderCode),
                RaytracingShaderStage(ShaderStage.MISS, missShaderCode),
                RaytracingShaderStage(ShaderStage.CLOSEST_HIT, chitShaderCode),
                RaytracingShaderStage(ShaderStage.INTERSECTION, intShaderCode)
            ),
            listOf(
                RaytracingShaderGroup(RaytracingShaderGroupType.GENERAL, 0, UNUSED, UNUSED, UNUSED),
                RaytracingShaderGroup(RaytracingShaderGroupType.GENERAL, 1, UNUSED, UNUSED, UNUSED),
                RaytracingShaderGroup(RaytracingShaderGroupType.PROCEDURAL_HIT, UNUSED, 2, UNUSED, 3)
            ),
            PushConstantsLayout(128, shaderStages = ShaderStage.RAYGEN + ShaderStage.CLOSEST_HIT)
        )
        this.raytracingPipeline.create(device, descriptorSetLayout, config)
        deviceUtil.assignName(this.raytracingPipeline.vkHandle, VK_OBJECT_TYPE_PIPELINE, "raytracing_pipeline")
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

        this@HardwareVoxelRaytracing.raytracingProperties = RaytracingProperties(
            raytracingProps.shaderGroupHandleSize(),
            raytracingProps.shaderGroupHandleAlignment()
        )

        val handleAlignmentSize = raytracingProperties.shaderGroupHandleAlignment
        val groupCount = raytracingPipeline.shadergroupCount
        val sbtSize = groupCount * handleAlignmentSize

        val pData = allocate(sbtSize)
        vkGetRayTracingShaderGroupHandlesKHR(device.vkHandle, raytracingPipeline.vkHandle, 0, groupCount, pData)

        val bufferUsage = BufferUsage.SHADER_BINDING_TABLE + BufferUsage.SHADER_DEVICE_ADDRESS
        val memoryProperties = MemoryProperty.HOST_VISIBLE + MemoryProperty.HOST_COHERENT
        val bufferConfig = VulkanBufferConfiguration(handleAlignmentSize.toLong(), memoryProperties, bufferUsage)

        this@HardwareVoxelRaytracing.raygenShaderBindingTable = bufferFactory.createBuffer(bufferConfig)
        this@HardwareVoxelRaytracing.missShaderBindingTable = bufferFactory.createBuffer(bufferConfig)
        this@HardwareVoxelRaytracing.closestHitShaderBindingTable = bufferFactory.createBuffer(bufferConfig)

        this@HardwareVoxelRaytracing.raygenShaderBindingTable.put(0, pData, handleAlignmentSize * 0, handleAlignmentSize)
        this@HardwareVoxelRaytracing.missShaderBindingTable.put(0, pData, handleAlignmentSize * 1, handleAlignmentSize)
        this@HardwareVoxelRaytracing.closestHitShaderBindingTable.put(0, pData, handleAlignmentSize * 2, handleAlignmentSize)
    }

    private fun writeDescriptorSets() {
        // -- DESCRIPTOR SET --
        this.descriptorSet.create(device, descriptorPool, descriptorSetLayout)

        val descWriteAccStruct = DescriptorAccelerationStructureWrite(
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
        val debugBufWrite = DescriptorBufferWrite(
            3, DescriptorType.STORAGE_BUFFER, 1, this.descriptorSet, 0,
            listOf(DescriptorBufferInfo(debugBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE))
        )
        val aabbsBufWrite = DescriptorBufferWrite(
                4, DescriptorType.STORAGE_BUFFER, 1, this.descriptorSet, 0,
                listOf(DescriptorBufferInfo(aabbsBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE))
        )

        this.descriptorSet.update(device, descWriteAccStruct, storageImageWrite, uniformBufWrite, debugBufWrite, aabbsBufWrite)
        // -- DESCRIPTOR SET --
    }

    override fun recordFrame(preparation: FramePreparation, delta: Float): FrameSubmitData = runMemorySafe {
        time += delta

        handleInput()

        val origin = Vec3(debugBuffer.getFloat(0), debugBuffer.getFloat(4), debugBuffer.getFloat(8))
        val dir = Vec3(debugBuffer.getFloat(12), debugBuffer.getFloat(16), debugBuffer.getFloat(20))
        val hitExecuted = debugBuffer.getFloat(40) == 1f

        //println("---------------------")
        //println("Origin: $origin")
        //println("Direction: $dir")
        //println("hit shader executed: $hitExecuted")

        val view = camera.calculateView().inverse()
        //val proj = camera.calculateProjection().inverse()
        val proj = Mat4(
            -11.387255f, 0.0f, -0.0f, 0.0f,
            0.0f, -6.405331f, 0.0f, -0.0f,
            -0.0f, -0.0f, -0.0f, -1.0f,
            0.0f, -0.0f, -4.999023f, 5.000976f
        )
        //println("-----------------------------")
        //println(view)
        //println(proj)
        val data = ByteBuffer.allocate(128)
        data.order(ByteOrder.LITTLE_ENDIAN)
        view.toByteBufferColumnMajor(data, 0)
        proj.toByteBufferColumnMajor(data, 64)
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

            val handleSizeAligned = alignedSize(raytracingProperties.shaderGroupHandleSize, raytracingProperties.shaderGroupHandleAlignment).toLong()

            val rayGenEntry = calloc(VkStridedDeviceAddressRegionKHR::calloc) {
                deviceAddress(raygenShaderBindingTable.getDeviceAddress())
                stride(handleSizeAligned)
                size(handleSizeAligned)
            }
            val missEntry = calloc(VkStridedDeviceAddressRegionKHR::calloc) {
                deviceAddress(missShaderBindingTable.getDeviceAddress())
                stride(handleSizeAligned)
                size(handleSizeAligned)
            }
            val ahitEntry = calloc(VkStridedDeviceAddressRegionKHR::calloc) {
                deviceAddress(closestHitShaderBindingTable.getDeviceAddress())
                stride(handleSizeAligned)
                size(handleSizeAligned)
            }
            val callableShaderEntry = calloc(VkStridedDeviceAddressRegionKHR::calloc) {}

            val pPushConstants = allocate(128)

            vkCmdBindPipeline(commandBuffer.vkHandle, bindPoint, raytracingPipeline.vkHandle)
            vkCmdPushConstants(
                commandBuffer.vkHandle,
                raytracingPipeline.vkLayoutHandle,
                (ShaderStage.ANY_HIT + ShaderStage.RAYGEN).vkBits,
                0,
                pPushConstants
            )
            vkCmdTraceRaysKHR(
                commandBuffer.vkHandle,
                rayGenEntry, missEntry, ahitEntry, callableShaderEntry,
                swapchain.imageExtent.width, swapchain.imageExtent.height, 1
            )

            deviceUtil.cmdTransitionImageLayout(
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

            deviceUtil.cmdTransitionImageLayout(
                commandBuffer, storageImage,
                AccessMask.TRANSFER_READ, AccessMask.SHADER_WRITE,
                ImageLayout.TRANSFER_SRC_OPTIMAL, ImageLayout.GENERAL,
                PipelineStage.TRANSFER, PipelineStage.FRAGMENT_SHADER
            )
        }

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

    private fun alignedSize(value: Int, alignment: Int): Int {
        return (value + alignment - 1) and (alignment - 1).inv()
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
        storageImage.destroy()
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

        val storageImageLayout = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D, ImageExtent3D(swapchain.imageExtent, 1), 1,
            VK_SAMPLE_COUNT_1_BIT, 1, ImageColorFormat.B8G8R8A8_UNORM, ImageTiling.OPTIMAL, ImageAspect.COLOR,
            ImageUsage.TRANSFER_SRC + ImageUsage.STORAGE, MemoryProperty.DEVICE_LOCAL
        )
        this.storageImage = imageFactory.createImage(storageImageLayout)
        deviceUtil.assignName(this.storageImage.vkImageHandle, VK_OBJECT_TYPE_IMAGE, "storage_image")
        deviceUtil.runSingleTimeCommands { cmdBuf ->
            deviceUtil.cmdTransitionImageLayout(
                cmdBuf, this@HardwareVoxelRaytracing.storageImage,
                AccessMask.NONE, AccessMask.SHADER_READ,
                ImageLayout.UNDEFINED, ImageLayout.GENERAL,
                PipelineStage.TOP_OF_PIPE, PipelineStage.FRAGMENT_SHADER
            )
        }

        writeDescriptorSets()
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
        debugBuffer.destroy()
        aabbsBuffer.destroy()
        raytracingPipeline.destroy(device)
        cobbleImage.destroy()
        vertexBuffer.destroy()
        indexBuffer.destroy()
        sampler.destroy(device)
        cameraBuffer.destroy()
        depthAttachment.destroy()
        descriptorPool.destroy(device)
        descriptorSetLayout.destroy(device)
        super.destroy()
    }
}