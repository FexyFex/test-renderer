package me.fexus.examples.surroundsound

import me.fexus.audio.AudioClip
import me.fexus.audio.FexAudioSystem
import me.fexus.audio.libraries.AudioLibraryOpenAL
import me.fexus.camera.CameraPerspective
import me.fexus.examples.Globals
import me.fexus.examples.surroundsound.monolith.Disc
import me.fexus.examples.surroundsound.monolith.HummingMonolith
import me.fexus.examples.surroundsound.monolith.MonolithModel
import me.fexus.examples.surroundsound.sound.SoundLibrary
import me.fexus.examples.surroundsound.sound.SoundRegistry
import me.fexus.math.clamp
import me.fexus.math.mat.Mat4
import me.fexus.math.rad
import me.fexus.math.vec.IVec2
import me.fexus.math.vec.Vec3
import me.fexus.memory.runMemorySafe
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
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.image.*
import me.fexus.vulkan.descriptors.image.aspect.ImageAspect
import me.fexus.vulkan.descriptors.image.usage.ImageUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
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
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRDynamicRendering.*
import org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR
import org.lwjgl.vulkan.VK12.*
import java.nio.ByteBuffer
import java.nio.ByteOrder


class SurroundSound: VulkanRendererBase(createWindow()), InputEventSubscriber {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SurroundSound().start()
        }

        private fun createWindow() = Window("Surround Sound via OpenAL") {
            windowVisible()
            enableResizable()
            setInitialWindowSize(1067,600)
            enableDecoration()
            setInitialWindowPosition(400, 300)
            enableAutoIconify()
        }

        private fun Boolean.toInt(): Int = if (this) 1 else 0
        private const val FIELD_SIZE = 24
    }

    private var time: Double = 0.0

    private val camera = CameraPerspective(window.aspect)
    private val player = Player()

    private val audioSystem = FexAudioSystem().initWithLibrary<AudioLibraryOpenAL>()
    private val soundRegistry = SoundRegistry()

    private val monolithModel = MonolithModel.load()
    private val discModel = Disc.loadModel()
    private val monoliths = mutableListOf<HummingMonolith>()
    private val monolithOuterPipeline = GraphicsPipeline()
    private val monolithInnerPipeline = GraphicsPipeline()
    private val discPipeline = GraphicsPipeline()
    private lateinit var monolithVertexBuffer: VulkanBuffer
    private lateinit var discVertexBuffer: VulkanBuffer

    private val ground = Ground(FIELD_SIZE, FIELD_SIZE)
    private val groundPipeline = GraphicsPipeline()

    private val meshUploader = MeshUploader(deviceUtil)

    private lateinit var groundVertexBuffer: VulkanBuffer
    private lateinit var groundIndexBuffer: VulkanBuffer

    private val cubemap = Cubemap(deviceUtil)

    private lateinit var depthAttachment: VulkanImage
    private lateinit var cameraBuffers: Array<VulkanBuffer>
    private lateinit var sampler: VulkanSampler
    private lateinit var monolithDataBuffer: VulkanBuffer

    private val descriptorPool = DescriptorPool()
    private val descriptorSetLayout = DescriptorSetLayout()
    private val descriptorSets = Array(Globals.FRAMES_TOTAL) { DescriptorSet() }

    private val inputHandler = InputHandler(window)

    private var trapMouse: Boolean = true


    fun start() {
        initVulkanCore()
        initObjects()
        startRenderLoop(window, this)
    }

    override fun onMouseMoved(newPosition: IVec2) {
        if (!trapMouse) return

        val windowSize = window.extent2D
        val middle = windowSize / 2
        val dist = middle - newPosition

        player.viewDirection.x -= dist.y.toFloat() / 10f
        player.viewDirection.x = player.viewDirection.x.clamp(-90f, 90f)
        player.viewDirection.y -= dist.x.toFloat() / 10f

        window.setCursorPos(middle)
    }

    override fun onKeyPressed(key: Key) {
        if (key == Key.ESC) trapMouse = !trapMouse
        if (key == Key.P) monoliths.first().play()
    }

    private fun initObjects() {
        subscribe(inputHandler)
        ground.generateNewHeightMap(103L)
        player.position.x = FIELD_SIZE * 0.5f
        player.position.z = FIELD_SIZE * 0.5f
        calculatePlayerY()

        createAttachments()

        cubemap.initImageArray()

        createMonolithMeshBuffers()

        val groundMesh = ground.buildMesh()
        this.groundVertexBuffer = meshUploader.uploadBuffer(groundMesh.vertexBuffer, BufferUsage.VERTEX_BUFFER)
        this.groundIndexBuffer = meshUploader.uploadBuffer(groundMesh.indexBuffer, BufferUsage.INDEX_BUFFER)

        // -- CAMERA BUFFER --
        val cameraBufferConfig = VulkanBufferConfiguration(
            144L,
            MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.UNIFORM_BUFFER
        )
        this.cameraBuffers = Array(Globals.FRAMES_TOTAL) { deviceUtil.createBuffer(cameraBufferConfig) }
        // -- CAMERA BUFFER --

        // -- SAMPLER --
        val samplerConfig = VulkanSamplerConfiguration(AddressMode.CLAMP_TO_EDGE, 1, Filtering.LINEAR)
        this.sampler = deviceUtil.createSampler(samplerConfig)
        // -- SAMPLER --

        val monolithBufferConfig = VulkanBufferConfiguration(
            HummingMonolith.SIZE_BYTES * 5L, // we won't need more than 5 monoliths
            MemoryPropertyFlag.HOST_COHERENT + MemoryPropertyFlag.HOST_VISIBLE,
            BufferUsage.STORAGE_BUFFER
        )
        this.monolithDataBuffer = deviceUtil.createBuffer(monolithBufferConfig)

        createDescriptorStuff()

        cubemap.init(descriptorSetLayout)
        createMonolithPipelines()
        createMonoliths()

        val groundPipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC4, 0)
            ),
            PushConstantsLayout(128),
            ClassLoader.getSystemResource("shaders/surroundsound/ground/ground_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/surroundsound/ground/ground_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.VIEWPORT, DynamicState.SCISSOR),
            cullMode = CullMode.NONE
        )
        this.groundPipeline.create(device, listOf(descriptorSetLayout), groundPipelineConfig)
    }

    private fun createMonolithMeshBuffers() {
        this.monolithVertexBuffer = meshUploader.uploadBuffer(
            monolithModel.buffer,
            BufferUsage.VERTEX_BUFFER + BufferUsage.INDEX_BUFFER
        )

        this.discVertexBuffer = meshUploader.uploadBuffer(
            discModel.byteBuffer, BufferUsage.VERTEX_BUFFER + BufferUsage.INDEX_BUFFER
        )
    }

    private fun createMonolithPipelines() {
        val outerPipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC3, 0, binding = 0),
                VertexAttribute(1, VertexAttributeFormat.VEC2, 0, binding = 1),
                VertexAttribute(2, VertexAttributeFormat.VEC3, 0, binding = 2)
            ),
            PushConstantsLayout(128),
            ClassLoader.getSystemResource("shaders/surroundsound/monolith/monolith_outer_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/surroundsound/monolith/monolith_outer_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.VIEWPORT, DynamicState.SCISSOR),
            vertexInputBindings = listOf(
                VertexInputBinding(0, VertexAttributeFormat.VEC3.size, VertexInputRate.VERTEX),
                VertexInputBinding(1, VertexAttributeFormat.VEC2.size, VertexInputRate.VERTEX),
                VertexInputBinding(2, VertexAttributeFormat.VEC3.size, VertexInputRate.VERTEX)
            ),
        )
        this.monolithOuterPipeline.create(device, listOf(descriptorSetLayout), outerPipelineConfig)

        val innerPipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC3, 0, binding = 0),
                VertexAttribute(1, VertexAttributeFormat.VEC2, 12, binding = 1),
                VertexAttribute(2, VertexAttributeFormat.VEC3, 20, binding = 2)
            ),
            PushConstantsLayout(128),
            ClassLoader.getSystemResource("shaders/surroundsound/monolith/monolith_inner_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/surroundsound/monolith/monolith_inner_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.VIEWPORT, DynamicState.SCISSOR),
            vertexInputBindings = listOf(
                VertexInputBinding(0, VertexAttributeFormat.VEC3.size, VertexInputRate.VERTEX),
                VertexInputBinding(1, VertexAttributeFormat.VEC2.size, VertexInputRate.VERTEX),
                VertexInputBinding(2, VertexAttributeFormat.VEC3.size, VertexInputRate.VERTEX)
            ),
        )
        this.monolithInnerPipeline.create(device, listOf(descriptorSetLayout), innerPipelineConfig)

        val discPipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC3, 0, binding = 0),
                VertexAttribute(1, VertexAttributeFormat.VEC2, 12, binding = 1),
                VertexAttribute(2, VertexAttributeFormat.VEC3, 20, binding = 2)
            ),
            PushConstantsLayout(128),
            ClassLoader.getSystemResource("shaders/surroundsound/monolith/disc/disc_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/surroundsound/monolith/disc/disc_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.VIEWPORT, DynamicState.SCISSOR),
            vertexInputBindings = listOf(
                VertexInputBinding(0, VertexAttributeFormat.VEC3.size, VertexInputRate.VERTEX),
                VertexInputBinding(1, VertexAttributeFormat.VEC2.size, VertexInputRate.VERTEX),
                VertexInputBinding(2, VertexAttributeFormat.VEC3.size, VertexInputRate.VERTEX)
            ), blendEnable = true
        )
        this.discPipeline.create(device, listOf(descriptorSetLayout), discPipelineConfig)
    }

    private fun createMonoliths() {
        val buf = ByteBuffer.allocate(this.monolithDataBuffer.config.size.toInt())
        buf.order(ByteOrder.LITTLE_ENDIAN)

        val pos = Vec3(4f, 3f, 6f)
        pos.y = ground.getHeightAt(pos.x, pos.z) + 2.4f
        val rot = Vec3(0.03f, 60f, 0.1f)
        val sound = soundRegistry.loadSound(SoundLibrary.FIRST_REVOLUTION)
        val clip = audioSystem.createClip(AudioClip.Type.STREAMING, sound)
        val monolith1 = HummingMonolith(pos, rot, audioSystem, clip)
        monolith1.intoByteBuffer(buf, 0)
        this.monoliths.add(monolith1)

        this.monolithDataBuffer.put(0, buf)
    }

    private fun createAttachments() {
        val depthAttachmentImageLayout = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D,
            ImageExtent3D(swapchain.imageExtent, 1),
            1, 1, 1,
            ImageColorFormat.D32_SFLOAT, ImageTiling.OPTIMAL,
            ImageAspect.DEPTH, ImageUsage.DEPTH_STENCIL_ATTACHMENT,
            MemoryPropertyFlag.DEVICE_LOCAL
        )
        this.depthAttachment = imageFactory.createImage(depthAttachmentImageLayout)
    }

    private fun createDescriptorStuff() {
        // Descriptor Sets and Pipeline
        val poolPlan = DescriptorPoolPlan(
            Globals.FRAMES_TOTAL, DescriptorPoolCreateFlag.FREE_DESCRIPTOR_SET,
            listOf(
                DescriptorPoolSize(DescriptorType.UNIFORM_BUFFER, Globals.FRAMES_TOTAL),
                DescriptorPoolSize(DescriptorType.STORAGE_BUFFER, 16 * Globals.FRAMES_TOTAL),
                DescriptorPoolSize(DescriptorType.SAMPLED_IMAGE, 16 * Globals.FRAMES_TOTAL),
                DescriptorPoolSize(DescriptorType.SAMPLER, Globals.FRAMES_TOTAL)
            )
        )
        this.descriptorPool.create(device, poolPlan)

        val setLayoutPlan = DescriptorSetLayoutPlan(
            DescriptorSetLayoutCreateFlag.NONE,
            listOf(
                DescriptorSetLayoutBinding(
                    0, 1, DescriptorType.UNIFORM_BUFFER,
                    ShaderStage.VERTEX, DescriptorSetLayoutBindingFlag.NONE
                ),
                DescriptorSetLayoutBinding(
                    1, 16, DescriptorType.SAMPLED_IMAGE,
                    ShaderStage.FRAGMENT, DescriptorSetLayoutBindingFlag.PARTIALLY_BOUND
                ),
                DescriptorSetLayoutBinding(
                    2, 1, DescriptorType.SAMPLER,
                    ShaderStage.FRAGMENT, DescriptorSetLayoutBindingFlag.NONE
                ),
                DescriptorSetLayoutBinding(
                    3, 16, DescriptorType.STORAGE_BUFFER,
                    ShaderStage.VERTEX, DescriptorSetLayoutBindingFlag.PARTIALLY_BOUND
                )
            )
        )
        this.descriptorSetLayout.create(device, setLayoutPlan)

        // Update Descrfiptor Sets
        descriptorSets.forEachIndexed { index, set ->
            set.create(device, descriptorPool, descriptorSetLayout)

            val descWriteCameraBuf = DescriptorBufferWrite(
                0, DescriptorType.UNIFORM_BUFFER, 1, set, 0,
                listOf(
                    DescriptorBufferInfo(cameraBuffers[index].vkBufferHandle, 0L, VK_WHOLE_SIZE)
                )
            )
            val descWriteTextures = DescriptorImageWrite(
                1, DescriptorType.SAMPLED_IMAGE, 1, set, 0,
                listOf(
                    DescriptorImageInfo(0L, cubemap.imageArray.vkImageViewHandle, ImageLayout.SHADER_READ_ONLY_OPTIMAL),
                )
            )
            val descWriteSampler = DescriptorImageWrite(
                2, DescriptorType.SAMPLER, 1, set, 0,
                listOf(
                    DescriptorImageInfo(this.sampler.vkHandle, 0L, ImageLayout.SHADER_READ_ONLY_OPTIMAL)
                )
            )
            val descWriteStorageBuffers = DescriptorBufferWrite(
                3, DescriptorType.STORAGE_BUFFER, 1, set, 0,
                listOf(DescriptorBufferInfo(monolithDataBuffer.vkBufferHandle, 0L, VK_WHOLE_SIZE))
            )

            set.update(device, descWriteCameraBuf, descWriteTextures, descWriteSampler, descWriteStorageBuffers)
        }
    }

    private fun handleInput() {
        val xMove = inputHandler.isKeyDown(Key.A).toInt() - inputHandler.isKeyDown(Key.D).toInt()
        val zMove = inputHandler.isKeyDown(Key.W).toInt() - inputHandler.isKeyDown(Key.S).toInt()

        if (xMove == 0 && zMove == 0) return

        val moveSpeed = 0.2f

        val rotM = Mat4(1f)
            .rotate(0f, Vec3(1f, 0f, 0f))
            .rotate(player.viewDirection.y.rad, Vec3(0f, 1f, 0f))
            .rotate(player.viewDirection.z.rad, Vec3(0f, 0f, 1f))
        val transM = Mat4(1f).translate(Vec3(xMove * moveSpeed, 0, zMove * moveSpeed)) / rotM
        val forward = Vec3(transM[3][0], transM[3][1], transM[3][2]).normalize() / 5f

        player.position.x -= forward.x * 0.35f
        player.position.z -= forward.z * 0.35f

        calculatePlayerY()

        audioSystem.setListenerPosition(player.position)
    }

    private fun calculatePlayerY() {
        try {
            player.position.y = -ground.getHeightAt(player.position.x, player.position.z) + 1f
        } catch(_: Exception) {
            player.position.y = 1f
        }
    }

    private fun updateBuffers() {
        camera.position = -player.position
        camera.rotation = player.viewDirection

        val view = camera.calculateView()
        val proj = camera.calculateReverseZProjection()
        val data = ByteBuffer.allocate(144)
        data.order(ByteOrder.LITTLE_ENDIAN)
        view.toByteBufferColumnMajor(data, 0)
        proj.toByteBufferColumnMajor(data, 64)
        data.putFloat(128, time.toFloat())

        cameraBuffers[currentFrame].put(0, data)

        val inverseView = view.inverse()
        audioSystem.setListenerOrientation(-Vec3(inverseView[2].xyz), Vec3(0f, 1f, 0f))
    }

    override fun recordFrame(preparation: FramePreparation, delta: Float): FrameSubmitData = runMemorySafe {
        time += delta
        handleInput()
        updateBuffers()

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
            depthStencil().depth(0.0f)
            depthStencil().stencil(0)
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
            viewport[0].set(0f, 0f, width.toFloat(), height.toFloat(), 0f, 1f)

            val scissor = calloc(VkRect2D::calloc, 1)
            scissor[0].offset().x(0).y(0)
            scissor[0].extent().width(width).height(height)

            val pDescriptorSets = allocateLong(1)
            pDescriptorSets.put(0, descriptorSets[currentFrame].vkHandle)

            val pVertexBuffers = allocateLongValues(cubemap.vertexBuffer.vkBufferHandle)
            val pOffsets = allocateLongValues(0L)

            val pPushConstants = allocate(128)

            val bindPointGraphics = VK_PIPELINE_BIND_POINT_GRAPHICS

            vkCmdSetViewport(commandBuffer.vkHandle, 0, viewport)
            vkCmdSetScissor(commandBuffer.vkHandle, 0, scissor)
            vkCmdBindDescriptorSets(
                commandBuffer.vkHandle, bindPointGraphics, groundPipeline.vkLayoutHandle,
                0, pDescriptorSets, null
            )

            // cubemap skybox
            pPushConstants.putInt(0, 0)
            vkCmdBindPipeline(commandBuffer.vkHandle, bindPointGraphics, cubemap.pipeline.vkHandle)
            vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
            vkCmdPushConstants(commandBuffer.vkHandle, cubemap.pipeline.vkLayoutHandle, ShaderStage.BOTH.vkBits, 0, pPushConstants)
            vkCmdDraw(commandBuffer.vkHandle, cubemap.vertexCount, 1, 0, 0)

            // ground
            pVertexBuffers.put(0, groundVertexBuffer.vkBufferHandle)
            vkCmdBindPipeline(commandBuffer.vkHandle, bindPointGraphics, groundPipeline.vkHandle)
            vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
            vkCmdBindIndexBuffer(commandBuffer.vkHandle, groundIndexBuffer.vkBufferHandle, 0L, VK_INDEX_TYPE_UINT32)
            vkCmdPushConstants(commandBuffer.vkHandle, groundPipeline.vkLayoutHandle, ShaderStage.BOTH.vkBits, 0, pPushConstants)
            val indexCount = ground.currentMesh.indexBuffer.capacity() / Int.SIZE_BYTES
            vkCmdDrawIndexed(commandBuffer.vkHandle, indexCount, 1, 0, 0, 0)


            // Monoliths
            val pMonolithBuffers = allocateLongValues(
                monolithVertexBuffer.vkBufferHandle,
                monolithVertexBuffer.vkBufferHandle,
                monolithVertexBuffer.vkBufferHandle,
            )

            // outer
            val pMonolithOuterOffsets = allocateLongValues(
                monolithModel.outerMesh.posOffset.toLong(),
                monolithModel.outerMesh.uvOffset.toLong(),
                monolithModel.outerMesh.normalOffset.toLong()
            )
            pPushConstants.putInt(0, 0)
            vkCmdBindPipeline(commandBuffer.vkHandle, bindPointGraphics, monolithOuterPipeline.vkHandle)
            vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pMonolithBuffers, pMonolithOuterOffsets)
            vkCmdBindIndexBuffer(commandBuffer.vkHandle, monolithVertexBuffer.vkBufferHandle, monolithModel.outerMesh.indexOffset.toLong(), VK_INDEX_TYPE_UINT16)
            vkCmdPushConstants(commandBuffer.vkHandle, monolithOuterPipeline.vkLayoutHandle, ShaderStage.BOTH.vkBits, 0, pPushConstants)
            vkCmdDrawIndexed(
                commandBuffer.vkHandle,
                monolithModel.outerMesh.indexCount,
                1,
                0,
                0,
                0
            )

            // inner
            val pMonolithInnerOffsets = allocateLongValues(
                monolithModel.innerMesh.posOffset.toLong(),
                monolithModel.innerMesh.uvOffset.toLong(),
                monolithModel.innerMesh.normalOffset.toLong()
            )
            vkCmdBindPipeline(commandBuffer.vkHandle, bindPointGraphics, monolithInnerPipeline.vkHandle)
            vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pMonolithBuffers, pMonolithInnerOffsets)
            vkCmdBindIndexBuffer(commandBuffer.vkHandle, monolithVertexBuffer.vkBufferHandle, monolithModel.innerMesh.indexOffset.toLong(), VK_INDEX_TYPE_UINT16)
            vkCmdPushConstants(commandBuffer.vkHandle, monolithInnerPipeline.vkLayoutHandle, ShaderStage.BOTH.vkBits, 0, pPushConstants)
            vkCmdDrawIndexed(
                commandBuffer.vkHandle,
                monolithModel.innerMesh.indexCount,
                1,
                0,
                0,
                0
            )

            // rings
            val pDiscVertexBuffers = allocateLongValues(
                discVertexBuffer.vkBufferHandle,
                discVertexBuffer.vkBufferHandle,
                discVertexBuffer.vkBufferHandle,
            )
            val pDiscOffsets = allocateLongValues(
                discModel.posOffset.toLong(),
                discModel.uvOffset.toLong(),
                discModel.normalOffset.toLong()
            )
            vkCmdBindPipeline(commandBuffer.vkHandle, bindPointGraphics, discPipeline.vkHandle)
            vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pDiscVertexBuffers, pDiscOffsets)
            vkCmdBindIndexBuffer(commandBuffer.vkHandle, discVertexBuffer.vkBufferHandle, discModel.indexOffset.toLong(), VK_INDEX_TYPE_UINT16)
            vkCmdPushConstants(commandBuffer.vkHandle, discPipeline.vkLayoutHandle, ShaderStage.BOTH.vkBits, 0, pPushConstants)
            vkCmdDrawIndexed(commandBuffer.vkHandle, discModel.indexCount, 1, 0, 0, 0)
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

    override fun onResizeDestroy() {
        depthAttachment.destroy()
    }

    override fun onResizeRecreate(newExtent2D: ImageExtent2D) {
        createAttachments()
    }

    override fun destroy() {
        device.waitIdle()
        sampler.destroy(device)
        cameraBuffers.forEach(VulkanBuffer::destroy)
        depthAttachment.destroy()
        descriptorPool.destroy(device)
        descriptorSetLayout.destroy(device)
        groundPipeline.destroy(device)

        groundVertexBuffer.destroy()
        groundIndexBuffer.destroy()

        cubemap.destroy()

        monolithDataBuffer.destroy()
        monolithVertexBuffer.destroy()
        discVertexBuffer.destroy()
        monolithOuterPipeline.destroy(device)
        monolithInnerPipeline.destroy(device)
        discPipeline.destroy(device)

        audioSystem.shutdown()

        super.destroy()
    }
}