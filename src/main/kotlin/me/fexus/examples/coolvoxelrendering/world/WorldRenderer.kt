package me.fexus.examples.coolvoxelrendering.world

import me.fexus.camera.CameraPerspective
import me.fexus.examples.coolvoxelrendering.misc.DescriptorFactory
import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHull
import me.fexus.examples.coolvoxelrendering.world.position.ChunkPosition
import me.fexus.examples.surroundsound.MeshUploader
import me.fexus.math.mat.Mat4
import me.fexus.math.rad
import me.fexus.math.vec.Vec3
import me.fexus.memory.runMemorySafe
import me.fexus.model.QuadModelTriangleStrips
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.pipeline.*
import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkBufferMemoryBarrier
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.math.ceil


class WorldRenderer(
    private val deviceUtil: VulkanDeviceUtil,
    private val descriptorFactory: DescriptorFactory,
    private val camera: CameraPerspective
) {
    private val cullPipeline = ComputePipeline()
    private val chunkPipeline = GraphicsPipeline()

    private lateinit var hullVertexBuffer: VulkanBuffer

    lateinit var cullCameraBuffers: Array<VulkanBuffer>
    lateinit var cullObjectsBuffers: Array<VulkanBuffer>
    lateinit var indirectCommandBuffer: VulkanBuffer
    lateinit var chunkPositionsBuffer: VulkanBuffer
    lateinit var sidePositionsBuffer: VulkanBuffer

    private val chunkHulls = HashMap<ChunkPosition, ChunkHull>()
    private var nextHullOffset: Int = 0

    private val threadPool = Executors.newCachedThreadPool()
    private val processorCount = Runtime.getRuntime().availableProcessors()
    private lateinit var latch: CountDownLatch


    fun init() {
        createHullBuffers()
        createHullPipeline()
    }


    fun recordComputeCommands(commandBuffer: CommandBuffer, frameIndex: Int) = runMemorySafe {
        val chunkCount = chunkHulls.size
        // Begin by loading the chunk data into the cullObjects buffer on other threads
        cullObjectsBuffers[frameIndex].set(0, 0, cullObjectsBuffers[frameIndex].config.size)
        this@WorldRenderer.latch = CountDownLatch(chunkCount)
        var chunkedSize = chunkCount / processorCount
        if (chunkedSize == 0) chunkedSize = 1
        chunkHulls.values.chunked(chunkedSize).forEachIndexed { chunkedID, chunkHulls ->
            val hullsOffset = chunkedID * chunkedSize
            threadPool.execute {
                val buf = ByteBuffer.allocate(32)
                buf.order(ByteOrder.LITTLE_ENDIAN)
                for ((hullLocalOffset, hull) in chunkHulls.withIndex()) {
                    val offset = hullsOffset + hullLocalOffset
                    buf.clear()
                    hull.chunkPosition.intoByteBuffer(buf, 0)
                    buf.putInt(12, hull.data.instanceCount)
                    buf.putInt(16, hull.data.firstInstance)
                    cullObjectsBuffers[frameIndex].put(offset * 32, buf)
                    latch.countDown()
                }
            }
        }

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

        val pDescriptorSets = allocateLongValues(descriptorFactory.descriptorSets[frameIndex].vkHandle)
        vkCmdBindDescriptorSets(
            commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_COMPUTE, cullPipeline.vkLayoutHandle,
            0, pDescriptorSets, null
        )

        val camBuf = allocate(128)
        camBuf.order(ByteOrder.LITTLE_ENDIAN)
        val view = camera.calculateView()
        val proj = camera.calculateProjection()
        view.toByteBufferColumnMajor(camBuf, 0)
        proj.toByteBufferColumnMajor(camBuf, 64)
        cullCameraBuffers[frameIndex].put(0, camBuf)

        // Barrier that ensures that the buffers aren't in use anymore
        val initBarrier = calloc(VkBufferMemoryBarrier::calloc, 2)
        with(initBarrier[0]) {
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
        with(initBarrier[1]) {
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
            0, null, initBarrier, null
        )

        // Clear the buffers
        vkCmdFillBuffer(commandBuffer.vkHandle, indirectCommandBuffer.vkBufferHandle, 0L, indirectCommandBuffer.config.size, 0)
        vkCmdFillBuffer(commandBuffer.vkHandle, chunkPositionsBuffer.vkBufferHandle, 0L, chunkPositionsBuffer.config.size, 0)

        // Barrier to ensure that the clearing has finished
        with(initBarrier[0]) {
            srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
        }
        with(initBarrier[1]) {
            srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
        }
        vkCmdPipelineBarrier(
            commandBuffer.vkHandle,
            VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
            0, null, initBarrier, null
        )

        // Run culling compute shader
        vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_COMPUTE, cullPipeline.vkHandle)
        vkCmdDispatch(commandBuffer.vkHandle, ceil(chunkCount / 256f).toInt(), 1, 1)
    }

    fun recordRenderCommands(commandBuffer: CommandBuffer, frameIndex: Int) = runMemorySafe {
        val chunkCount = chunkHulls.size

        val pVertexBuffers = allocateLongValues(hullVertexBuffer.vkBufferHandle)
        val pOffsets = allocateLongValues(0)
        val pPushConstants = allocate(128)
        pPushConstants.putInt(0, 0)//cullCameraBuffers[frameIndex].index)
        pPushConstants.putInt(4, cullObjectsBuffers[frameIndex].index)
        pPushConstants.putInt(8, indirectCommandBuffer.index)
        pPushConstants.putInt(12, chunkPositionsBuffer.index)
        pPushConstants.putInt(16, sidePositionsBuffer.index)
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

        // Barrier that syncs between culling and rendering
        val computeBarrier = calloc(VkBufferMemoryBarrier::calloc, 2)
        with(computeBarrier[0]) {
            sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
            pNext(0)
            srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
            dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            buffer(indirectCommandBuffer.vkBufferHandle)
            offset(0L)
            size(VK_WHOLE_SIZE)
        }
        with(computeBarrier[1]) {
            sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
            pNext(0)
            srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
            dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            buffer(chunkPositionsBuffer.vkBufferHandle)
            offset(0L)
            size(VK_WHOLE_SIZE)
        }

        // Indirect Draw
        vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, chunkPipeline.vkHandle)
        vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)
        vkCmdDrawIndirect(commandBuffer.vkHandle, indirectCommandBuffer.vkBufferHandle, 0L, chunkCount, 16)

        // Wait for the cullObjects buffer to be fully written to
        this@WorldRenderer.latch.await()
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

        val cullCameraBuffer = VulkanBufferConfiguration(
            128L,
            MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.UNIFORM_BUFFER
        )
        this.cullCameraBuffers = descriptorFactory.createNBuffers(cullCameraBuffer)

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
            1_000_000_000L,
            MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.STORAGE_BUFFER
        )
        this.sidePositionsBuffer = descriptorFactory.createBuffer(positionsBufferConfig)

        // Just to map the buffers in advance
        cullObjectsBuffers.forEach { it.getInt(0) }
        sidePositionsBuffer.getInt(0)
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

        val computeConfig = ComputePipelineConfiguration(
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/compute/frustum_culling/frustum_culling_comp.spv").readBytes(),
            PushConstantsLayout(128, shaderStages = ShaderStage.COMPUTE + ShaderStage.FRAGMENT + ShaderStage.VERTEX),
        )
        this.cullPipeline.create(deviceUtil.device, descriptorFactory.descriptorSetLayout, computeConfig)
    }


    fun destroy() {
        cullPipeline.destroy()
        chunkPipeline.destroy()

        hullVertexBuffer.destroy()

        cullCameraBuffers.forEach(VulkanBuffer::destroy)
        cullObjectsBuffers.forEach(VulkanBuffer::destroy)
        indirectCommandBuffer.destroy()
        chunkPositionsBuffer.destroy()
        sidePositionsBuffer.destroy()

        threadPool.shutdown()
    }
}