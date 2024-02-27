package me.fexus.examples.compute

import me.fexus.examples.compute.bulletlimbo.GameLogic
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
import me.fexus.vulkan.component.descriptor.write.DescriptorImageInfo
import me.fexus.vulkan.component.descriptor.write.DescriptorImageWrite
import me.fexus.vulkan.component.pipeline.CullMode
import me.fexus.vulkan.component.pipeline.GraphicsPipeline
import me.fexus.vulkan.component.pipeline.GraphicsPipelineConfiguration
import me.fexus.vulkan.component.pipeline.PushConstantsLayout
import me.fexus.vulkan.component.pipeline.pipelinestage.PipelineStage
import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage
import me.fexus.vulkan.descriptors.DescriptorType
import me.fexus.vulkan.descriptors.image.*
import me.fexus.vulkan.descriptors.image.aspect.ImageAspect
import me.fexus.vulkan.descriptors.image.usage.ImageUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
import me.fexus.vulkan.descriptors.image.sampler.AddressMode
import me.fexus.vulkan.descriptors.image.sampler.Filtering
import me.fexus.vulkan.descriptors.image.sampler.VulkanSampler
import me.fexus.vulkan.descriptors.image.sampler.VulkanSamplerConfiguration
import me.fexus.vulkan.util.ImageExtent2D
import me.fexus.vulkan.util.ImageExtent3D
import me.fexus.window.Window
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRDynamicRendering.*
import org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR
import org.lwjgl.vulkan.VK12.*


