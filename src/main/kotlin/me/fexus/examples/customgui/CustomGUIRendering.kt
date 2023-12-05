package me.fexus.examples.customgui

import me.fexus.fexgui.FexVulkanGUI
import me.fexus.fexgui.logic.component.SpatialComponent
import me.fexus.fexgui.logic.component.Label
import me.fexus.math.vec.IVec2
import me.fexus.memory.runMemorySafe
import me.fexus.vulkan.util.FramePreparation
import me.fexus.vulkan.util.FrameSubmitData
import me.fexus.vulkan.VulkanRendererBase
import me.fexus.vulkan.accessmask.AccessMask
import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.pipeline.*
import me.fexus.vulkan.descriptors.image.*
import me.fexus.vulkan.descriptors.image.aspect.ImageAspect
import me.fexus.vulkan.descriptors.image.usage.ImageUsage
import me.fexus.vulkan.descriptors.memoryproperties.MemoryProperty
import me.fexus.vulkan.component.pipeline.pipelinestage.PipelineStage
import me.fexus.vulkan.util.ImageExtent2D
import me.fexus.vulkan.util.ImageExtent3D
import me.fexus.window.Window
import me.fexus.window.input.InputHandler
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRDynamicRendering.*
import org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR
import org.lwjgl.vulkan.VK12.*
import kotlin.math.min


class CustomGUIRendering: VulkanRendererBase(createWindow()) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CustomGUIRendering().start()
        }

        private fun createWindow() = Window("Custom GUI Rendering") {
            windowVisible()
            enableResizable()
            setInitialWindowSize(1067,600)
            enableDecoration()
            setInitialWindowPosition(400, 300)
            enableAutoIconify()
        }
    }

    private lateinit var depthAttachment: VulkanImage

    private val inputHandler = InputHandler(window)
    private val gui = FexVulkanGUI.create(window, inputHandler, deviceUtil)


    fun start() {
        initVulkanCore()
        initObjects()
        gui.init()
        startRenderLoop(window, this)
    }


    private fun initObjects() {
        createAttachmentImages()
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


    private fun frameUpdate() {
        handleInput()
    }

    override fun recordFrame(preparation: FramePreparation, delta: Float): FrameSubmitData = runMemorySafe {
        frameUpdate()

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

        val guiComponents = gui.getAllChildren().filterIsInstance<SpatialComponent>()
        updateGUIText(commandBuffer, guiComponents.filterIsInstance<Label>())

        vkCmdBeginRenderingKHR(commandBuffer.vkHandle, defaultRendering)
        runMemorySafe {
            val viewport = calloc(VkViewport::calloc, 1)
            viewport[0].set(0f, 0f, width.toFloat(), height.toFloat(), 1f, 0f)

            val scissor = calloc(VkRect2D::calloc, 1)
            scissor[0].offset().x(0).y(0)
            scissor[0].extent().width(width).height(height)

            vkCmdSetViewport(commandBuffer.vkHandle, 0, viewport)
            vkCmdSetScissor(commandBuffer.vkHandle, 0, scissor)

            gui.recordGUIRenderCommands(commandBuffer, currentFrame)
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

    private fun updateGUIText(cmdBuf: CommandBuffer, textComponents: List<Label>) {
        textComponents.filter { it.textRequiresUpdate }.forEach {
            // Transition the component's texture to TRANSFER_DST first
            val targetImage = images[it.textureIndex]!!
            deviceUtil.cmdTransitionImageLayout(
                cmdBuf, targetImage,
                AccessMask.SHADER_READ, AccessMask.TRANSFER_WRITE,
                ImageLayout.SHADER_READ_ONLY_OPTIMAL, ImageLayout.TRANSFER_DST_OPTIMAL,
                PipelineStage.FRAGMENT_SHADER, PipelineStage.TRANSFER
            )

            runMemorySafe {
                val pClearColor = calloc(VkClearColorValue::calloc) {
                    this.float32(0, 0f)
                    this.float32(1, 0f)
                    this.float32(2, 0f)
                    this.float32(3, 0f)
                }

                val pRange = calloc(VkImageSubresourceRange::calloc) {
                    set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1)
                }

                vkCmdClearColorImage(
                    cmdBuf.vkHandle, targetImage.vkImageHandle,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    pClearColor, pRange
                )
            }

            if (it.text.isNotEmpty())
                blitCharacters(cmdBuf, it.text, targetImage)

            deviceUtil.cmdTransitionImageLayout(
                cmdBuf, targetImage,
                AccessMask.TRANSFER_WRITE, AccessMask.SHADER_READ,
                ImageLayout.TRANSFER_DST_OPTIMAL, ImageLayout.SHADER_READ_ONLY_OPTIMAL,
                PipelineStage.TRANSFER, PipelineStage.FRAGMENT_SHADER
            )

            it.textRequiresUpdate = false
        }
    }

    private fun blitCharacters(cmdBuf: CommandBuffer, text: String, dstImage: VulkanImage) = runMemorySafe {
        val glyphHeight = dstImage.config.extent.height
        val glyphWidth = (glyphHeight / glyphAtlas.glyphHeight) * glyphAtlas.glyphWidth
        val pRegions = calloc(VkImageBlit::calloc, text.length)
        val maxX = dstImage.config.extent.width

        repeat(text.length) { i ->
            val glyphBounds = glyphAtlas.getGlyphBounds(text[i])

            val dstMin = IVec2(min(i * glyphWidth, maxX), 0)
            val dstMax = IVec2(min(i * glyphWidth + glyphWidth, maxX), glyphHeight)

            pRegions[i].srcSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1)
            pRegions[i].dstSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1)
            pRegions[i].srcOffsets(0).x(glyphBounds.min.x).y(glyphBounds.min.y).z(0)
            pRegions[i].srcOffsets(1).x(glyphBounds.max.x).y(glyphBounds.max.y).z(1)
            pRegions[i].dstOffsets(0).x(dstMin.x).y(dstMin.y).z(0)
            pRegions[i].dstOffsets(1).x(dstMax.x).y(dstMax.y).z(1)
        }

        vkCmdBlitImage(
            cmdBuf.vkHandle,
            glyphImage.vkImageHandle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
            dstImage.vkImageHandle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            pRegions, VK_FILTER_NEAREST
        )
    }

    private fun handleInput() {}

    override fun onResizeDestroy() {
        depthAttachment.destroy()
    }

    override fun onResizeRecreate(newExtent2D: ImageExtent2D) {
        createAttachmentImages()
    }

    override fun destroy() {
        device.waitIdle()
        depthAttachment.destroy()
        super.destroy()
    }
}