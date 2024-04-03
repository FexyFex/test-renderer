package me.fexus.examples.coolvoxelrendering

import me.fexus.camera.CameraPerspective
import me.fexus.examples.Globals
import me.fexus.examples.coolvoxelrendering.misc.DescriptorFactory
import me.fexus.examples.coolvoxelrendering.world.Cubemap
import me.fexus.math.clamp
import me.fexus.math.mat.Mat4
import me.fexus.math.rad
import me.fexus.math.vec.IVec2
import me.fexus.math.vec.Vec3
import me.fexus.memory.runMemorySafe
import me.fexus.voxel.VoxelRegistry
import me.fexus.vulkan.util.FramePreparation
import me.fexus.vulkan.util.FrameSubmitData
import me.fexus.vulkan.VulkanRendererBase
import me.fexus.vulkan.component.descriptor.pool.DescriptorPool
import me.fexus.vulkan.component.descriptor.pool.DescriptorPoolPlan
import me.fexus.vulkan.component.descriptor.pool.DescriptorPoolSize
import me.fexus.vulkan.component.descriptor.pool.flags.DescriptorPoolCreateFlag
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayout
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayoutBinding
import me.fexus.vulkan.component.descriptor.set.layout.DescriptorSetLayoutPlan
import me.fexus.vulkan.component.descriptor.set.layout.bindingflags.DescriptorSetLayoutBindingFlag
import me.fexus.vulkan.component.descriptor.set.layout.createflags.DescriptorSetLayoutCreateFlag
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
import me.fexus.vulkan.extension.DescriptorIndexingEXT
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


/**
 * The fundamental idea in this example is to construct voxel chunk meshes by instancing a single quad for each
 * visible side. I dubbed this a "hull" instead of a "mesh" in an ettempt to avoid confusion.
 * The algorithm for creating the hull is much the same as in the traditional meshing approach except for the fact
 * that we compress the data for each side into a single integer instead of writing vertices for a mesh.
 * This saves a good amount of memory (typically 80% - 90%) over meshing but is limited to only full blocks.
 */