class BulletLimbo: VulkanRendererBase(createWindow()) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            BulletLimbo().start()
        }

        private fun createWindow() = Window("Gamelogic on the GPU") {
            windowVisible()
            enableResizable()
            setInitialWindowSize(1067, 600)
            enableDecoration()
            setInitialWindowPosition(400, 300)
            enableAutoIconify()
        }
    }

    private lateinit var depthAttachmentImage: VulkanImage
    private lateinit var colorAttachmentImage: VulkanImage
    private val descriptorPool = DescriptorPool()
    private val descriptorSetLayout = DescriptorSetLayout()
    private val descriptorSet = DescriptorSet()
    private val postProcessingPipeline = GraphicsPipeline()

    private lateinit var sampler: VulkanSampler
    private val gamelogic = GameLogic()


    fun start() {
        initVulkanCore()
        initObjects()
        gamelogic.init(deviceUtil)
        startRenderLoop(window, this)
    }

    private fun initObjects() {
        createAttachments(ImageExtent2D(window.extent2D))

        // -- SAMPLER --
        val samplerLayout = VulkanSamplerConfiguration(AddressMode.REPEAT, 1, Filtering.LINEAR)
        this.sampler = imageFactory.createSampler(samplerLayout)
        // -- SAMPLER --

        initDescriptorStuff()
        updateDescriptorSet()

        val postPipelineConfig = GraphicsPipelineConfiguration(
            emptyList(), PushConstantsLayout(128),
            ClassLoader.getSystemResource("shaders/compute/postprocessing/postprocessing_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/compute/postprocessing/postprocessing_frag.spv").readBytes(),
            cullMode = CullMode.NONE, depthTest = false, depthWrite = false
        )
        this.postProcessingPipeline.create(device, listOf(descriptorSetLayout), postPipelineConfig)
    }

    private fun initDescriptorStuff() {
        val poolPlan = DescriptorPoolPlan(
            1, DescriptorPoolCreateFlag.FREE_DESCRIPTOR_SET,
            listOf(
                DescriptorPoolSize(DescriptorType.SAMPLED_IMAGE, 8),
                DescriptorPoolSize(DescriptorType.SAMPLER, 1)
            )
        )
        this.descriptorPool.create(device, poolPlan)

        val descSetLayoutPlan = DescriptorSetLayoutPlan(
            DescriptorSetLayoutCreateFlag.NONE,
            listOf(
                DescriptorSetLayoutBinding(
                    0, 8, DescriptorType.SAMPLED_IMAGE,
                    ShaderStage.FRAGMENT, DescriptorSetLayoutBindingFlag.PARTIALLY_BOUND
                ),
                DescriptorSetLayoutBinding(
                    1, 1, DescriptorType.SAMPLER,
                    ShaderStage.FRAGMENT, DescriptorSetLayoutBindingFlag.NONE
                )
            )
        )
        this.descriptorSetLayout.create(device, descSetLayoutPlan)

        this.descriptorSet.create(device, descriptorPool, descriptorSetLayout)
    }

    private fun updateDescriptorSet() {
        val imagesWrite = DescriptorImageWrite(
            0, DescriptorType.SAMPLED_IMAGE, 1, this.descriptorSet, 0,
            listOf(DescriptorImageInfo(0L, colorAttachmentImage.vkImageViewHandle, ImageLayout.SHADER_READ_ONLY_OPTIMAL))
        )
        val samplerWrite = DescriptorImageWrite(
            1, DescriptorType.SAMPLER, 1, this.descriptorSet, 0,
            listOf(DescriptorImageInfo(sampler.vkHandle, 0L, ImageLayout.SHADER_READ_ONLY_OPTIMAL))
        )
        this.descriptorSet.update(device, imagesWrite, samplerWrite)
    }

    override fun recordFrame(preparation: FramePreparation, delta: Float): FrameSubmitData = runMemorySafe {
        val width: Int = swapchain.imageExtent.width
        val height: Int = swapchain.imageExtent.height

        val cmdBeginInfo = calloc(VkCommandBufferBeginInfo::calloc) {
            sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            pNext(0)
            flags(0)
            pInheritanceInfo(null)
        }

        val commandBuffer = commandBuffers[currentFrameInFlight]
        val swapchainImageHandle = swapchain.images[preparation.imageIndex]
        val swapchainImageViewHandle = swapchain.imageViews[preparation.imageIndex]

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
            .imageView(colorAttachmentImage.vkImageViewHandle)

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
            imageView(depthAttachmentImage.vkImageViewHandle)
        }

        val renderArea = calloc(VkRect2D::calloc) {
            extent().width(width).height(height)
        }

        val gameRenderPass = calloc(VkRenderingInfoKHR::calloc) {
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

        // Game computing
        gamelogic.recordGameLogicCompute(commandBuffer, currentFrame)
        // Game computing

        val barrierTransitionColorAttachmentToWrite = calloc(VkImageMemoryBarrier::calloc, 1)
        with(barrierTransitionColorAttachmentToWrite[0]) {
            sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            pNext(0)
            srcAccessMask(0)
            dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            oldLayout(ImageLayout.UNDEFINED.vkValue)
            newLayout(ImageLayout.COLOR_ATTACHMENT_OPTIMAL.vkValue)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            image(colorAttachmentImage.vkImageHandle)
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
            0, null, null, barrierTransitionColorAttachmentToWrite
        )

        // Game rendering
        vkCmdBeginRenderingKHR(commandBuffer.vkHandle, gameRenderPass)
        val viewport = calloc(VkViewport::calloc, 1)
        viewport[0].set(0f, 0f, width.toFloat(), height.toFloat(), 1f, 0f)

        val scissor = calloc(VkRect2D::calloc, 1)
        scissor[0].offset().x(0).y(0)
        scissor[0].extent().width(width).height(height)

        vkCmdSetViewport(commandBuffer.vkHandle, 0, viewport)
        vkCmdSetScissor(commandBuffer.vkHandle, 0, scissor)

        gamelogic.recordDrawCommands(commandBuffer, currentFrame)
        vkCmdEndRenderingKHR(commandBuffer.vkHandle)
        // Game rendering

        val barrierTransitionColorAttachmentToRead = calloc(VkImageMemoryBarrier::calloc, 2)
        with(barrierTransitionColorAttachmentToRead[0]) {
            sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            pNext(0)
            srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
            oldLayout(ImageLayout.COLOR_ATTACHMENT_OPTIMAL.vkValue)
            newLayout(ImageLayout.SHADER_READ_ONLY_OPTIMAL.vkValue)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            image(colorAttachmentImage.vkImageHandle)
            subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1)
        }
        with(barrierTransitionColorAttachmentToRead[1]) { // also transition swapchain image
            sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            pNext(0)
            srcAccessMask(0)
            dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            oldLayout(ImageLayout.UNDEFINED.vkValue)
            newLayout(ImageLayout.COLOR_ATTACHMENT_OPTIMAL.vkValue)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            image(swapchainImageHandle)
            subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1)
        }

        vkCmdPipelineBarrier(
            commandBuffer.vkHandle,
            PipelineStage.COLOR_ATTACHMENT_OUTPUT.vkBits, PipelineStage.FRAGMENT_SHADER.vkBits,
            0, null, null, barrierTransitionColorAttachmentToRead
        )

        // Postprocessing here

        // Postprocessing here

        val swapchainImageTransitionBarrier = calloc(VkImageMemoryBarrier::calloc, 1)
        with(swapchainImageTransitionBarrier[0]) {
            sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            pNext(0)
            srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            dstAccessMask(0)
            oldLayout(ImageLayout.COLOR_ATTACHMENT_OPTIMAL.vkValue)
            newLayout(ImageLayout.PRESENT_SRC.vkValue)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            image(swapchainImageHandle)
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
            0, null, null, swapchainImageTransitionBarrier
        )

        vkEndCommandBuffer(commandBuffer.vkHandle)

        return@runMemorySafe FrameSubmitData(preparation.acquireSuccessful, preparation.imageIndex)
    }

    private fun createAttachments(newExtent2D: ImageExtent2D) {
        val depthAttachmentImageLayout = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D,
            ImageExtent3D(newExtent2D, 1),
            1, 1, 1,
            ImageColorFormat.D32_SFLOAT, ImageTiling.OPTIMAL,
            ImageAspect.DEPTH, ImageUsage.DEPTH_STENCIL_ATTACHMENT,
            MemoryPropertyFlag.DEVICE_LOCAL
        )
        this.depthAttachmentImage = deviceUtil.createImage(depthAttachmentImageLayout)

        val colorAttachmentImageLayout = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D,
            ImageExtent3D(newExtent2D, 1),
            1, 1, 1,
            this.swapchain.imageColorFormat, ImageTiling.OPTIMAL,
            ImageAspect.COLOR, ImageUsage.COLOR_ATTACHMENT + ImageUsage.SAMPLED,
            MemoryPropertyFlag.DEVICE_LOCAL
        )
        this.colorAttachmentImage = deviceUtil.createImage(colorAttachmentImageLayout)
    }

    override fun onResizeDestroy() {
        gamelogic.onResizeDestroy()
        depthAttachmentImage.destroy()
        colorAttachmentImage.destroy()
    }

    override fun onResizeRecreate(newExtent2D: ImageExtent2D) {
        gamelogic.onResizeRecreate(newExtent2D)
        createAttachments(newExtent2D)
        updateDescriptorSet()
    }

    override fun destroy() {
        device.waitIdle()
        sampler.destroy(device)

        super.destroy()
    }
}