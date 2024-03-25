package me.fexus.examples.coolvoxelrendering.world.rendering

import me.fexus.examples.coolvoxelrendering.DescriptorFactory
import me.fexus.examples.coolvoxelrendering.VoxelSide
import me.fexus.examples.coolvoxelrendering.world.chunk.ChunkHull
import me.fexus.examples.surroundsound.MeshUploader
import me.fexus.math.vec.IVec3
import me.fexus.memory.runMemorySafe
import me.fexus.model.QuadModelTriangleStrips
import me.fexus.voxel.VoxelOctree
import me.fexus.vulkan.VulkanDeviceUtil
import me.fexus.vulkan.component.CommandBuffer
import me.fexus.vulkan.component.pipeline.*
import me.fexus.vulkan.component.pipeline.shaderstage.ShaderStage
import me.fexus.vulkan.descriptors.buffer.VulkanBuffer
import me.fexus.vulkan.descriptors.buffer.VulkanBufferConfiguration
import me.fexus.vulkan.descriptors.buffer.usage.BufferUsage
import me.fexus.vulkan.descriptors.memorypropertyflags.MemoryPropertyFlag
import org.lwjgl.vulkan.VK12.*
import java.nio.ByteBuffer
import java.nio.ByteOrder


class WorldRenderer(private val deviceUtil: VulkanDeviceUtil, private val descriptorFactory: DescriptorFactory) {
    private val hullPipeline = GraphicsPipeline()
    private lateinit var hullVertexBuffer: VulkanBuffer
    lateinit var hullPositionBuffer: VulkanBuffer
    private val chunkHulls = HashMap<IVec3, ChunkHull>()
    private var nextHullOffset: Int = 0


    fun init() {
        createHullBuffers()
        createHullPipeline()
    }


    fun recordComputeCommands(commandBuffer: CommandBuffer) = runMemorySafe {

    }

    fun recordRenderCommands(commandBuffer: CommandBuffer) = runMemorySafe {
        val pVertexBuffers = allocateLongValues(hullVertexBuffer.vkBufferHandle)
        val pOffsets = allocateLongValues(0)

        val pPushConstants = allocate(128)
        pPushConstants.putInt(0, hullPositionBuffer.index)

        vkCmdBindPipeline(commandBuffer.vkHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, hullPipeline.vkHandle)
        vkCmdBindVertexBuffers(commandBuffer.vkHandle, 0, pVertexBuffers, pOffsets)

        // Indirect draw soon
        chunkHulls.forEach {
            pPushConstants.putInt(4, it.value.data.firstInstance)
            it.value.chunkPosition.intoByteBuffer(pPushConstants, 8)
            vkCmdPushConstants(
                commandBuffer.vkHandle,
                hullPipeline.vkLayoutHandle,
                ShaderStage.BOTH.vkBits,
                0,
                pPushConstants
            )

            vkCmdDraw(commandBuffer.vkHandle, 4, it.value.data.instanceCount, 0, 0)
        }
    }


    fun submitChunkHull(hull: ChunkHull) {
        hull.data.firstInstance = nextHullOffset / VoxelSide.SIZE_BYTES

        chunkHulls[hull.chunkPosition] = hull

        hullPositionBuffer.put(nextHullOffset, hull.data.buffer)
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

        val positionsBufferConfig = VulkanBufferConfiguration(
            256_000_000L,
            MemoryPropertyFlag.HOST_VISIBLE + MemoryPropertyFlag.HOST_COHERENT,
            BufferUsage.STORAGE_BUFFER
        )
        this.hullPositionBuffer = descriptorFactory.createBuffer(positionsBufferConfig)
    }

    private fun createHullPipeline() {
        val config = GraphicsPipelineConfiguration(
            listOf(
                VertexAttribute(0, VertexAttributeFormat.VEC3, 0),
                VertexAttribute(1, VertexAttributeFormat.VEC2, 12)
            ),
            PushConstantsLayout(128),
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/chunk/chunk_vert.spv").readBytes(),
            ClassLoader.getSystemResource("shaders/coolvoxelrendering/chunk/chunk_frag.spv").readBytes(),
            dynamicStates = listOf(DynamicState.SCISSOR, DynamicState.VIEWPORT),
            primitive = Primitive.TRIANGLE_STRIPS,
            cullMode = CullMode.BACKFACE
        )
        this.hullPipeline.create(deviceUtil.device, listOf(descriptorFactory.descriptorSetLayout), config)
    }


    fun destroy() {
        hullVertexBuffer.destroy()
        hullPipeline.destroy()
        hullPositionBuffer.destroy()
    }
}