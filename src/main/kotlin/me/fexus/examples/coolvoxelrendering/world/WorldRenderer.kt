package me.fexus.examples.coolvoxelrendering.world

import me.fexus.camera.CameraOrthographic
import me.fexus.camera.CameraPerspective
import me.fexus.examples.coolvoxelrendering.misc.DescriptorFactory
import me.fexus.examples.coolvoxelrendering.misc.ImageDebugQuad
import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHull
import me.fexus.examples.coolvoxelrendering.world.position.ChunkPosition
import me.fexus.examples.surroundsound.MeshUploader
import me.fexus.math.mat.Mat4
import me.fexus.math.vec.Vec2
import me.fexus.math.vec.Vec3
import me.fexus.memory.runMemorySafe
import me.fexus.model.QuadModelTriangleStrips
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.pipeline.*
import me.fexus.vulkan.component.pipeline.pipelinestage.PipelineStage
import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.image.*
import me.fexus.vulkan.descriptors.image.aspect.ImageAspect
import me.fexus.vulkan.descriptors.image.usage.ImageUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
import me.fexus.vulkan.util.ImageExtent3D
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRDynamicRendering.*
import org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR
import org.lwjgl.vulkan.VK12.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.roundToInt


class WorldRenderer(
    private val deviceUtil: VulkanDeviceUtil,
    private val descriptorFactory: DescriptorFactory,
    private val camera: CameraPerspective
) {
    companion object {
        const val SHADOW_MAP_WIDTH = 600
        const val SHADOW_MAP_HEIGHT = 600
    }

    private val lightSourceCam = CameraOrthographic(100f, 100f, 100f, 100f)

    private val cullPipeline = ComputePipeline()
    private val shadowPipeline = GraphicsPipeline()
    private val chunkPipeline = GraphicsPipeline()

    private lateinit var hullVertexBuffer: VulkanBuffer

    lateinit var cullCameraBuffers: Array<VulkanBuffer>
    lateinit var shadowCameraBuffers: Array<VulkanBuffer>
    lateinit var cullObjectsBuffers: Array<VulkanBuffer>
    lateinit var indirectCommandBuffer: VulkanBuffer
    lateinit var chunkPositionsBuffer: VulkanBuffer
    lateinit var sidePositionsBuffer: VulkanBuffer

    lateinit var shadowDepthAttachment: VulkanImage
    lateinit var shadowColorAttachment: VulkanImage

    private val chunkHulls = HashMap<ChunkPosition, ChunkHull>()
    private var nextHullOffset: Int = 0

    private val threadPool = Executors.newCachedThreadPool()
    private val processorCount = Runtime.getRuntime().availableProcessors()

    private val debugQuad = ImageDebugQuad(deviceUtil, descriptorFactory)


    fun init() {
        createHullBuffers()
        createHullPipeline()
        debugQuad.init()
    }

    fun recordComputeCommands(commandBuffer: CommandBuffer, frameIndex: Int) = runMemorySafe {
        val chunkCount = chunkHulls.size

        // Begin by loading the chunk data into the cullObjects buffer on other threads
        cullObjectsBuffers[frameIndex].set(0, 0, cullObjectsBuffers[frameIndex].config.size)
        var chunkedSize = (chunkCount.toFloat() / processorCount.toFloat()).roundToInt()
        if (chunkedSize == 0) chunkedSize = 1
        val tasks = mutableListOf<Callable<Unit>>()
        chunkHulls.values.chunked(chunkedSize).forEachIndexed { chunkedID, chunkHulls ->
            val hullsOffset = chunkedID * chunkedSize
            val task = Callable {
                val buf = ByteBuffer.allocate(32)
                buf.order(ByteOrder.LITTLE_ENDIAN)
                for ((hullLocalOffset, hull) in chunkHulls.withIndex()) {
                    val offset = (hullsOffset * 32) + (hullLocalOffset * 32)
                    buf.clear()
                    hull.chunkPosition.intoByteBuffer(buf, 0)
                    buf.putInt(12, hull.data.instanceCount)
                    buf.putInt(16, hull.data.firstInstance)
                    cullObjectsBuffers[frameIndex].put(offset, buf)
                }
            }
            tasks.add(task)
        }
        val futures = threadPool.invokeAll(tasks)

        val pDescriptorSets = allocateLongValues(descriptorFactory.descriptorSets[frameIndex].vkHandle)
        vkCmdBindDescriptorSets(
            commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_COMPUTE, cullPipeline.vkLayoutHandle,
            0, pDescriptorSets, null
        )

        val pVertexBuffers = allocateLongValues(hullVertexBuffer.vkBufferHandle)
        val pOffsets = allocateLongValues(0)
        val pPushConstants = allocate(128)
        pPushConstants.putInt(0, cullCameraBuffers[frameIndex].index)
        pPushConstants.putInt(4, cullObjectsBuffers[frameIndex].index)
        pPushConstants.putInt(8, indirectCommandBuffer.index)
        pPushConstants.putInt(12, chunkPositionsBuffer.index)
        pPushConstants.putInt(16, sidePositionsBuffer.index)
        vkCmdPushConstants(
            commandBuffer.vkHandle, cullPipeline.vkLayoutHandle,
            (ShaderStage.COMPUTE + ShaderStage.VERTEX + ShaderStage.FRAGMENT).vkBits,
            0, pPushConstants
        )

        val camBuf = allocate(128)
        camBuf.order(ByteOrder.LITTLE_ENDIAN)
        val view = camera.calculateView()
        //view[3][2] -= 1f
        val proj = camera.calculateProjection()
        view.toByteBufferColumnMajor(camBuf, 0)
        proj.toByteBufferColumnMajor(camBuf, 64)
        cullCameraBuffers[frameIndex].put(0, camBuf)

        lightSourceCam.zNear = camera.zNear
        lightSourceCam.zFar = camera.zFar
        //lightSourceCam.position = Vec3(0f, -200f, 0f)
        //lightSourceCam.rotation = Vec3(90f, 0f ,0f)
        val lightView = Mat4().lookAt(Vec3(0f, -200f, 0f), Vec3(0.1f), Vec3(0f, -1f, 0f))
        lightView.toByteBufferColumnMajor(camBuf, 0)
        val lightProj = lightSourceCam.calculateProjection()
        println(lightProj)
        lightProj.toByteBufferColumnMajor(camBuf, 64)
        shadowCameraBuffers[frameIndex].put(0, camBuf)

        // Barrier that ensures that the buffers aren't in use anymore
        val barrier = calloc(VkBufferMemoryBarrier::calloc, 2)
        with(barrier[0]) {
            sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
            pNext(0)
            srcAccessMask(0)
            dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            buffer(indirectCommandBuffer.vkBufferHandle)
            offset(0L)
            size(VK_WHOLE_SIZE)
        }
        with(barrier[1]) {
            sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
            pNext(0)
            srcAccessMask(0)
            dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            buffer(chunkPositionsBuffer.vkBufferHandle)
            offset(0L)
            size(VK_WHOLE_SIZE)
        }
        vkCmdPipelineBarrier(
            commandBuffer.vkHandle,
            VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
            0, null, barrier, null
        )

        // Clear the buffers
        vkCmdFillBuffer(commandBuffer.vkHandle, indirectCommandBuffer.vkBufferHandle, 0L, indirectCommandBuffer.config.size, 0)
        vkCmdFillBuffer(commandBuffer.vkHandle, chunkPositionsBuffer.vkBufferHandle, 0L, chunkPositionsBuffer.config.size, 0)

        // Barrier to ensure that the clearing has finished
        with(barrier[0]) {
            srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
        }
        with(barrier[1]) {
            srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
        }
        vkCmdPipelineBarrier(
            commandBuffer.vkHandle,
            VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
            0, null, barrier, null
        )

        // Run culling compute shader
        vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_COMPUTE, cullPipeline.vkHandle)
        vkCmdDispatch(commandBuffer.vkHandle, ceil(chunkCount / 256f).toInt(), 1, 1)

        with(barrier[0]) {
            srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
            dstAccessMask(VK_ACCESS_INDIRECT_COMMAND_READ_BIT)
        }
        with(barrier[1]) {
            srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
            dstAccessMask(VK_ACCESS_INDIRECT_COMMAND_READ_BIT)
        }
        vkCmdPipelineBarrier(
            commandBuffer.vkHandle,
            VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT,
            0, null, barrier, null
        )

        // Start a renderpass for the shadow map
        val clearValues = calloc(VkClearValue::calloc) {
            color()
                .float32(0, 0.0f)
                .float32(1, 0.0f)
                .float32(2, 0.0f)
                .float32(3, 1.0f)

            depthStencil().depth(1.0f)
            depthStencil().stencil(0)
        }

        val colorAttachment = calloc(VkRenderingAttachmentInfoKHR::calloc, 1)
        with(colorAttachment[0]) {
            sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR)
            pNext(0)
            imageLayout(VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR)
            resolveMode(VK_RESOLVE_MODE_NONE)
            resolveImageView(0)
            resolveImageLayout(0)
            loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            clearValue(clearValues)
            imageView(shadowColorAttachment.vkImageViewHandle)
        }

        val depthAttachment = calloc(VkRenderingAttachmentInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR)
            pNext(0)
            imageLayout(VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR)
            resolveMode(VK_RESOLVE_MODE_NONE)
            resolveImageView(0)
            resolveImageLayout(0)
            loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            clearValue(clearValues)
            imageView(shadowDepthAttachment.vkImageViewHandle)
        }

        val renderArea = calloc(VkRect2D::calloc) {
            extent().width(SHADOW_MAP_WIDTH).height(SHADOW_MAP_HEIGHT)
        }

        // Make color and depth image usable for rendering
        val shadowColorImageBarrier = calloc(VkImageMemoryBarrier::calloc, 1)
        with(shadowColorImageBarrier[0]) {
            sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            pNext(0)
            srcAccessMask(0)
            dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT or VK_ACCESS_COLOR_ATTACHMENT_READ_BIT)
            oldLayout(ImageLayout.UNDEFINED.vkValue)
            newLayout(ImageLayout.COLOR_ATTACHMENT_OPTIMAL.vkValue)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            image(shadowColorAttachment.vkImageHandle)
            subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1)
        }
        vkCmdPipelineBarrier(
            commandBuffer.vkHandle,
            PipelineStage.TOP_OF_PIPE.vkBits, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
            0, null, null, shadowColorImageBarrier
        )

        val renderingInfo = calloc(VkRenderingInfoKHR::calloc) {
            sType(VK_STRUCTURE_TYPE_RENDERING_INFO_KHR)
            pNext(0)
            flags(0)
            renderArea(renderArea)
            layerCount(1)
            viewMask(0)
            pColorAttachments(colorAttachment)
            pDepthAttachment(depthAttachment)
            pStencilAttachment(null)
        }

        vkCmdBeginRenderingKHR(commandBuffer.vkHandle, renderingInfo)
        runMemorySafe {
            val viewport = calloc(VkViewport::calloc, 1)
            viewport[0].set(0f, 0f, SHADOW_MAP_WIDTH.toFloat(), SHADOW_MAP_HEIGHT.toFloat(), 0f, 1f)

            val scissors = calloc(VkRect2D::calloc, 1)
            scissors[0].offset().x(0).y(0)
            scissors[0].extent().width(SHADOW_MAP_WIDTH).height(SHADOW_MAP_HEIGHT)

            vkCmdSetViewport(commandBuffer.vkHandle, 0, viewport)
            vkCmdSetScissor(commandBuffer.vkHandle, 0, scissors)

            vkCmdBindDescriptorSets(
                commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, shadowPipeline.vkLayoutHandle,
                0, pDescriptorSets, null
            )

            pPushConstants.putInt(0, shadowCameraBuffers[frameIndex].index)
            vkCmdPushConstants(
                commandBuffer.vkHandle, shadowPipeline.vkLayoutHandle,
                (ShaderStage.COMPUTE + ShaderStage.VERTEX + ShaderStage.FRAGMENT).vkBits,
                0, pPushConstants
            )

            vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, shadowPipeline.vkHandle)
            vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
            vkCmdDrawIndirect(commandBuffer.vkHandle, indirectCommandBuffer.vkBufferHandle, 0L, chunkCount, 16)
        }
        vkCmdEndRenderingKHR(commandBuffer.vkHandle)

        // Make shadow depth and color map readable for the next draw call
        with(shadowColorImageBarrier[0]) {
            srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
            oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
        }
        vkCmdPipelineBarrier(
            commandBuffer.vkHandle,
            VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
            0, null, null, shadowColorImageBarrier
        )

        while (futures.any { !it.isDone }) { continue }
    }

    fun recordRenderCommands(commandBuffer: CommandBuffer, frameIndex: Int) = runMemorySafe {
        val chunkCount = chunkHulls.size

        val pVertexBuffers = allocateLongValues(hullVertexBuffer.vkBufferHandle)
        val pOffsets = allocateLongValues(0)
        val pPushConstants = allocate(128)
        pPushConstants.putInt(0, 0)
        pPushConstants.putInt(4, cullObjectsBuffers[frameIndex].index)
        pPushConstants.putInt(8, indirectCommandBuffer.index)
        pPushConstants.putInt(12, chunkPositionsBuffer.index)
        pPushConstants.putInt(16, sidePositionsBuffer.index)
        pPushConstants.putInt(20, shadowDepthAttachment.index)
        pPushConstants.putInt(24, shadowCameraBuffers[frameIndex].index)
        lightSourceCam.position.intoByteBuffer(pPushConstants, 32)
        camera.position.intoByteBuffer(pPushConstants, 48)
        Vec2(camera.zNear, camera.zFar).intoByteBuffer(pPushConstants, 64)
        vkCmdPushConstants(
            commandBuffer.vkHandle, chunkPipeline.vkLayoutHandle,
            (ShaderStage.COMPUTE + ShaderStage.VERTEX + ShaderStage.FRAGMENT).vkBits,
            0, pPushConstants
        )

        val pDescriptorSets = allocateLongValues(descriptorFactory.descriptorSets[frameIndex].vkHandle)
        vkCmdBindDescriptorSets(
            commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, chunkPipeline.vkLayoutHandle,
            0, pDescriptorSets, null
        )

        // Indirect Draw
        vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, chunkPipeline.vkHandle)
        vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
        vkCmdDrawIndirect(commandBuffer.vkHandle, indirectCommandBuffer.vkBufferHandle, 0L, chunkCount, 16)


        // Debug Quad
        pVertexBuffers.put(0, debugQuad.vertexBuffer.vkBufferHandle)
        Vec2(-0.98f).intoByteBuffer(pPushConstants, 0)
        Vec2(0.8f).intoByteBuffer(pPushConstants, 8)
        pPushConstants.putInt(16, shadowColorAttachment.index)
        vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, debugQuad.pipeline.vkHandle)
        vkCmdPushConstants(
            commandBuffer.vkHandle, debugQuad.pipeline.vkLayoutHandle,
            (ShaderStage.COMPUTE + ShaderStage.VERTEX + ShaderStage.FRAGMENT).vkBits,
            0, pPushConstants
        )
        vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
        vkCmdDraw(commandBuffer.vkHandle, 4, 1, 0, 0)
    }


    fun submitChunkHull(hull: ChunkHull) {
        hull.data.firstInstance = nextHullOffset / VoxelSide.SIZE_BYTES

        chunkHulls[hull.chunkPosition] = hull

        sidePositionsBuffer.put(nextHullOffset, hull.data.buffer)
        nextHullOffset += hull.data.buffer.capacity()
    }


    private fun createHullBuffers() {
        val meshUploader = MeshUploader(deviceUtil)

        val size = QuadModelTriangleStrips.vertices.size * QuadModelTriangleStrips.Vertex.SIZE_BYTES
        val buf = ByteBuffer.allocate(size)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        QuadModelTriangleStrips.vertices.forEachIndexed { vIndex, vertex ->
            val offset = vIndex * QuadModelTriangleStrips.Vertex.SIZE_BYTES
            vertex.writeToByteBuffer(buf, offset)
        }
        this.hullVertexBuffer = meshUploader.uploadBuffer(buf, BufferUsage.VERTEX_BUFFER)

        val cullCameraBufferConfig = VulkanBufferConfiguration(
            144L,
            MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.UNIFORM_BUFFER
        )
        this.cullCameraBuffers = descriptorFactory.createNBuffers(cullCameraBufferConfig)
        this.shadowCameraBuffers = descriptorFactory.createNBuffers(cullCameraBufferConfig) // same thing

        val cullObjectsBufferConfig = VulkanBufferConfiguration(
            16L * 65536L,
            MemoryPropertyFlag.HOST_COHERENT + MemoryPropertyFlag.HOST_VISIBLE,
            BufferUsage.STORAGE_BUFFER
        )
        this.cullObjectsBuffers = descriptorFactory.createNBuffers(cullObjectsBufferConfig)

        val indirectCommandBufferConfig = VulkanBufferConfiguration(
            16L * 65536L,
            MemoryPropertyFlag.DEVICE_LOCAL,
            BufferUsage.INDIRECT + BufferUsage.STORAGE_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.indirectCommandBuffer = descriptorFactory.createBuffer(indirectCommandBufferConfig)

        val chunkPositionsBufferConfig = VulkanBufferConfiguration(
            16L * 65536L,
            MemoryPropertyFlag.DEVICE_LOCAL,
            BufferUsage.STORAGE_BUFFER + BufferUsage.TRANSFER_DST
        )
        this.chunkPositionsBuffer = descriptorFactory.createBuffer(chunkPositionsBufferConfig)

        val positionsBufferConfig = VulkanBufferConfiguration(
            900_000_000L,
            MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.STORAGE_BUFFER
        )
        this.sidePositionsBuffer = descriptorFactory.createBuffer(positionsBufferConfig)

        // Just to map the buffers in advance
        cullObjectsBuffers.forEach { it.getInt(0) }
        sidePositionsBuffer.getInt(0)

        val shadowMapDepthConfig = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D, ImageExtent3D(SHADOW_MAP_WIDTH, SHADOW_MAP_HEIGHT, 1),
            1, 1, 1, ImageColorFormat.D32_SFLOAT, ImageTiling.OPTIMAL,
            ImageAspect.DEPTH, ImageUsage.DEPTH_STENCIL_ATTACHMENT + ImageUsage.SAMPLED, MemoryPropertyFlag.DEVICE_LOCAL
        )
        this.shadowDepthAttachment = descriptorFactory.createImage(shadowMapDepthConfig)

        val shadowMapColorConfig = VulkanImageConfiguration(
            ImageType.TYPE_2D, ImageViewType.TYPE_2D, ImageExtent3D(SHADOW_MAP_WIDTH, SHADOW_MAP_HEIGHT, 1),
            1, 1, 1, ImageColorFormat.B8G8R8A8_SRGB, ImageTiling.OPTIMAL,
            ImageAspect.COLOR, ImageUsage.COLOR_ATTACHMENT + ImageUsage.SAMPLED, MemoryPropertyFlag.DEVICE_LOCAL
        )
        this.shadowColorAttachment = descriptorFactory.createImage(shadowMapColorConfig)
    }

    private fun createHullPipeline() {
        val graphicsConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC3, 0),
                VertexAttribute(1, VertexAttributeFormat.VEC2, 12)
            ),
            PushConstantsLayout(128, shaderStages = ShaderStage.COMPUTE + ShaderStage.FRAGMENT + ShaderStage.VERTEX),
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/chunk/chunk_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/chunk/chunk_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.SCISSOR, DynamicState.VIEWPORT),
            primitive = Primitive.TRIANGLE_STRIPS,
            cullMode = CullMode.BACKFACE
        )
        this.chunkPipeline.create(deviceUtil.device, listOf(descriptorFactory.descriptorSetLayout), graphicsConfig)

        val shadowPipelineConfig = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC3, 0),
                VertexAttribute(1, VertexAttributeFormat.VEC2, 12)
            ),
            PushConstantsLayout(128, shaderStages = ShaderStage.COMPUTE + ShaderStage.FRAGMENT + ShaderStage.VERTEX),
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/chunk/shadow/chunk_shadow_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/chunk/shadow/chunk_shadow_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.SCISSOR, DynamicState.VIEWPORT),
            primitive = Primitive.TRIANGLE_STRIPS,
            cullMode = CullMode.FRONTFACE,
        )
        this.shadowPipeline.create(deviceUtil.device, listOf(descriptorFactory.descriptorSetLayout), shadowPipelineConfig)

        val computeConfig = ComputePipelineConfiguration(
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/compute/frustum_culling/frustum_culling_comp.spv").readBytes(),
            PushConstantsLayout(128, shaderStages = ShaderStage.COMPUTE + ShaderStage.FRAGMENT + ShaderStage.VERTEX),
        )
        this.cullPipeline.create(deviceUtil.device, descriptorFactory.descriptorSetLayout, computeConfig)
    }


    fun destroy() {
        cullPipeline.destroy()
        shadowPipeline.destroy()
        chunkPipeline.destroy()

        hullVertexBuffer.destroy()

        cullCameraBuffers.forEach(VulkanBuffer::destroy)
        shadowCameraBuffers.forEach(VulkanBuffer::destroy)
        cullObjectsBuffers.forEach(VulkanBuffer::destroy)
        indirectCommandBuffer.destroy()
        chunkPositionsBuffer.destroy()
        sidePositionsBuffer.destroy()
        shadowDepthAttachment.destroy()
        shadowColorAttachment.destroy()

        debugQuad.destroy()

        threadPool.shutdown()
    }
}