class CoolVoxelRendering: VulkanRendererBase(createWindow()), InputEventSubscriber {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CoolVoxelRendering().start()
        }

        private fun createWindow() = Window("Cool Voxel Rendering") {
            windowVisible()
            enableResizable()
            setInitialWindowSize(1067,600)
            enableDecoration()
            setInitialWindowPosition(400, 300)
            enableAutoIconify()
        }

        private fun Boolean.toInt(): Int = if (this) 1 else 0
    }

    private var time: Double = 0.0

    private val camera = CameraPerspective(window.aspect)
    private val player = Player()

    private val voxelRegistry = VoxelRegistry()

    private val descriptorPool = DescriptorPool()
    private val descriptorSetLayout = DescriptorSetLayout()
    private val descriptorFactory = DescriptorFactory(deviceUtil, descriptorPool, descriptorSetLayout)

    private val cubemap = Cubemap(deviceUtil, descriptorFactory)

    private lateinit var depthAttachment: VulkanImage
    private lateinit var cameraBuffers: Array<VulkanBuffer>
    private lateinit var nearSampler: VulkanSampler

    private val textureArray = VoxelTextureArray(deviceUtil, descriptorFactory, voxelRegistry)

    private val world = World(deviceUtil, descriptorFactory, voxelRegistry, camera)

    private val inputHandler = InputHandler(window)

    private var trapMouse: Boolean = true


    fun start() {
        initVulkanCore(withDebug = true, extensions = listOf(DescriptorIndexingEXT))
        initObjects()
        startRenderLoop(window, this)
    }

    override fun onMouseMoved(newPosition: IVec2) {
        if (!trapMouse) return

        val windowSize = window.extent2D
        val middle = windowSize / 2
        val dist = middle - newPosition

        player.rotation.x -= dist.y.toFloat() / 10f
        player.rotation.x = player.rotation.x.clamp(-90f, 90f)
        player.rotation.y -= dist.x.toFloat() / 10f

        window.setCursorPos(middle)
    }

    override fun onKeyPressed(key: Key) {
        if (key == Key.ESC) trapMouse = !trapMouse
        if (key == Key.P) println(player.position)
    }

    private fun initObjects() {
        voxelRegistry.init()
        subscribe(inputHandler)

        player.position.y = 100f

        createDescriptorPool()
        createDescriptorSetLayout()
        descriptorFactory.init()

        // -- CAMERA BUFFER --
        val cameraBufferConfig = VulkanBufferConfiguration(
            144L,
            MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.UNIFORM_BUFFER
        )
        this.cameraBuffers = descriptorFactory.createNBuffers(cameraBufferConfig)
        // -- CAMERA BUFFER --

        // -- SAMPLERS --
        val nearSamplerConfig = VulkanSamplerConfiguration(AddressMode.REPEAT, 1, Filtering.NEAREST)
        this.nearSampler = descriptorFactory.createSampler(nearSamplerConfig)
        // -- SAMPLERS --

        textureArray.init()
        cubemap.init()
        world.init()

        createAttachments()
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

    private fun createDescriptorPool() {
        // Descriptor Sets and Pipeline
        val poolPlan = DescriptorPoolPlan(
            Globals.FRAMES_TOTAL, DescriptorPoolCreateFlag.UPDATE_AFTER_BIND + DescriptorPoolCreateFlag.FREE_DESCRIPTOR_SET,
            listOf(
                DescriptorPoolSize(DescriptorType.UNIFORM_BUFFER, 4 * Globals.FRAMES_TOTAL),
                DescriptorPoolSize(DescriptorType.SAMPLED_IMAGE, 16 * Globals.FRAMES_TOTAL),
                DescriptorPoolSize(DescriptorType.SAMPLER, 8 * Globals.FRAMES_TOTAL),
                DescriptorPoolSize(DescriptorType.STORAGE_BUFFER, 16 * Globals.FRAMES_TOTAL),
            )
        )
        this.descriptorPool.create(device, poolPlan)
    }

    fun createDescriptorSetLayout() {
        val setLayoutPlan = DescriptorSetLayoutPlan(
            DescriptorSetLayoutCreateFlag.UPDATE_AFTER_BIND,
            listOf(
                DescriptorSetLayoutBinding(
                    0, 4, DescriptorType.UNIFORM_BUFFER,
                    ShaderStage.VERTEX + ShaderStage.COMPUTE,
                    DescriptorSetLayoutBindingFlag.PARTIALLY_BOUND
                ),
                DescriptorSetLayoutBinding(
                    1, 16, DescriptorType.SAMPLED_IMAGE,
                    ShaderStage.FRAGMENT,
                    DescriptorSetLayoutBindingFlag.PARTIALLY_BOUND + DescriptorSetLayoutBindingFlag.UPDATE_AFTER_BIND
                ),
                DescriptorSetLayoutBinding(
                    2, 8, DescriptorType.SAMPLER,
                    ShaderStage.FRAGMENT,
                    DescriptorSetLayoutBindingFlag.PARTIALLY_BOUND + DescriptorSetLayoutBindingFlag.UPDATE_AFTER_BIND
                ),
                DescriptorSetLayoutBinding(
                    3, 16, DescriptorType.STORAGE_BUFFER,
                    ShaderStage.VERTEX + ShaderStage.COMPUTE,
                    DescriptorSetLayoutBindingFlag.PARTIALLY_BOUND + DescriptorSetLayoutBindingFlag.UPDATE_AFTER_BIND
                )
            )
        )
        this.descriptorSetLayout.create(device, setLayoutPlan)
    }

    private fun handleInput(delta: Float) {
        val speed = 1f + (inputHandler.isKeyDown(Key.LCTRL).toInt() * 10f)

        val leftRight = inputHandler.isKeyDown(Key.A).toInt() - inputHandler.isKeyDown(Key.D).toInt()
        val frontBack = inputHandler.isKeyDown(Key.W).toInt() - inputHandler.isKeyDown(Key.S).toInt()
        val upDown = inputHandler.isKeyDown(Key.SPACE).toInt() - inputHandler.isKeyDown(Key.LSHIFT).toInt()

        val moveSpeed = delta * 10f * speed

        player.position.y += upDown * moveSpeed

        if (leftRight == 0 && frontBack == 0) return

        val rotM = Mat4(1f)
            .rotate(0f, Vec3(1f, 0f, 0f))
            .rotate(player.rotation.y.rad, Vec3(0f, 1f, 0f))
            .rotate(player.rotation.z.rad, Vec3(0f, 0f, 1f))
        val transM = Mat4(1f).translate(Vec3(leftRight, 0, frontBack)) / rotM
        val forward = Vec3(transM[3][0], transM[3][1], transM[3][2]).normalize() * moveSpeed

        player.position.x -= forward.x
        player.position.z -= forward.z
    }

    private fun updateBuffers() {
        camera.zNear = 0.1f
        camera.zFar = 500f
        camera.position = -player.position
        camera.rotation = player.rotation

        val view = camera.calculateView()
        val proj = camera.calculateProjection()
        val data = ByteBuffer.allocate(144)
        data.order(ByteOrder.LITTLE_ENDIAN)
        view.toByteBufferColumnMajor(data, 0)
        proj.toByteBufferColumnMajor(data, 64)
        data.putFloat(128, time.toFloat())

        cameraBuffers[currentFrame].put(0, data)
    }

    override fun recordFrame(preparation: FramePreparation, delta: Float): FrameSubmitData = runMemorySafe {
        time += delta
        handleInput(delta)
        updateBuffers()

        if (descriptorFactory.updateRequired) descriptorFactory.updateDescriptorSets()

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
                .float32(0, 0.0f)
                .float32(1, 0.0f)
                .float32(2, 0.0f)
                .float32(3, 1.0f)
        }

        val clearValueDepth = calloc(VkClearValue::calloc) {
            depthStencil().depth(1.0f)
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

        val pDescriptorSets = allocateLongValues(descriptorFactory.descriptorSets[currentFrame].vkHandle)

        world.recordComputeCommands(commandBuffer, currentFrame)

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

            vkCmdSetViewport(commandBuffer.vkHandle, 0, viewport)
            vkCmdSetScissor(commandBuffer.vkHandle, 0, scissor)

            vkCmdBindDescriptorSets(
                commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, cubemap.pipeline.vkLayoutHandle,
                0, pDescriptorSets, null
            )

            cubemap.recordRenderCommands(commandBuffer, currentFrame)
            world.recordGraphicsCommands(commandBuffer, currentFrame)
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
        nearSampler.destroy()
        cameraBuffers.forEach(VulkanBuffer::destroy)
        depthAttachment.destroy()
        descriptorPool.destroy()
        descriptorSetLayout.destroy()

        world.destroy()

        textureArray.destroy()

        cubemap.destroy()

        super.destroy()
    }
}